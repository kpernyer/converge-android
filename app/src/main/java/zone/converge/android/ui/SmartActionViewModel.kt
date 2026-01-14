// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zone.converge.android.data.*
import zone.converge.android.grpc.ConnectionState
import zone.converge.android.grpc.ConvergeClient
import zone.converge.android.grpc.RunStatus
import zone.converge.android.grpc.RunStatusType
import zone.converge.android.ml.*

/**
 * ViewModel for the Smart Action Screen.
 *
 * Per CROSS_PLATFORM_CONTRACT.md, this implements:
 * - Truths-First UX Rule (§12): UI state derived from context, not local flags
 * - SmartAction Predictions (§10): ML produces proposals, not facts
 * - Action Lifecycle (§11): Propose → Consent → Check → Execute → Record → Trace
 * - Live Convergence (§1.7): Handle facts from any actor, not just our requests
 */
class SmartActionViewModel(
    private val client: ConvergeClient,
    private val jtbdPredictor: JTBDPredictor,
    private val actionPredictor: ActionPredictor,
) : ViewModel() {

    // Per contract §10: Predictions are PROPOSALS, not facts
    private val _proposals = MutableStateFlow<List<JTBDPrediction>>(emptyList())
    val proposals = _proposals.asStateFlow()

    private val _blueprintSuggestions = MutableStateFlow<List<BlueprintSuggestion>>(emptyList())
    val blueprintSuggestions = _blueprintSuggestions.asStateFlow()

    // Activity log derived from context traces per §12
    private val _recentActivity = MutableStateFlow<List<ActivityRecord>>(emptyList())
    val recentActivity = _recentActivity.asStateFlow()

    private val _domainContext = MutableStateFlow(
        DomainContext(
            activeBlueprints = emptyMap(),
            recentlyProducedArtifacts = emptySet(),
            completedJtbds = emptySet(),
            currentPackId = null,
        ),
    )

    // Connection state from client per contract §1.5
    val connectionState: StateFlow<ConnectionState> = client.connectionState

    // Run status per contract §1.8
    private val _runStatus = MutableStateFlow<RunStatus?>(null)
    val runStatus = _runStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Halt explanation per contract §5.1
    private val _haltExplanation = MutableStateFlow<HaltExplanation?>(null)
    val haltExplanation = _haltExplanation.asStateFlow()

    init {
        refreshProposals()
    }

    /**
     * Refresh proposals based on current context.
     * Per contract §10: These are PROPOSALS, not facts.
     * They must pass through convergence layer before becoming facts.
     */
    fun refreshProposals() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = _domainContext.value
                // Per §10.2: predictions include truth_id, confidence, requires_consent
                _proposals.value = jtbdPredictor.predict(context)
                _blueprintSuggestions.value = jtbdPredictor.suggestBlueprints(context)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Submit a proposal to execute a JTBD.
     *
     * Per contract §11, follows action lifecycle:
     * 1. PROPOSE - ML or user suggests action
     * 2. CONSENT - Get user approval if needed
     * 3. CHECK INVARIANTS - Verify against Truths
     * 4. EXECUTE - Perform the action
     * 5. RECORD FACT - Append to context
     * 6. EMIT TRACE - Audit trail
     */
    fun submitProposal(jtbd: JTBD) {
        viewModelScope.launch {
            _isLoading.value = true
            _haltExplanation.value = null

            val correlationId = java.util.UUID.randomUUID().toString()
            val idempotencyKey = client.generateIdempotencyKey("execute_jtbd")

            try {
                // Update run status to RUNNING per §1.8
                _runStatus.value = RunStatus(
                    runId = "run_${System.currentTimeMillis()}",
                    status = RunStatusType.RUNNING,
                )

                // Step 1-2: PROPOSE & CONSENT - Already done by user tapping the action

                // Step 3: CHECK INVARIANTS - Submit proposal, await decision
                // In production, this streams back early facts and may HALT
                val decision = checkInvariants(jtbd, correlationId)

                if (decision.halted) {
                    // Per §5.1: HALT → EXPLAIN → offer RESTART
                    _runStatus.value = RunStatus(
                        runId = _runStatus.value?.runId ?: "",
                        status = RunStatusType.HALTED,
                        haltReason = decision.reason,
                        haltTruthId = decision.truthId,
                    )
                    _haltExplanation.value = HaltExplanation(
                        reason = decision.reason ?: "Invariant violated",
                        truthId = decision.truthId,
                        canRetry = true,
                    )
                    return@launch
                }

                // Step 4: EXECUTE
                jtbdPredictor.recordCompletion(jtbd.id, jtbd.producedArtifacts.toSet())

                // Step 5: RECORD FACT - Context only grows (§0.1 Monotonicity)
                val currentContext = _domainContext.value
                _domainContext.value = currentContext.copy(
                    completedJtbds = currentContext.completedJtbds + jtbd.id,
                    recentlyProducedArtifacts = jtbd.producedArtifacts.toSet(),
                )

                // Step 6: EMIT TRACE - Activity from context traces
                val activity = ActivityRecord(
                    id = "activity-${System.currentTimeMillis()}",
                    title = jtbd.name,
                    subtitle = jtbd.outcome,
                    type = ActivityType.JOB_COMPLETED,
                    timeAgo = "Just now",
                )
                _recentActivity.value = listOf(activity) + _recentActivity.value

                // Update run status to CONVERGED
                _runStatus.value = RunStatus(
                    runId = _runStatus.value?.runId ?: "",
                    status = RunStatusType.CONVERGED,
                    factsCount = 1,
                )

                refreshProposals()
            } catch (e: Exception) {
                _error.value = e.message
                _runStatus.value = _runStatus.value?.copy(status = RunStatusType.HALTED)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check invariants against Truths per contract §5
     */
    private suspend fun checkInvariants(jtbd: JTBD, correlationId: String): InvariantDecision {
        // TODO: Actually call convergence layer via gRPC
        // For now, always pass (simulated)
        return InvariantDecision(halted = false, reason = null, truthId = null)
    }

    /**
     * Clear halt explanation and allow retry per contract §5.1
     */
    fun clearHaltAndRetry() {
        _haltExplanation.value = null
        _runStatus.value = null
    }

    /**
     * Start a blueprint workflow.
     * Per §10.1: Blueprint-Driven has highest confidence (0.95)
     */
    fun startBlueprint(blueprint: Blueprint) {
        viewModelScope.launch {
            val currentContext = _domainContext.value
            _domainContext.value = currentContext.copy(
                activeBlueprints = currentContext.activeBlueprints + (blueprint.id to emptySet()),
            )
            refreshProposals()
        }
    }

    /**
     * Connect to converge-runtime.
     * Per contract §8: Session starts with capability negotiation.
     */
    fun connect(host: String, port: Int) {
        viewModelScope.launch {
            try {
                client.connect()
                // TODO: Perform capability negotiation per §8.1
            } catch (e: Exception) {
                _error.value = "Connection failed: ${e.message}"
            }
        }
    }

    /**
     * Disconnect from converge-runtime.
     */
    fun disconnect() {
        viewModelScope.launch {
            client.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            client.disconnect()
        }
    }
}

/**
 * Result of invariant check per contract §5
 */
data class InvariantDecision(
    val halted: Boolean,
    val reason: String?,
    val truthId: String?,
)

/**
 * Halt explanation for UI per contract §5.1
 * Supports: HALT → EXPLAIN → RESTART
 */
data class HaltExplanation(
    val reason: String,
    val truthId: String?,
    val canRetry: Boolean,
)
