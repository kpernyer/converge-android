// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.ml

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import zone.converge.android.data.*

/**
 * JTBD Predictor - Predicts the user's next Job To Be Done.
 *
 * Prediction strategies (in priority order):
 * 1. Blueprint-driven: Continue active blueprints
 * 2. Artifact flow: What uses what you just produced
 * 3. Frequency-based: Your most common actions at this time
 * 4. Pack affinity: Stay in your frequent domains
 * 5. Onboarding: Suggest starting points for new users
 *
 * Goal: Minimize clicks to reach desired outcome.
 */
class JTBDPredictor(
    private val outcomeTracker: OutcomeTracker,
) {
    private val _predictions = MutableStateFlow<List<JTBDPrediction>>(emptyList())
    val predictions = _predictions.asStateFlow()

    /**
     * Predict next JTBDs based on current context.
     */
    fun predict(context: DomainContext): List<JTBDPrediction> {
        val predictions = mutableListOf<JTBDPrediction>()

        // Strategy 1: Blueprint-driven (highest priority)
        context.activeBlueprints.forEach { (blueprintId, completedJtbds) ->
            DomainKnowledgeBase.suggestFromBlueprint(blueprintId, completedJtbds)?.let { jtbd ->
                predictions.add(
                    JTBDPrediction(
                        jtbd = jtbd,
                        confidence = 0.95f,
                        reason = "Continue ${DomainKnowledgeBase.getBlueprint(blueprintId)?.name}",
                        strategy = PredictionStrategy.BLUEPRINT,
                    ),
                )
            }
        }

        // Strategy 2: Artifact flow
        if (context.recentlyProducedArtifacts.isNotEmpty()) {
            val artifactSuggestions = DomainKnowledgeBase.suggestFromArtifacts(context.recentlyProducedArtifacts)
            artifactSuggestions.take(3).forEachIndexed { idx, jtbd ->
                predictions.add(
                    JTBDPrediction(
                        jtbd = jtbd,
                        confidence = 0.85f - (idx * 0.1f),
                        reason = "Uses your recent ${context.recentlyProducedArtifacts.first().name.lowercase().replace(
                            '_',
                            ' ',
                        )}",
                        strategy = PredictionStrategy.ARTIFACT_FLOW,
                    ),
                )
            }
        }

        // Strategy 3: Frequency-based from user patterns
        val frequencyPredictions = outcomeTracker.getFrequentJTBDs()
        frequencyPredictions.take(2).forEach { (jtbdId, count) ->
            DomainKnowledgeBase.getJTBD(jtbdId)?.let { jtbd ->
                if (predictions.none { it.jtbd.id == jtbdId }) {
                    predictions.add(
                        JTBDPrediction(
                            jtbd = jtbd,
                            confidence = 0.7f,
                            reason = "You do this often",
                            strategy = PredictionStrategy.FREQUENCY,
                        ),
                    )
                }
            }
        }

        // Strategy 4: Pack affinity
        val topPack = outcomeTracker.getTopPack()
        if (topPack != null) {
            val packJtbds = DomainKnowledgeBase.getJTBDsForPack(topPack)
            packJtbds
                .filter { jtbd -> predictions.none { it.jtbd.id == jtbd.id } }
                .take(2)
                .forEach { jtbd ->
                    predictions.add(
                        JTBDPrediction(
                            jtbd = jtbd,
                            confidence = 0.5f,
                            reason = "From your frequent ${DomainKnowledgeBase.getPack(topPack)?.name} pack",
                            strategy = PredictionStrategy.PACK_AFFINITY,
                        ),
                    )
                }
        }

        // Strategy 5: Onboarding suggestions for new users
        if (predictions.isEmpty()) {
            val starterJtbds = listOf("jtbd-capture-lead", "jtbd-issue-invoice", "jtbd-onboard-person")
            starterJtbds.mapNotNull { DomainKnowledgeBase.getJTBD(it) }.forEach { jtbd ->
                predictions.add(
                    JTBDPrediction(
                        jtbd = jtbd,
                        confidence = 0.3f,
                        reason = "Get started",
                        strategy = PredictionStrategy.ONBOARDING,
                    ),
                )
            }
        }

        // Sort by confidence and deduplicate
        val result = predictions
            .sortedByDescending { it.confidence }
            .distinctBy { it.jtbd.id }

        _predictions.value = result
        return result
    }

    /**
     * Get blueprints that the user might want to start.
     */
    fun suggestBlueprints(context: DomainContext): List<BlueprintSuggestion> {
        val suggestions = mutableListOf<BlueprintSuggestion>()

        // Check which blueprints align with recently completed JTBDs
        DomainKnowledgeBase.blueprints.forEach { blueprint ->
            val matchingJtbds = blueprint.jtbdSequence.filter { it in context.completedJtbds }
            if (matchingJtbds.isNotEmpty() && blueprint.id !in context.activeBlueprints.keys) {
                val progress = matchingJtbds.size.toFloat() / blueprint.jtbdSequence.size
                suggestions.add(
                    BlueprintSuggestion(
                        blueprint = blueprint,
                        confidence = 0.6f + (progress * 0.3f),
                        reason = "You've completed ${matchingJtbds.size}/${blueprint.jtbdSequence.size} steps",
                        alreadyCompleted = matchingJtbds.toSet(),
                    ),
                )
            }
        }

        // Also suggest blueprints based on pack affinity
        val topPack = outcomeTracker.getTopPack()
        if (topPack != null) {
            DomainKnowledgeBase.blueprints
                .filter { topPack in it.packIds && it.id !in context.activeBlueprints.keys }
                .filter { bp -> suggestions.none { it.blueprint.id == bp.id } }
                .forEach { blueprint ->
                    suggestions.add(
                        BlueprintSuggestion(
                            blueprint = blueprint,
                            confidence = 0.4f,
                            reason = "Matches your ${DomainKnowledgeBase.getPack(topPack)?.name} focus",
                            alreadyCompleted = emptySet(),
                        ),
                    )
                }
        }

        return suggestions.sortedByDescending { it.confidence }
    }

    /**
     * Record a completed JTBD for learning.
     */
    fun recordCompletion(jtbdId: String, producedArtifacts: Set<ArtifactType>) {
        outcomeTracker.recordJTBDCompletion(jtbdId, producedArtifacts)
    }
}

/**
 * Prediction strategy used.
 */
enum class PredictionStrategy {
    BLUEPRINT,
    ARTIFACT_FLOW,
    FREQUENCY,
    PACK_AFFINITY,
    ONBOARDING,
}

/**
 * A predicted JTBD with confidence and explanation.
 */
data class JTBDPrediction(
    val jtbd: JTBD,
    val confidence: Float,
    val reason: String,
    val strategy: PredictionStrategy,
)

/**
 * A suggested blueprint to start or continue.
 */
data class BlueprintSuggestion(
    val blueprint: Blueprint,
    val confidence: Float,
    val reason: String,
    val alreadyCompleted: Set<String>,
)

/**
 * Current domain context for predictions.
 */
data class DomainContext(
    val activeBlueprints: Map<String, Set<String>>, // blueprintId -> completed jtbds
    val recentlyProducedArtifacts: Set<ArtifactType>,
    val completedJtbds: Set<String>,
    val currentPackId: String?,
)

/**
 * Outcome Tracker - learns from user behavior.
 */
class OutcomeTracker(
    private val behaviorStore: BehaviorStore,
) {
    private val jtbdCompletions = mutableMapOf<String, Int>()
    private val packCompletions = mutableMapOf<String, Int>()
    private val artifactProductions = mutableMapOf<ArtifactType, Int>()

    /**
     * Record a JTBD completion.
     */
    fun recordJTBDCompletion(jtbdId: String, producedArtifacts: Set<ArtifactType>) {
        jtbdCompletions.merge(jtbdId, 1) { a, b -> a + b }

        DomainKnowledgeBase.getJTBD(jtbdId)?.let { jtbd ->
            packCompletions.merge(jtbd.packId, 1) { a, b -> a + b }
        }

        producedArtifacts.forEach { artifact ->
            artifactProductions.merge(artifact, 1) { a, b -> a + b }
        }
    }

    /**
     * Get frequently completed JTBDs.
     */
    fun getFrequentJTBDs(): List<Pair<String, Int>> = jtbdCompletions.entries
        .sortedByDescending { it.value }
        .map { it.key to it.value }

    /**
     * Get the most frequently used pack.
     */
    fun getTopPack(): String? = packCompletions.entries
        .maxByOrNull { it.value }
        ?.key

    /**
     * Get artifact production counts.
     */
    fun getArtifactStats(): Map<ArtifactType, Int> = artifactProductions.toMap()
}
