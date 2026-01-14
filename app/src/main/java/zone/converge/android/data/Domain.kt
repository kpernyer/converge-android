// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.data

import java.time.Instant

/**
 * Runtime Job - an instance of work being executed.
 * This represents a running job, not a JTBD.
 */
data class Job(
    val id: String,
    val title: String,
    val outcome: String,
    val packId: String,
    val jtbdId: String,
    val priority: Priority = Priority.NORMAL,
    val status: JobStatus = JobStatus.PENDING,
    val createdAt: Instant = Instant.now(),
)

enum class Priority { LOW, NORMAL, HIGH, URGENT }
enum class JobStatus { PENDING, IN_PROGRESS, BLOCKED, COMPLETED, CANCELLED }

/**
 * Template for starting jobs.
 */
data class Template(
    val id: String,
    val name: String,
    val description: String,
    val seedSchema: String,
)

/**
 * Artifact instance - a concrete output from a job.
 */
data class ArtifactInstance(
    val id: String,
    val type: ArtifactType,
    val title: String,
    val jobId: String,
    val content: ByteArray,
    val metadata: Map<String, String>,
    val createdAt: Instant = Instant.now(),
) {
    override fun equals(other: Any?) = other is ArtifactInstance && id == other.id
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
    data class ExecuteJTBD(val jtbdId: String) : Action()
    data class StartBlueprint(val blueprintId: String) : Action()
    data class ContinueBlueprint(val blueprintId: String) : Action()
    data object Refresh : Action()
    data object OpenSettings : Action()
    data object OpenNotifications : Action()
}

/**
 * User's domain preferences learned over time.
 * Matches iOS UserDomainPreferences.
 */
data class UserDomainPreferences(
    val frequentPacks: MutableMap<String, Int> = mutableMapOf(),
    val frequentJTBDs: MutableMap<String, Int> = mutableMapOf(),
    val preferredBlueprints: MutableMap<String, Int> = mutableMapOf(),
    var averageSessionJTBDs: Double = 0.0,
) {
    fun recordPackUsage(packId: String) {
        frequentPacks.merge(packId, 1) { a, b -> a + b }
    }

    fun recordJTBDUsage(jtbdId: String) {
        frequentJTBDs.merge(jtbdId, 1) { a, b -> a + b }
    }

    fun topPacks(limit: Int = 3): List<String> = frequentPacks.entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { it.key }

    fun topJTBDs(limit: Int = 5): List<String> = frequentJTBDs.entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { it.key }
}
