// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.data

import java.time.Instant

/**
 * Jobs To Be Done - represents what the user is trying to accomplish.
 * JTBD focus on outcomes, not features.
 */
data class Job(
    val id: String,
    val title: String,
    val outcome: String,
    val pack: Pack,
    val flows: List<Flow>,
    val priority: Priority = Priority.NORMAL,
    val status: JobStatus = JobStatus.PENDING,
    val createdAt: Instant = Instant.now(),
)

enum class Priority { LOW, NORMAL, HIGH, URGENT }
enum class JobStatus { PENDING, IN_PROGRESS, BLOCKED, COMPLETED, CANCELLED }

/**
 * A Flow is a sequence of steps to complete a Job.
 */
data class Flow(
    val id: String,
    val name: String,
    val steps: List<FlowStep>,
    val requiredArtifacts: List<ArtifactType>,
    val producedArtifacts: List<ArtifactType>,
)

data class FlowStep(
    val id: String,
    val name: String,
    val action: Action,
    val optional: Boolean = false,
)

/**
 * Packs are domain modules (e.g., Growth Strategy, SDR Pipeline).
 */
data class Pack(
    val id: String,
    val name: String,
    val description: String,
    val templates: List<Template>,
    val invariants: List<String>,
)

data class Template(
    val id: String,
    val name: String,
    val description: String,
    val seedSchema: String,
)

/**
 * Artifacts are outputs from flows (reports, data, decisions).
 */
data class Artifact(
    val id: String,
    val type: ArtifactType,
    val title: String,
    val content: ByteArray,
    val metadata: Map<String, String>,
    val createdAt: Instant = Instant.now(),
) {
    override fun equals(other: Any?) = other is Artifact && id == other.id
    override fun hashCode() = id.hashCode()
}

// ArtifactType is defined in DomainKnowledgeBase.kt

/**
 * Actions are the atomic operations a user can perform.
 */
sealed class Action {
    data class StartJob(val templateId: String, val seeds: Map<String, Any>) : Action()
    data class ViewJob(val jobId: String) : Action()
    data class ViewArtifact(val artifactId: String) : Action()
    data class ApproveDecision(val decisionId: String) : Action()
    data class RejectDecision(val decisionId: String, val reason: String) : Action()
    data class ExportData(val artifactId: String, val format: String) : Action()
    data class NavigateToPack(val packId: String) : Action()
    data class OpenFlow(val flowId: String) : Action()
    data object Refresh : Action()
    data object OpenSettings : Action()
    data object OpenNotifications : Action()
}
