// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.eval

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import zone.converge.android.data.ArtifactType
import zone.converge.android.ml.*

/**
 * JTBD Predictor Eval Tests
 *
 * These tests verify prediction behavior using fixture-driven test cases.
 * Each test corresponds to an eval case from jtbd_predictions.json.
 *
 * Run with: ./gradlew test --tests "*JTBDPredictorEvalTest*"
 */
@DisplayName("JTBD Predictor Evals")
class JTBDPredictorEvalTest {

    private lateinit var predictor: JTBDPredictor
    private lateinit var outcomeTracker: OutcomeTracker

    @BeforeEach
    fun setup() {
        val behaviorStore = mockk<BehaviorStore>(relaxed = true)
        outcomeTracker = OutcomeTracker(behaviorStore)
        predictor = JTBDPredictor(outcomeTracker)
    }

    @Nested
    @DisplayName("Blueprint-driven predictions")
    inner class BlueprintDriven {

        @Test
        @DisplayName("should suggest next step when blueprint is active")
        fun blueprintContinuation() = runTest {
            // Given: Active blueprint with some steps completed
            val context = DomainContext(
                activeBlueprints = mapOf(
                    "blueprint-lead-to-cash" to setOf("jtbd-capture-lead", "jtbd-qualify-opportunity"),
                ),
                recentlyProducedArtifacts = setOf(ArtifactType.OPPORTUNITY),
                completedJtbds = setOf("jtbd-capture-lead", "jtbd-qualify-opportunity"),
                currentPackId = "pack-customers",
            )

            // When
            val predictions = predictor.predict(context)

            // Then
            assertThat(predictions).isNotEmpty()
            val top = predictions.first()
            assertThat(top.jtbd.id).isEqualTo("jtbd-create-proposal")
            assertThat(top.strategy).isEqualTo(PredictionStrategy.BLUEPRINT)
            assertThat(top.confidence).isAtLeast(0.9f)
        }
    }

    @Nested
    @DisplayName("Artifact flow predictions")
    inner class ArtifactFlow {

        @Test
        @DisplayName("should suggest actions that consume recent artifacts")
        fun artifactFlowSuggestion() = runTest {
            // Given: Recently produced an invoice
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = setOf(ArtifactType.INVOICE),
                completedJtbds = setOf("jtbd-issue-invoice"),
                currentPackId = "pack-money",
            )

            // When
            val predictions = predictor.predict(context)

            // Then: Should suggest collecting payment (uses invoice)
            assertThat(predictions).isNotEmpty()
            val artifactFlowPredictions = predictions.filter {
                it.strategy == PredictionStrategy.ARTIFACT_FLOW
            }
            assertThat(artifactFlowPredictions).isNotEmpty()
            assertThat(artifactFlowPredictions.first().jtbd.id).isEqualTo("jtbd-collect-payment")
        }
    }

    @Nested
    @DisplayName("Onboarding predictions")
    inner class Onboarding {

        @Test
        @DisplayName("should suggest starter JTBDs for new users")
        fun onboardingEmpty() = runTest {
            // Given: Empty context (new user)
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            // When
            val predictions = predictor.predict(context)

            // Then: Should have onboarding predictions
            assertThat(predictions).isNotEmpty()
            val onboardingPredictions = predictions.filter {
                it.strategy == PredictionStrategy.ONBOARDING
            }
            assertThat(onboardingPredictions).isNotEmpty()

            // Check that starter JTBDs are suggested
            val starterIds = setOf("jtbd-capture-lead", "jtbd-issue-invoice", "jtbd-onboard-person")
            val predictedIds = onboardingPredictions.map { it.jtbd.id }.toSet()
            assertThat(predictedIds.intersect(starterIds)).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Invariants")
    inner class Invariants {

        @Test
        @DisplayName("predictions are sorted by confidence descending")
        fun sortedByConfidence() = runTest {
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)

            if (predictions.size > 1) {
                for (i in 0 until predictions.size - 1) {
                    assertThat(predictions[i].confidence)
                        .isAtLeast(predictions[i + 1].confidence)
                }
            }
        }

        @Test
        @DisplayName("no duplicate JTBD IDs in predictions")
        fun noDuplicates() = runTest {
            val context = DomainContext(
                activeBlueprints = mapOf(
                    "blueprint-lead-to-cash" to setOf("jtbd-capture-lead"),
                ),
                recentlyProducedArtifacts = setOf(ArtifactType.LEAD),
                completedJtbds = setOf("jtbd-capture-lead"),
                currentPackId = "pack-customers",
            )

            val predictions = predictor.predict(context)
            val jtbdIds = predictions.map { it.jtbd.id }

            assertThat(jtbdIds).containsNoDuplicates()
        }

        @Test
        @DisplayName("confidence values are between 0 and 1")
        fun confidenceBounds() = runTest {
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)

            predictions.forEach { prediction ->
                assertThat(prediction.confidence).isIn(com.google.common.collect.Range.closed(0f, 1f))
            }
        }

        @Test
        @DisplayName("every prediction has a non-empty reason")
        fun nonEmptyReasons() = runTest {
            val context = DomainContext(
                activeBlueprints = emptyMap(),
                recentlyProducedArtifacts = emptySet(),
                completedJtbds = emptySet(),
                currentPackId = null,
            )

            val predictions = predictor.predict(context)

            predictions.forEach { prediction ->
                assertThat(prediction.reason).isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("Flow testing with Turbine")
    inner class FlowTests {

        @Test
        @DisplayName("predictions flow emits updates")
        fun predictionsFlowEmits() = runTest {
            predictor.predictions.test {
                // Initial empty state
                assertThat(awaitItem()).isEmpty()

                // Trigger prediction
                val context = DomainContext(
                    activeBlueprints = emptyMap(),
                    recentlyProducedArtifacts = emptySet(),
                    completedJtbds = emptySet(),
                    currentPackId = null,
                )
                predictor.predict(context)

                // Should emit predictions
                val predictions = awaitItem()
                assertThat(predictions).isNotEmpty()

                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
