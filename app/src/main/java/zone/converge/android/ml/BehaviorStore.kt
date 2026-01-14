// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.ml

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persists user behavior patterns for action prediction learning.
 *
 * Stores:
 * - Action sequences (what actions follow what)
 * - Time-based patterns (morning vs evening behavior)
 * - Pack/flow affinity (which domains user frequents)
 * - Feature vectors for model fine-tuning
 */
class BehaviorStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("behavior_store", Context.MODE_PRIVATE)

    private val maxEvents = 1000
    private val maxSequenceLength = 50

    /**
     * Record an action event for learning.
     */
    suspend fun recordAction(event: ActionEvent) = withContext(Dispatchers.IO) {
        val events = getRecentEvents().toMutableList()
        events.add(event)

        // Trim to max size
        while (events.size > maxEvents) {
            events.removeFirst()
        }

        saveEvents(events)
        updateActionSequences(event)
        updateTimePatterns(event)
    }

    /**
     * Get recent action events.
     */
    suspend fun getRecentEvents(limit: Int = 100): List<ActionEvent> = withContext(Dispatchers.IO) {
        loadEvents().takeLast(limit)
    }

    /**
     * Get action transition probabilities.
     * Returns P(next_action | current_action)
     */
    suspend fun getTransitionProbabilities(currentAction: String): Map<String, Float> = withContext(Dispatchers.IO) {
        val sequences = prefs.getString("action_sequences", null) ?: return@withContext emptyMap()
        val counts = mutableMapOf<String, Int>()
        var total = 0

        sequences.split(";").forEach { seq ->
            val parts = seq.split("->")
            if (parts.size == 2 && parts[0] == currentAction) {
                val next = parts[1].substringBefore(":")
                val count = parts[1].substringAfter(":").toIntOrNull() ?: 1
                counts[next] = (counts[next] ?: 0) + count
                total += count
            }
        }

        if (total == 0) {
            emptyMap()
        } else {
            counts.mapValues { it.value.toFloat() / total }
        }
    }

    /**
     * Get hour-of-day action distribution.
     */
    suspend fun getHourlyPatterns(): Map<Int, Map<String, Float>> = withContext(Dispatchers.IO) {
        val patterns = mutableMapOf<Int, MutableMap<String, Int>>()

        loadEvents().forEach { event ->
            val hour = event.timestamp.atZone(java.time.ZoneId.systemDefault()).hour
            val actionType = event.action::class.simpleName ?: "Unknown"
            patterns.getOrPut(hour) { mutableMapOf() }
                .merge(actionType, 1) { a, b -> a + b }
        }

        patterns.mapValues { (_, counts) ->
            val total = counts.values.sum().toFloat()
            counts.mapValues { it.value / total }
        }
    }

    /**
     * Get most frequent packs.
     */
    suspend fun getFrequentPacks(): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        val packCounts = mutableMapOf<String, Int>()

        loadEvents().forEach { event ->
            val action = event.action
            if (action is zone.converge.android.data.Action.NavigateToPack) {
                packCounts.merge(action.packId, 1) { a, b -> a + b }
            }
        }

        packCounts.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    /**
     * Clear all stored behavior data.
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    private fun loadEvents(): List<ActionEvent> {
        // Simplified - in production use proper serialization
        val data = prefs.getString("events", null) ?: return emptyList()
        return try {
            // Parse stored events (simplified format)
            emptyList() // TODO: Implement proper deserialization
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveEvents(events: List<ActionEvent>) {
        // Simplified - in production use proper serialization
        prefs.edit().putInt("event_count", events.size).apply()
    }

    private fun updateActionSequences(event: ActionEvent) {
        val actionType = event.action::class.simpleName ?: return
        val lastAction = prefs.getString("last_action", null)

        if (lastAction != null) {
            val key = "$lastAction->$actionType"
            val count = prefs.getInt("seq_$key", 0) + 1
            prefs.edit()
                .putInt("seq_$key", count)
                .putString("last_action", actionType)
                .apply()
        } else {
            prefs.edit().putString("last_action", actionType).apply()
        }
    }

    private fun updateTimePatterns(event: ActionEvent) {
        val hour = event.timestamp.atZone(java.time.ZoneId.systemDefault()).hour
        val actionType = event.action::class.simpleName ?: return
        val key = "hour_${hour}_$actionType"
        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count).apply()
    }
}
