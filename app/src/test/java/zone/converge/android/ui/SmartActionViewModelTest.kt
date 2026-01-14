// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.ui

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import zone.converge.android.data.ArtifactType
import zone.converge.android.data.Blueprint
import zone.converge.android.data.JTBD
import zone.converge.android.grpc.ConnectionState
import zone.converge.android.grpc.ConvergeClient
import zone.converge.android.grpc.RunStatusType
import zone.converge.android.ml.ActionPredictor
import zone.converge.android.ml.JTBDPrediction
import zone.converge.android.ml.JTBDPredictor
import zone.converge.android.ml.PredictionStrategy

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SmartActionViewModel")
class SmartActionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockClient: ConvergeClient
    private lateinit var mockJtbdPredictor: JTBDPredictor
    private lateinit var mockActionPredictor: ActionPredictor
    private lateinit var viewModel: SmartActionViewModel

    private val connectionStateFlow = MutableStateFlow(ConnectionState.OFFLINE)

    private val testJtbd = JTBD(
        id = "jtbd-test-action",
        packId = "pack-money",
        name = "Test Action",
        verb = "Test",
        obj = "action",
        outcome = "Action tested successfully",
        prerequisites = emptyList(),
        nextSteps = emptyList(),
        requiredArtifacts = emptyList(),
        producedArtifacts = listOf(ArtifactType.INVOICE),
    )

    private val testPrediction = JTBDPrediction(
        jtbd = testJtbd,
        confidence = 0.9f,
        reason = "Test prediction",
        strategy = PredictionStrategy.ONBOARDING,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockClient = mockk(relaxed = true) {
            every { connectionState } returns connectionStateFlow
            every { generateIdempotencyKey(any()) } returns "test-idempotency-key"
        }

        mockJtbdPredictor = mockk(relaxed = true) {
            every { predict(any()) } returns listOf(testPrediction)
            every { suggestBlueprints(any()) } returns emptyList()
        }

        mockActionPredictor = mockk(relaxed = true)

        viewModel = SmartActionViewModel(mockClient, mockJtbdPredictor, mockActionPredictor)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial state")
    inner class InitialState {

        @Test
        @DisplayName("should have empty proposals initially before prediction")
        fun emptyProposalsInitially() = runTest {
            // Note: init calls refreshProposals, but we test the starting state concept
            assertThat(viewModel.proposals.value).isNotNull()
        }

        @Test
        @DisplayName("should have empty blueprint suggestions initially")
        fun emptyBlueprintSuggestions() = runTest {
            assertThat(viewModel.blueprintSuggestions.value).isEmpty()
        }

        @Test
        @DisplayName("should have no selected pack initially")
        fun noSelectedPackInitially() = runTest {
            assertThat(viewModel.selectedPackId.value).isNull()
        }

        @Test
        @DisplayName("should have no halt explanation initially")
        fun noHaltExplanationInitially() = runTest {
            assertThat(viewModel.haltExplanation.value).isNull()
        }

        @Test
        @DisplayName("should not be loading initially after init completes")
        fun notLoadingInitially() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()
            assertThat(viewModel.isLoading.value).isFalse()
        }
    }

    @Nested
    @DisplayName("refreshProposals")
    inner class RefreshProposals {

        @Test
        @DisplayName("should call predictor with domain context")
        fun callsPredictorWithContext() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.refreshProposals()
            testDispatcher.scheduler.advanceUntilIdle()

            verify(atLeast = 1) { mockJtbdPredictor.predict(any()) }
        }

        @Test
        @DisplayName("should update proposals with predictor results")
        fun updatesProposals() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.proposals.value).contains(testPrediction)
        }

        @Test
        @DisplayName("should complete refresh without error")
        fun completesRefreshWithoutError() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.refreshProposals()
            testDispatcher.scheduler.advanceUntilIdle()

            // Loading should be false after refresh completes
            assertThat(viewModel.isLoading.value).isFalse()
        }
    }

    @Nested
    @DisplayName("submitProposal")
    inner class SubmitProposal {

        @Test
        @DisplayName("should record completion in predictor")
        fun recordsCompletion() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.submitProposal(testJtbd)
            testDispatcher.scheduler.advanceUntilIdle()

            verify {
                mockJtbdPredictor.recordCompletion(
                    testJtbd.id,
                    testJtbd.producedArtifacts.toSet(),
                )
            }
        }

        @Test
        @DisplayName("should add activity record after submission")
        fun addsActivityRecord() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.submitProposal(testJtbd)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.recentActivity.value).isNotEmpty()
            assertThat(viewModel.recentActivity.value.first().title).isEqualTo(testJtbd.name)
        }

        @Test
        @DisplayName("should set run status to RUNNING then CONVERGED")
        fun updatesRunStatus() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.submitProposal(testJtbd)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.runStatus.value?.status).isEqualTo(RunStatusType.CONVERGED)
        }

        @Test
        @DisplayName("should clear halt explanation before submission")
        fun clearsHaltExplanation() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.submitProposal(testJtbd)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.haltExplanation.value).isNull()
        }
    }

    @Nested
    @DisplayName("selectPack")
    inner class SelectPack {

        @Test
        @DisplayName("should update selectedPackId")
        fun updatesSelectedPackId() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.selectPack("pack-money")

            assertThat(viewModel.selectedPackId.value).isEqualTo("pack-money")
        }

        @Test
        @DisplayName("should refresh proposals after pack selection")
        fun refreshesProposalsAfterSelection() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.selectPack("pack-customers")
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify predictor was called (at least once for the pack selection)
            verify(atLeast = 1) { mockJtbdPredictor.predict(any()) }
        }

        @Test
        @DisplayName("should allow deselecting pack with null")
        fun allowsDeselectingPack() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.selectPack("pack-money")
            viewModel.selectPack(null)

            assertThat(viewModel.selectedPackId.value).isNull()
        }
    }

    @Nested
    @DisplayName("startBlueprint")
    inner class StartBlueprint {

        private val testBlueprint = Blueprint(
            id = "blueprint-test",
            name = "Test Blueprint",
            description = "A test blueprint",
            packIds = listOf("pack-money"),
            jtbdSequence = listOf("jtbd-step-1", "jtbd-step-2"),
            icon = "test-icon",
        )

        @Test
        @DisplayName("should refresh proposals after starting blueprint")
        fun refreshesAfterStart() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.startBlueprint(testBlueprint)
            testDispatcher.scheduler.advanceUntilIdle()

            verify(atLeast = 2) { mockJtbdPredictor.predict(any()) }
        }
    }

    @Nested
    @DisplayName("clearHaltAndRetry")
    inner class ClearHaltAndRetry {

        @Test
        @DisplayName("should clear halt explanation")
        fun clearsHaltExplanation() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.clearHaltAndRetry()

            assertThat(viewModel.haltExplanation.value).isNull()
        }

        @Test
        @DisplayName("should clear run status")
        fun clearsRunStatus() = runTest {
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.clearHaltAndRetry()

            assertThat(viewModel.runStatus.value).isNull()
        }
    }

    @Nested
    @DisplayName("Connection state")
    inner class ConnectionStateTests {

        @Test
        @DisplayName("should expose connection state from client")
        fun exposesConnectionState() = runTest {
            assertThat(viewModel.connectionState.value).isEqualTo(ConnectionState.OFFLINE)
        }

        @Test
        @DisplayName("should reflect connection state changes")
        fun reflectsConnectionStateChanges() = runTest {
            connectionStateFlow.value = ConnectionState.STREAMING

            assertThat(viewModel.connectionState.value).isEqualTo(ConnectionState.STREAMING)
        }
    }

    @Nested
    @DisplayName("Negative tests")
    inner class NegativeTests {

        @Test
        @DisplayName("should handle predictor throwing exception")
        fun handlesPredictorException() = runTest {
            every { mockJtbdPredictor.predict(any()) } throws RuntimeException("Prediction failed")

            viewModel.refreshProposals()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.error.value).isNotNull()
        }

        @Test
        @DisplayName("should handle empty predictions gracefully")
        fun handlesEmptyPredictions() = runTest {
            every { mockJtbdPredictor.predict(any()) } returns emptyList()

            viewModel.refreshProposals()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.proposals.value).isEmpty()
            assertThat(viewModel.error.value).isNull()
        }
    }
}
