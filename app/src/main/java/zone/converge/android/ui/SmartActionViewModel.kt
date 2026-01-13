// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zone.converge.android.data.*
import zone.converge.android.grpc.ConvergeClient
import zone.converge.android.ml.*

/**
 * ViewModel for the Smart Action Screen.
 *
 * Orchestrates:
 * - gRPC client for converge-runtime
 * - JTBD predictions
 * - Action tracking and learning
 */
class SmartActionViewModel(
    private val client: ConvergeClient,
    private val jtbdPredictor: JTBDPredictor,
    private val actionPredictor: ActionPredictor,
) : ViewModel() {

    private val _predictions = MutableStateFlow<List<JTBDPrediction>>(emptyList())
    val predictions = _predictions.asStateFlow()

    private val _blueprintSuggestions = MutableStateFlow<List<BlueprintSuggestion>>(emptyList())
    val blueprintSuggestions = _blueprintSuggestions.asStateFlow()

    private val _recentActivity = MutableStateFlow<List<ActivityRecord>>(emptyList())
    val recentActivity = _recentActivity.asStateFlow()

    private val _domainContext = MutableStateFlow(
        DomainContext(
            activeBlueprints = emptyMap(),
            recentlyProducedArtifacts = emptySet(),
            completedJtbds = emptySet(),
            currentPackId = null,
        )
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        refreshPredictions()
    }

    /**
     * Refresh predictions based on current context.
     */
    fun refreshPredictions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = _domainContext.value
                _predictions.value = jtbdPredictor.predict(context)
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
     * Execute a JTBD (start the job).
     */
    fun executeJTBD(jtbd: JTBD) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: Actually submit job to converge-runtime
                // For now, record completion and refresh
                jtbdPredictor.recordCompletion(jtbd.id, jtbd.producesArtifacts.toSet())

                // Update context
                val currentContext = _domainContext.value
                _domainContext.value = currentContext.copy(
                    completedJtbds = currentContext.completedJtbds + jtbd.id,
                    recentlyProducedArtifacts = jtbd.producesArtifacts.toSet(),
                )

                // Add to activity
                val activity = ActivityRecord(
                    id = "activity-${System.currentTimeMillis()}",
                    title = jtbd.title,
                    subtitle = jtbd.outcome,
                    type = ActivityType.JOB_COMPLETED,
                    timeAgo = "Just now",
                )
                _recentActivity.value = listOf(activity) + _recentActivity.value

                refreshPredictions()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Start a blueprint workflow.
     */
    fun startBlueprint(blueprint: Blueprint) {
        viewModelScope.launch {
            val currentContext = _domainContext.value
            _domainContext.value = currentContext.copy(
                activeBlueprints = currentContext.activeBlueprints + (blueprint.id to emptySet()),
            )
            refreshPredictions()
        }
    }

    /**
     * Connect to converge-runtime.
     */
    fun connect(host: String, port: Int) {
        viewModelScope.launch {
            try {
                client.connect()
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
