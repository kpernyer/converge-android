// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.ml

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import zone.converge.android.data.ArtifactType

@DisplayName("OutcomeTracker")
class OutcomeTrackerTest {

    private lateinit var mockBehaviorStore: BehaviorStore
    private lateinit var outcomeTracker: OutcomeTracker

    @BeforeEach
    fun setup() {
        mockBehaviorStore = mockk(relaxed = true)
        outcomeTracker = OutcomeTracker(mockBehaviorStore)
    }

    @Nested
    @DisplayName("Initial state")
    inner class InitialState {

        @Test
        @DisplayName("should have no frequent JTBDs initially")
        fun noFrequentJtbds() {
            assertThat(outcomeTracker.getFrequentJTBDs()).isEmpty()
        }

        @Test
        @DisplayName("should have no top pack initially")
        fun noTopPack() {
            assertThat(outcomeTracker.getTopPack()).isNull()
        }

        @Test
        @DisplayName("should have empty artifact stats initially")
        fun emptyArtifactStats() {
            assertThat(outcomeTracker.getArtifactStats()).isEmpty()
        }
    }

    @Nested
    @DisplayName("recordJTBDCompletion")
    inner class RecordJTBDCompletion {

        @Test
        @DisplayName("should track JTBD completion count")
        fun tracksJtbdCompletionCount() {
            outcomeTracker.recordJTBDCompletion("jtbd-issue-invoice", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-issue-invoice", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-issue-invoice", emptySet())

            val frequent = outcomeTracker.getFrequentJTBDs()
            assertThat(frequent).hasSize(1)
            assertThat(frequent.first().first).isEqualTo("jtbd-issue-invoice")
            assertThat(frequent.first().second).isEqualTo(3)
        }

        @Test
        @DisplayName("should track multiple JTBDs")
        fun tracksMultipleJtbds() {
            outcomeTracker.recordJTBDCompletion("jtbd-issue-invoice", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-collect-payment", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-capture-lead", emptySet())

            val frequent = outcomeTracker.getFrequentJTBDs()
            assertThat(frequent).hasSize(3)
        }

        @Test
        @DisplayName("should track pack completions from JTBD")
        fun tracksPackCompletions() {
            // jtbd-issue-invoice belongs to pack-money
            outcomeTracker.recordJTBDCompletion("jtbd-issue-invoice", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-collect-payment", emptySet())

            assertThat(outcomeTracker.getTopPack()).isEqualTo("pack-money")
        }

        @Test
        @DisplayName("should track produced artifacts")
        fun tracksProducedArtifacts() {
            outcomeTracker.recordJTBDCompletion(
                "jtbd-issue-invoice",
                setOf(ArtifactType.INVOICE),
            )
            outcomeTracker.recordJTBDCompletion(
                "jtbd-issue-invoice",
                setOf(ArtifactType.INVOICE),
            )

            val stats = outcomeTracker.getArtifactStats()
            assertThat(stats[ArtifactType.INVOICE]).isEqualTo(2)
        }

        @Test
        @DisplayName("should track multiple artifact types")
        fun tracksMultipleArtifactTypes() {
            outcomeTracker.recordJTBDCompletion(
                "jtbd-issue-invoice",
                setOf(ArtifactType.INVOICE, ArtifactType.PAYMENT),
            )

            val stats = outcomeTracker.getArtifactStats()
            assertThat(stats).hasSize(2)
            assertThat(stats[ArtifactType.INVOICE]).isEqualTo(1)
            assertThat(stats[ArtifactType.PAYMENT]).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("getFrequentJTBDs")
    inner class GetFrequentJTBDs {

        @Test
        @DisplayName("should return JTBDs sorted by frequency descending")
        fun sortedByFrequency() {
            outcomeTracker.recordJTBDCompletion("jtbd-capture-lead", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-issue-invoice", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-issue-invoice", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-issue-invoice", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-collect-payment", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-collect-payment", emptySet())

            val frequent = outcomeTracker.getFrequentJTBDs()

            assertThat(frequent[0].first).isEqualTo("jtbd-issue-invoice")
            assertThat(frequent[0].second).isEqualTo(3)
            assertThat(frequent[1].first).isEqualTo("jtbd-collect-payment")
            assertThat(frequent[1].second).isEqualTo(2)
            assertThat(frequent[2].first).isEqualTo("jtbd-capture-lead")
            assertThat(frequent[2].second).isEqualTo(1)
        }

        @Test
        @DisplayName("should handle ties in frequency")
        fun handlesTies() {
            outcomeTracker.recordJTBDCompletion("jtbd-a", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-b", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-c", emptySet())

            val frequent = outcomeTracker.getFrequentJTBDs()
            assertThat(frequent).hasSize(3)
            frequent.forEach { (_, count) ->
                assertThat(count).isEqualTo(1)
            }
        }
    }

    @Nested
    @DisplayName("getTopPack")
    inner class GetTopPack {

        @Test
        @DisplayName("should return pack with highest completion count")
        fun returnsHighestPack() {
            // Issue invoices (pack-money) multiple times
            outcomeTracker.recordJTBDCompletion("jtbd-issue-invoice", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-collect-payment", emptySet())
            outcomeTracker.recordJTBDCompletion("jtbd-record-expense", emptySet())

            // Capture lead (pack-customers) once
            outcomeTracker.recordJTBDCompletion("jtbd-capture-lead", emptySet())

            assertThat(outcomeTracker.getTopPack()).isEqualTo("pack-money")
        }

        @Test
        @DisplayName("should handle unknown JTBD gracefully")
        fun handlesUnknownJtbd() {
            outcomeTracker.recordJTBDCompletion("jtbd-unknown", emptySet())

            // Should not crash, but also no pack tracked
            assertThat(outcomeTracker.getTopPack()).isNull()
        }
    }

    @Nested
    @DisplayName("getArtifactStats")
    inner class GetArtifactStats {

        @Test
        @DisplayName("should accumulate artifact counts correctly")
        fun accumulatesArtifactCounts() {
            outcomeTracker.recordJTBDCompletion("jtbd-1", setOf(ArtifactType.INVOICE))
            outcomeTracker.recordJTBDCompletion("jtbd-2", setOf(ArtifactType.INVOICE, ArtifactType.LEAD))
            outcomeTracker.recordJTBDCompletion("jtbd-3", setOf(ArtifactType.INVOICE))

            val stats = outcomeTracker.getArtifactStats()
            assertThat(stats[ArtifactType.INVOICE]).isEqualTo(3)
            assertThat(stats[ArtifactType.LEAD]).isEqualTo(1)
        }

        @Test
        @DisplayName("should return defensive copy")
        fun returnsDefensiveCopy() {
            outcomeTracker.recordJTBDCompletion("jtbd-1", setOf(ArtifactType.INVOICE))

            val stats1 = outcomeTracker.getArtifactStats()
            outcomeTracker.recordJTBDCompletion("jtbd-2", setOf(ArtifactType.INVOICE))
            val stats2 = outcomeTracker.getArtifactStats()

            // Original map should not be affected
            assertThat(stats1[ArtifactType.INVOICE]).isEqualTo(1)
            assertThat(stats2[ArtifactType.INVOICE]).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("Property tests - Invariants")
    inner class PropertyTests {

        @Test
        @DisplayName("completion count should never decrease")
        fun completionCountNeverDecreases() {
            val jtbdId = "jtbd-issue-invoice"

            repeat(10) { iteration ->
                outcomeTracker.recordJTBDCompletion(jtbdId, emptySet())
                val count = outcomeTracker.getFrequentJTBDs()
                    .find { it.first == jtbdId }
                    ?.second ?: 0
                assertThat(count).isEqualTo(iteration + 1)
            }
        }

        @Test
        @DisplayName("total artifacts should equal sum of individual completions")
        fun artifactCountsAreConsistent() {
            outcomeTracker.recordJTBDCompletion("jtbd-1", setOf(ArtifactType.INVOICE, ArtifactType.PAYMENT))
            outcomeTracker.recordJTBDCompletion("jtbd-2", setOf(ArtifactType.INVOICE))
            outcomeTracker.recordJTBDCompletion("jtbd-3", setOf(ArtifactType.LEAD, ArtifactType.OPPORTUNITY))

            val stats = outcomeTracker.getArtifactStats()
            val totalArtifacts = stats.values.sum()
            // 2 + 1 + 2 = 5 artifact productions
            assertThat(totalArtifacts).isEqualTo(5)
        }

        @Test
        @DisplayName("frequent JTBDs list should contain all recorded JTBDs")
        fun allRecordedJtbdsInFrequent() {
            val recordedJtbds = setOf("jtbd-a", "jtbd-b", "jtbd-c")
            recordedJtbds.forEach { jtbdId ->
                outcomeTracker.recordJTBDCompletion(jtbdId, emptySet())
            }

            val frequentIds = outcomeTracker.getFrequentJTBDs().map { it.first }.toSet()
            assertThat(frequentIds).containsExactlyElementsIn(recordedJtbds)
        }
    }

    @Nested
    @DisplayName("Negative tests")
    inner class NegativeTests {

        @Test
        @DisplayName("should handle empty artifact set")
        fun handlesEmptyArtifactSet() {
            outcomeTracker.recordJTBDCompletion("jtbd-test", emptySet())

            val stats = outcomeTracker.getArtifactStats()
            assertThat(stats).isEmpty()
        }

        @Test
        @DisplayName("should handle recording same JTBD many times")
        fun handlesManyRecordings() {
            repeat(1000) {
                outcomeTracker.recordJTBDCompletion("jtbd-issue-invoice", setOf(ArtifactType.INVOICE))
            }

            val frequent = outcomeTracker.getFrequentJTBDs()
            assertThat(frequent).hasSize(1)
            assertThat(frequent.first().second).isEqualTo(1000)
        }

        @Test
        @DisplayName("should handle JTBD ID with special characters")
        fun handlesSpecialCharacters() {
            val specialId = "jtbd-special-!@#"
            outcomeTracker.recordJTBDCompletion(specialId, emptySet())

            val frequent = outcomeTracker.getFrequentJTBDs()
            assertThat(frequent.first().first).isEqualTo(specialId)
        }
    }
}
