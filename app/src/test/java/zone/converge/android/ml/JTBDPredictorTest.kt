// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.ml

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import zone.converge.android.data.ArtifactType
import zone.converge.android.data.DomainKnowledgeBase

@DisplayName("JTBDPredictor")
class JTBDPredictorTest {

    private lateinit var mockOutcomeTracker: OutcomeTracker
    private lateinit var predictor: JTBDPredictor

    @BeforeEach
    fun setup() {
        mockOutcomeTracker = mockk(relaxed = true) {
            every { getFrequentJTBDs() } returns emptyList()
            every { getTopPack() } returns null
        }
        predictor = JTBDPredictor(mockOutcomeTracker)
    }

    @Nested
    @DisplayName("predict - Blueprint strategy")
    inner class BlueprintStrategy {

        @Test
        @DisplayName("should prioritize blueprint continuation over other strategies")
        fun prioritizesBlueprintContinuation() = runTest {
            val context = DomainContext(
                activeBlueprints = mapOf(
                    "blueprint-lead-to-cash" to setOf("jtbd-capture-lead"),
                ),
                recentlyProducedArtifacts = setOf(ArtifactType.LEAD),
                completedJtbds = setOf("jtbd-capture-lead"),
                currentPackId = "pack-customers",
            )

            val predictions = predictor.predict(context)

            assertThat(predictions).isNotEmpty()
            assertThat(predictions.first().strategy).isEqualTo(PredictionStrategy.BLUEPRINT)
            assertThat(predictions.first().confidence).isAtLeast(0.9f)
        }

        @Test
        @DisplayName("should suggest correct next step in blueprint")
        fun suggestsCorrectNextStep() = runTest {
            val context = DomainContext(
                activeBlueprints = mapOf(
                    "blueprint-lead-to-cash" to setOf("jtbd-capture-lead"),
                ),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = setOf("jtbd-capture-lead"),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)

            assertThat(predictions.first().jtbd.id).isEqualTo("jtbd-develop-opportunity")
        }

        @Test
        @DisplayName("should handle multiple active blueprints")
        fun handlesMultipleBlueprints() = runTest {
            val context = DomainContext(
                activeBlueprints = mapOf(
                    "blueprint-lead-to-cash" to setOf("jtbd-capture-lead"),
                    "blueprint-hire-to-retire" to emptySet(),
                ),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = setOf("jtbd-capture-lead"),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)
            val blueprintPredictions = predictions.filter {
                it.strategy == PredictionStrategy.BLUEPRINT
            }

            // Should have at least one blueprint prediction
            assertThat(blueprintPredictions).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("predict - Artifact flow strategy")
    inner class ArtifactFlowStrategy {

        @Test
        @DisplayName("should suggest actions that use recent artifacts")
        fun suggestsActionsForArtifacts() = runTest {
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = setOf(ArtifactType.INVOICE),
                completedJtbds = setOf("jtbd-issue-invoice"),
                currentPackId = "pack-money",
            )

            val predictions = predictor.predict(context)
            val artifactPredictions = predictions.filter {
                it.strategy == PredictionStrategy.ARTIFACT_FLOW
            }

            assertThat(artifactPredictions).isNotEmpty()
            assertThat(artifactPredictions.first().jtbd.id).isEqualTo("jtbd-collect-payment")
        }

        @Test
        @DisplayName("should limit artifact flow suggestions to 3")
        fun limitsArtifactSuggestions() = runTest {
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = setOf(ArtifactType.LEAD),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)
            val artifactPredictions = predictions.filter {
                it.strategy == PredictionStrategy.ARTIFACT_FLOW
            }

            assertThat(artifactPredictions.size).isAtMost(3)
        }

        @Test
        @DisplayName("should not suggest artifact flow when no recent artifacts")
        fun noSuggestionsWithoutArtifacts() = runTest {
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)
            val artifactPredictions = predictions.filter {
                it.strategy == PredictionStrategy.ARTIFACT_FLOW
            }

            assertThat(artifactPredictions).isEmpty()
        }
    }

    @Nested
    @DisplayName("predict - Frequency strategy")
    inner class FrequencyStrategy {

        @Test
        @DisplayName("should use frequent JTBDs from outcome tracker")
        fun usesFrequentJtbds() = runTest {
            every { mockOutcomeTracker.getFrequentJTBDs() } returns listOf(
                "jtbd-issue-invoice" to 10,
                "jtbd-collect-payment" to 5,
            )

            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)
            val frequencyPredictions = predictions.filter {
                it.strategy == PredictionStrategy.FREQUENCY
            }

            assertThat(frequencyPredictions).isNotEmpty()
        }

        @Test
        @DisplayName("should limit frequency suggestions to 2")
        fun limitsFrequencySuggestions() = runTest {
            every { mockOutcomeTracker.getFrequentJTBDs() } returns listOf(
                "jtbd-issue-invoice" to 10,
                "jtbd-collect-payment" to 8,
                "jtbd-capture-lead" to 6,
                "jtbd-develop-opportunity" to 4,
            )

            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)
            val frequencyPredictions = predictions.filter {
                it.strategy == PredictionStrategy.FREQUENCY
            }

            assertThat(frequencyPredictions.size).isAtMost(2)
        }
    }

    @Nested
    @DisplayName("predict - Pack affinity strategy")
    inner class PackAffinityStrategy {

        @Test
        @DisplayName("should suggest JTBDs from top pack")
        fun suggestsFromTopPack() = runTest {
            every { mockOutcomeTracker.getTopPack() } returns "pack-money"

            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)
            val packPredictions = predictions.filter {
                it.strategy == PredictionStrategy.PACK_AFFINITY
            }

            assertThat(packPredictions).isNotEmpty()
            packPredictions.forEach { prediction ->
                assertThat(prediction.jtbd.packId).isEqualTo("pack-money")
            }
        }

        @Test
        @DisplayName("should not duplicate JTBDs from other strategies")
        fun avoidsPackDuplicates() = runTest {
            every { mockOutcomeTracker.getTopPack() } returns "pack-money"
            every { mockOutcomeTracker.getFrequentJTBDs() } returns listOf(
                "jtbd-issue-invoice" to 10,
            )

            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)
            val jtbdIds = predictions.map { it.jtbd.id }

            assertThat(jtbdIds).containsNoDuplicates()
        }
    }

    @Nested
    @DisplayName("predict - Onboarding strategy")
    inner class OnboardingStrategy {

        @Test
        @DisplayName("should show onboarding for new users")
        fun showsOnboardingForNewUsers() = runTest {
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)
            val onboardingPredictions = predictions.filter {
                it.strategy == PredictionStrategy.ONBOARDING
            }

            assertThat(onboardingPredictions).isNotEmpty()
        }

        @Test
        @DisplayName("onboarding should have low confidence")
        fun onboardingLowConfidence() = runTest {
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)
            val onboardingPredictions = predictions.filter {
                it.strategy == PredictionStrategy.ONBOARDING
            }

            onboardingPredictions.forEach { prediction ->
                assertThat(prediction.confidence).isLessThan(0.5f)
            }
        }
    }

    @Nested
    @DisplayName("recordCompletion")
    inner class RecordCompletion {

        @Test
        @DisplayName("should delegate to outcome tracker")
        fun delegatesToOutcomeTracker() {
            predictor.recordCompletion("jtbd-issue-invoice", setOf(ArtifactType.INVOICE))

            verify {
                mockOutcomeTracker.recordJTBDCompletion(
                    "jtbd-issue-invoice",
                    setOf(ArtifactType.INVOICE),
                )
            }
        }
    }

    @Nested
    @DisplayName("suggestBlueprints")
    inner class SuggestBlueprints {

        @Test
        @DisplayName("should suggest blueprints based on completed JTBDs")
        fun suggestsBasedOnCompleted() = runTest {
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = setOf("jtbd-capture-lead", "jtbd-develop-opportunity"),
                currentPackId = null,
            )

            val suggestions = predictor.suggestBlueprints(context)

            assertThat(suggestions).isNotEmpty()
            assertThat(suggestions.first().blueprint.id).isEqualTo("blueprint-lead-to-cash")
        }

        @Test
        @DisplayName("should not suggest already active blueprints")
        fun excludesActiveBlueprints() = runTest {
            val context = DomainContext(
                activeBlueprints = mapOf("blueprint-lead-to-cash" to emptySet()),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = setOf("jtbd-capture-lead"),
                currentPackId = null,
            )

            val suggestions = predictor.suggestBlueprints(context)
            val blueprintIds = suggestions.map { it.blueprint.id }

            assertThat(blueprintIds).doesNotContain("blueprint-lead-to-cash")
        }

        @Test
        @DisplayName("should include pack affinity suggestions")
        fun includesPackAffinitySuggestions() = runTest {
            every { mockOutcomeTracker.getTopPack() } returns "pack-money"

            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val suggestions = predictor.suggestBlueprints(context)

            // Should have suggestions from pack affinity
            val packAffinitySuggestions = suggestions.filter {
                it.reason.contains("focus")
            }
            assertThat(packAffinitySuggestions).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("predictions flow")
    inner class PredictionsFlow {

        @Test
        @DisplayName("should update flow when predict is called")
        fun updatesFlowOnPredict() = runTest {
            predictor.predictions.test {
                assertThat(awaitItem()).isEmpty()

                predictor.predict(
                    DomainContext(
                        activeBlueprints = emptyMap(),
                        recentlyProducedArtifacts = emptySet(),
                        completedJtbds = emptySet(),
                        currentPackId = null,
                    ),
                )

                val predictions = awaitItem()
                assertThat(predictions).isNotEmpty()

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Property tests - Prediction invariants")
    inner class PredictionInvariants {

        @Test
        @DisplayName("predictions should always be sorted by confidence")
        fun alwaysSortedByConfidence() = runTest {
            val contexts = listOf(
                DomainContext(emptyMap(), emptySet(), emptySet(), null),
                DomainContext(
                    mapOf("blueprint-lead-to-cash" to setOf("jtbd-capture-lead")),
                    setOf(ArtifactType.LEAD),
                    setOf("jtbd-capture-lead"),
                    "pack-customers",
                ),
                DomainContext(emptyMap(), setOf(ArtifactType.INVOICE), emptySet(), "pack-money"),
            )

            contexts.forEach { context ->
                val predictions = predictor.predict(context)
                for (i in 0 until predictions.size - 1) {
                    assertThat(predictions[i].confidence)
                        .isAtLeast(predictions[i + 1].confidence)
                }
            }
        }

        @Test
        @DisplayName("predictions should never have duplicates")
        fun neverHaveDuplicates() = runTest {
            every { mockOutcomeTracker.getTopPack() } returns "pack-customers"
            every { mockOutcomeTracker.getFrequentJTBDs() } returns listOf(
                "jtbd-capture-lead" to 10,
            )

            val context = DomainContext(
                activeBlueprints = mapOf("blueprint-lead-to-cash" to emptySet()),
                recentlyProducedArtifacts = setOf(ArtifactType.LEAD),
                completedJtbds = emptySet(),
                currentPackId = "pack-customers",
            )

            val predictions = predictor.predict(context)
            val jtbdIds = predictions.map { it.jtbd.id }

            assertThat(jtbdIds).containsNoDuplicates()
        }

        @Test
        @DisplayName("all predictions should have valid JTBD references")
        fun allPredictionsHaveValidJtbds() = runTest {
            val context = DomainContext(
                activeBlueprints = mapOf("blueprint-lead-to-cash" to setOf("jtbd-capture-lead")),
                recentlyProducedArtifacts = setOf(ArtifactType.INVOICE),
                completedJtbds = setOf("jtbd-capture-lead"),
                currentPackId = "pack-money",
            )

            val predictions = predictor.predict(context)

            predictions.forEach { prediction ->
                val jtbd = DomainKnowledgeBase.getJTBD(prediction.jtbd.id)
                assertThat(jtbd).isNotNull()
                assertThat(jtbd).isEqualTo(prediction.jtbd)
            }
        }

        @Test
        @DisplayName("confidence should be between 0 and 1")
        fun confidenceInRange() = runTest {
            val context = DomainContext(
                activeBlueprints = mapOf("blueprint-lead-to-cash" to emptySet()),
                recentlyProducedArtifacts = setOf(ArtifactType.LEAD),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)

            predictions.forEach { prediction ->
                assertThat(prediction.confidence).isIn(
                    com.google.common.collect.Range.closed(0f, 1f),
                )
            }
        }

        @Test
        @DisplayName("blueprint predictions should have highest confidence")
        fun blueprintHighestConfidence() = runTest {
            val context = DomainContext(
                activeBlueprints = mapOf("blueprint-lead-to-cash" to setOf("jtbd-capture-lead")),
                recentlyProducedArtifacts = setOf(ArtifactType.LEAD),
                completedJtbds = setOf("jtbd-capture-lead"),
                currentPackId = "pack-customers",
            )

            val predictions = predictor.predict(context)
            val blueprintPredictions = predictions.filter {
                it.strategy == PredictionStrategy.BLUEPRINT
            }
            val otherPredictions = predictions.filter {
                it.strategy != PredictionStrategy.BLUEPRINT
            }

            if (blueprintPredictions.isNotEmpty() && otherPredictions.isNotEmpty()) {
                val lowestBlueprintConfidence = blueprintPredictions.minOf { it.confidence }
                val highestOtherConfidence = otherPredictions.maxOf { it.confidence }
                assertThat(lowestBlueprintConfidence).isAtLeast(highestOtherConfidence)
            }
        }
    }

    @Nested
    @DisplayName("Negative tests")
    inner class NegativeTests {

        @Test
        @DisplayName("should handle empty blueprints map")
        fun handlesEmptyBlueprints() = runTest {
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)

            assertThat(predictions).isNotEmpty()
        }

        @Test
        @DisplayName("should handle invalid blueprint ID")
        fun handlesInvalidBlueprint() = runTest {
            val context = DomainContext(
                activeBlueprints = mapOf("blueprint-invalid" to emptySet()),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)

            // Should still return predictions (onboarding)
            assertThat(predictions).isNotEmpty()
        }

        @Test
        @DisplayName("should handle invalid pack ID")
        fun handlesInvalidPack() = runTest {
            every { mockOutcomeTracker.getTopPack() } returns "pack-invalid"

            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)

            // Should still return predictions (onboarding)
            assertThat(predictions).isNotEmpty()
        }

        @Test
        @DisplayName("should handle all JTBDs already completed")
        fun handlesAllCompleted() = runTest {
            val allJtbdIds = DomainKnowledgeBase.jtbds.keys

            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = allJtbdIds,
                currentPackId = null,
            )

            val predictions = predictor.predict(context)

            // Should still work, might return onboarding suggestions
            assertThat(predictions).isNotNull()
        }
    }
}
