// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.ml

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import org.tensorflow.lite.Interpreter
import zone.converge.android.data.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Smart Action Prediction Layer
 *
 * This layer sits between the data layer and UI rendering to predict
 * which action the user most likely wants to perform next.
 *
 * The prediction is based on:
 * 1. Current context (active jobs, recent artifacts, time of day)
 * 2. User behavior history (action sequences, frequency patterns)
 * 3. Domain knowledge (flow dependencies, pack relationships)
 * 4. On-device ML model (TFLite) for pattern recognition
 *
 * Goal: Minimize clicks to reach desired outcome.
 */
class ActionPredictor(
    private val context: Context,
    private val behaviorStore: BehaviorStore,
) {
    private var interpreter: Interpreter? = null
    private val _predictions = MutableStateFlow<List<PredictedAction>>(emptyList())
    val predictions = _predictions.asStateFlow()

    // Feature vector size for ML model
    private val featureSize = 64

    /**
     * Initialize TFLite model for action prediction.
     * Falls back to heuristics if model not available.
     */
    fun initialize() {
        try {
            val modelBuffer = loadModelFile("action_predictor.tflite")
            if (modelBuffer != null) {
                interpreter = Interpreter(modelBuffer)
            }
        } catch (e: Exception) {
            // Model not available, use heuristic fallback
            interpreter = null
        }
    }

    /**
     * Predict the next most likely actions given current state.
     *
     * @param userContext Current user context (jobs, artifacts, etc.)
     * @return Ranked list of predicted actions with confidence scores
     */
    suspend fun predict(userContext: UserContext): List<PredictedAction> {
        val features = extractFeatures(userContext)
        val scores = interpreter?.let { runModel(it, features) }
            ?: heuristicPrediction(userContext)

        val predictions = rankActions(userContext, scores)
        _predictions.value = predictions
        return predictions
    }

    /**
     * Record that user took an action (for learning).
     */
    suspend fun recordAction(action: Action, context: UserContext) {
        behaviorStore.recordAction(
            ActionEvent(
                action = action,
                timestamp = Instant.now(),
                contextHash = context.hashCode(),
                features = extractFeatures(context),
            )
        )
    }

    /**
     * Extract feature vector from current context for ML model.
     */
    private fun extractFeatures(ctx: UserContext): FloatArray {
        val features = FloatArray(featureSize)
        var idx = 0

        // Time features (8 features)
        val now = LocalDateTime.now()
        features[idx++] = now.hour / 24f
        features[idx++] = now.minute / 60f
        features[idx++] = now.dayOfWeek.value / 7f
        features[idx++] = if (now.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) 1f else 0f
        features[idx++] = now.dayOfMonth / 31f
        features[idx++] = now.monthValue / 12f
        features[idx++] = if (now.hour in 9..17) 1f else 0f // Work hours
        features[idx++] = if (now.hour in 6..9) 1f else 0f   // Morning

        // Job status features (8 features)
        val jobs = ctx.activeJobs
        features[idx++] = jobs.size.coerceAtMost(10) / 10f
        features[idx++] = jobs.count { it.status == JobStatus.IN_PROGRESS } / 10f
        features[idx++] = jobs.count { it.status == JobStatus.BLOCKED } / 10f
        features[idx++] = jobs.count { it.priority == Priority.URGENT } / 10f
        features[idx++] = jobs.count { it.priority == Priority.HIGH } / 10f
        features[idx++] = if (jobs.any { it.status == JobStatus.BLOCKED }) 1f else 0f
        features[idx++] = ctx.pendingDecisions.size.coerceAtMost(5) / 5f
        features[idx++] = ctx.unreadNotifications.coerceAtMost(20) / 20f

        // Recent behavior features (16 features)
        val recentActions = ctx.recentActions.takeLast(10)
        val actionCounts = recentActions.groupingBy { it::class.simpleName }.eachCount()
        features[idx++] = actionCounts.getOrDefault("ViewJob", 0) / 10f
        features[idx++] = actionCounts.getOrDefault("ViewArtifact", 0) / 10f
        features[idx++] = actionCounts.getOrDefault("StartJob", 0) / 10f
        features[idx++] = actionCounts.getOrDefault("ApproveDecision", 0) / 10f
        features[idx++] = actionCounts.getOrDefault("NavigateToPack", 0) / 10f
        features[idx++] = recentActions.size / 10f

        // Time since last action
        val timeSinceLastAction = ctx.lastActionTime?.let {
            (Instant.now().epochSecond - it.epochSecond) / 3600f
        } ?: 1f
        features[idx++] = timeSinceLastAction.coerceAtMost(24f) / 24f

        // Pack affinity (which packs user interacts with most)
        val packCounts = ctx.recentActions
            .filterIsInstance<Action.NavigateToPack>()
            .groupingBy { it.packId }
            .eachCount()
        features[idx++] = packCounts.values.maxOrNull()?.div(10f) ?: 0f

        // Fill remaining with recency-weighted action embeddings
        while (idx < featureSize) {
            features[idx++] = 0f
        }

        return features
    }

    /**
     * Run TFLite model to get action scores.
     */
    private fun runModel(interpreter: Interpreter, features: FloatArray): FloatArray {
        val inputBuffer = ByteBuffer.allocateDirect(featureSize * 4)
            .order(ByteOrder.nativeOrder())
        features.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        val outputBuffer = ByteBuffer.allocateDirect(ActionType.entries.size * 4)
            .order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        return FloatArray(ActionType.entries.size) { outputBuffer.float }
    }

    /**
     * Heuristic fallback when ML model is not available.
     * Uses rule-based scoring based on context.
     */
    private fun heuristicPrediction(ctx: UserContext): FloatArray {
        val scores = FloatArray(ActionType.entries.size) { 0.1f }

        // High priority: Pending decisions need attention
        if (ctx.pendingDecisions.isNotEmpty()) {
            scores[ActionType.APPROVE_DECISION.ordinal] = 0.9f
        }

        // Blocked jobs need attention
        val blockedJobs = ctx.activeJobs.filter { it.status == JobStatus.BLOCKED }
        if (blockedJobs.isNotEmpty()) {
            scores[ActionType.VIEW_JOB.ordinal] = 0.85f
        }

        // In-progress jobs - user likely wants to check status
        val inProgress = ctx.activeJobs.filter { it.status == JobStatus.IN_PROGRESS }
        if (inProgress.isNotEmpty()) {
            scores[ActionType.VIEW_JOB.ordinal] = maxOf(scores[ActionType.VIEW_JOB.ordinal], 0.7f)
        }

        // New artifacts to review
        if (ctx.newArtifacts.isNotEmpty()) {
            scores[ActionType.VIEW_ARTIFACT.ordinal] = 0.75f
        }

        // Notifications
        if (ctx.unreadNotifications > 0) {
            scores[ActionType.OPEN_NOTIFICATIONS.ordinal] = 0.6f + (ctx.unreadNotifications * 0.02f)
        }

        // Morning = more likely to start new work
        val hour = LocalDateTime.now().hour
        if (hour in 8..10) {
            scores[ActionType.START_JOB.ordinal] += 0.2f
        }

        // End of day = more likely to review/export
        if (hour in 16..18) {
            scores[ActionType.EXPORT_DATA.ordinal] += 0.15f
            scores[ActionType.VIEW_ARTIFACT.ordinal] += 0.1f
        }

        // Recent action continuation patterns
        ctx.recentActions.lastOrNull()?.let { lastAction ->
            when (lastAction) {
                is Action.ViewJob -> {
                    // After viewing job, likely to approve or view artifact
                    scores[ActionType.APPROVE_DECISION.ordinal] += 0.15f
                    scores[ActionType.VIEW_ARTIFACT.ordinal] += 0.1f
                }
                is Action.StartJob -> {
                    // After starting, likely to view the job
                    scores[ActionType.VIEW_JOB.ordinal] += 0.3f
                }
                is Action.ViewArtifact -> {
                    // After viewing artifact, might export or start related job
                    scores[ActionType.EXPORT_DATA.ordinal] += 0.2f
                }
                else -> {}
            }
        }

        return scores
    }

    /**
     * Convert model scores to ranked PredictedAction list.
     */
    private fun rankActions(ctx: UserContext, scores: FloatArray): List<PredictedAction> {
        val candidates = mutableListOf<PredictedAction>()

        // Map scores to concrete actions based on context
        ActionType.entries.forEachIndexed { idx, actionType ->
            val baseScore = scores[idx]
            when (actionType) {
                ActionType.VIEW_JOB -> {
                    ctx.activeJobs
                        .sortedByDescending { jobPriority(it) }
                        .take(3)
                        .forEachIndexed { i, job ->
                            candidates.add(
                                PredictedAction(
                                    action = Action.ViewJob(job.id),
                                    confidence = baseScore * (1f - i * 0.1f),
                                    reason = "Job: ${job.title}",
                                    category = ActionCategory.JOBS,
                                )
                            )
                        }
                }
                ActionType.APPROVE_DECISION -> {
                    ctx.pendingDecisions.take(2).forEach { decision ->
                        candidates.add(
                            PredictedAction(
                                action = Action.ApproveDecision(decision.id),
                                confidence = baseScore,
                                reason = "Decision pending: ${decision.title}",
                                category = ActionCategory.DECISIONS,
                            )
                        )
                    }
                }
                ActionType.VIEW_ARTIFACT -> {
                    ctx.newArtifacts.take(2).forEach { artifact ->
                        candidates.add(
                            PredictedAction(
                                action = Action.ViewArtifact(artifact.id),
                                confidence = baseScore,
                                reason = "New: ${artifact.title}",
                                category = ActionCategory.ARTIFACTS,
                            )
                        )
                    }
                }
                ActionType.START_JOB -> {
                    ctx.suggestedTemplates.take(2).forEach { template ->
                        candidates.add(
                            PredictedAction(
                                action = Action.StartJob(template.id, emptyMap()),
                                confidence = baseScore,
                                reason = "Start: ${template.name}",
                                category = ActionCategory.CREATE,
                            )
                        )
                    }
                }
                ActionType.OPEN_NOTIFICATIONS -> {
                    if (ctx.unreadNotifications > 0) {
                        candidates.add(
                            PredictedAction(
                                action = Action.OpenNotifications,
                                confidence = baseScore,
                                reason = "${ctx.unreadNotifications} unread",
                                category = ActionCategory.SYSTEM,
                            )
                        )
                    }
                }
                ActionType.NAVIGATE_TO_PACK -> {
                    ctx.frequentPacks.take(2).forEach { pack ->
                        candidates.add(
                            PredictedAction(
                                action = Action.NavigateToPack(pack.id),
                                confidence = baseScore * 0.5f,
                                reason = pack.name,
                                category = ActionCategory.NAVIGATION,
                            )
                        )
                    }
                }
                ActionType.EXPORT_DATA -> {
                    ctx.exportableArtifacts.firstOrNull()?.let { artifact ->
                        candidates.add(
                            PredictedAction(
                                action = Action.ExportData(artifact.id, "pdf"),
                                confidence = baseScore,
                                reason = "Export: ${artifact.title}",
                                category = ActionCategory.ARTIFACTS,
                            )
                        )
                    }
                }
                ActionType.REFRESH -> {
                    candidates.add(
                        PredictedAction(
                            action = Action.Refresh,
                            confidence = 0.1f,
                            reason = "Refresh data",
                            category = ActionCategory.SYSTEM,
                        )
                    )
                }
                ActionType.OPEN_SETTINGS -> {
                    candidates.add(
                        PredictedAction(
                            action = Action.OpenSettings,
                            confidence = 0.05f,
                            reason = "Settings",
                            category = ActionCategory.SYSTEM,
                        )
                    )
                }
            }
        }

        return candidates
            .sortedByDescending { it.confidence }
            .distinctBy { it.action::class to actionKey(it.action) }
    }

    private fun jobPriority(job: Job): Float {
        var score = 0f
        score += when (job.priority) {
            Priority.URGENT -> 1.0f
            Priority.HIGH -> 0.7f
            Priority.NORMAL -> 0.4f
            Priority.LOW -> 0.2f
        }
        score += when (job.status) {
            JobStatus.BLOCKED -> 0.5f
            JobStatus.IN_PROGRESS -> 0.3f
            JobStatus.PENDING -> 0.1f
            else -> 0f
        }
        return score
    }

    private fun actionKey(action: Action): String = when (action) {
        is Action.ViewJob -> action.jobId
        is Action.ViewArtifact -> action.artifactId
        is Action.ApproveDecision -> action.decisionId
        is Action.NavigateToPack -> action.packId
        is Action.StartJob -> action.templateId
        is Action.ExportData -> action.artifactId
        else -> ""
    }

    private fun loadModelFile(filename: String): ByteBuffer? {
        return try {
            context.assets.open(filename).use { stream ->
                val bytes = stream.readBytes()
                ByteBuffer.allocateDirect(bytes.size).apply {
                    order(ByteOrder.nativeOrder())
                    put(bytes)
                    rewind()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        interpreter?.close()
    }
}

/**
 * Action types for ML model output mapping.
 */
enum class ActionType {
    VIEW_JOB,
    VIEW_ARTIFACT,
    START_JOB,
    APPROVE_DECISION,
    NAVIGATE_TO_PACK,
    EXPORT_DATA,
    OPEN_NOTIFICATIONS,
    REFRESH,
    OPEN_SETTINGS,
}

enum class ActionCategory {
    JOBS, ARTIFACTS, DECISIONS, CREATE, NAVIGATION, SYSTEM
}

/**
 * A predicted action with confidence score and explanation.
 */
data class PredictedAction(
    val action: Action,
    val confidence: Float,
    val reason: String,
    val category: ActionCategory,
)

/**
 * Current user context used for prediction.
 */
data class UserContext(
    val activeJobs: List<Job>,
    val pendingDecisions: List<Decision>,
    val newArtifacts: List<Artifact>,
    val exportableArtifacts: List<Artifact>,
    val recentActions: List<Action>,
    val lastActionTime: Instant?,
    val unreadNotifications: Int,
    val frequentPacks: List<Pack>,
    val suggestedTemplates: List<Template>,
)

data class Decision(
    val id: String,
    val title: String,
    val jobId: String,
    val options: List<String>,
)

/**
 * Recorded action event for learning.
 */
data class ActionEvent(
    val action: Action,
    val timestamp: Instant,
    val contextHash: Int,
    val features: FloatArray,
) {
    override fun equals(other: Any?) = other is ActionEvent && timestamp == other.timestamp
    override fun hashCode() = timestamp.hashCode()
}
