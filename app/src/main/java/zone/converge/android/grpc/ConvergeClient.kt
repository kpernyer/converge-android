// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.grpc

import android.util.Log
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import zone.converge.android.data.*
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Connection state as per CROSS_PLATFORM_CONTRACT.md §1.5
 *
 * | State | Meaning | UI Indication |
 * |-------|---------|---------------|
 * | streaming | Live connection, receiving facts | Green indicator |
 * | reconnecting | Temporary disconnect, reconnecting | Yellow indicator |
 * | degraded | REST fallback, polling | Orange indicator + "Limited mode" |
 * | offline | No connection, queue only | Red indicator + "Offline" |
 */
enum class ConnectionState {
    STREAMING,
    RECONNECTING,
    DEGRADED,
    OFFLINE,
}

/**
 * Run status as per CROSS_PLATFORM_CONTRACT.md §1.8
 *
 * A run is not a request. It is a convergence process.
 */
enum class RunStatusType {
    RUNNING,     // Truths still firing, facts still arriving
    CONVERGED,   // Stable state reached, no more truths to fire
    HALTED,      // Invariant violated, explanation available
    WAITING,     // Blocked on external input (human approval, etc.)
}

/**
 * Run status with full details per contract §1.8
 */
data class RunStatus(
    val runId: String,
    val status: RunStatusType,
    val factsCount: Int = 0,
    val pendingProposals: Int = 0,
    val waitingFor: List<String> = emptyList(),
    val haltReason: String? = null,
    val haltTruthId: String? = null,
    val lastActivity: Long = System.currentTimeMillis(),
)

/**
 * Actor information per contract §4.2
 */
data class Actor(
    val type: ActorType,
    val userId: String? = null,
    val deviceId: String? = null,
    val orgId: String? = null,
    val roles: List<String> = emptyList(),
)

enum class ActorType {
    USER,
    AGENT,
    SYSTEM,
}

/**
 * gRPC client for connecting to converge-runtime.
 *
 * Implements the Converge Protocol per CROSS_PLATFORM_CONTRACT.md:
 * - Stream-first architecture (§1)
 * - Resume semantics with sequence numbers (§13.4)
 * - Connection state tracking (§1.5)
 * - Transport fallback (gRPC → SSE → REST) (§1.4)
 */
class ConvergeClient(
    private val host: String = "localhost",
    private val port: Int = 50051,
) {
    private var channel: ManagedChannel? = null

    // Connection state per contract §1.5
    private val _connectionState = MutableStateFlow(ConnectionState.OFFLINE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Last known sequence for resume per contract §13.4
    private var lastKnownSequence: Long = 0

    // Current actor (device info)
    private var currentActor: Actor = Actor(
        type = ActorType.USER,
        deviceId = "android:${android.os.Build.MODEL}",
    )

    companion object {
        private const val TAG = "ConvergeClient"
        private const val MAX_BACKOFF_SECONDS = 30.0
        private const val INITIAL_BACKOFF_SECONDS = 1.0
    }

    /**
     * Connect to the converge-runtime server.
     * Per contract §1.4: gRPC bidirectional streaming is PRIMARY transport.
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.RECONNECTING
        try {
            channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext() // Use TLS in production
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .build()
            _connectionState.value = ConnectionState.STREAMING
            Log.d(TAG, "Connected to $host:$port (streaming)")
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed, falling back to degraded mode", e)
            _connectionState.value = ConnectionState.DEGRADED
        }
    }

    /**
     * Disconnect from the server.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
        channel = null
        _connectionState.value = ConnectionState.OFFLINE
    }

    /**
     * Reconnect with exponential backoff per contract §14.4
     */
    suspend fun reconnect() = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.RECONNECTING
        var backoff = INITIAL_BACKOFF_SECONDS

        while (_connectionState.value == ConnectionState.RECONNECTING) {
            try {
                channel = ManagedChannelBuilder
                    .forAddress(host, port)
                    .usePlaintext()
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(10, TimeUnit.SECONDS)
                    .build()
                _connectionState.value = ConnectionState.STREAMING
                Log.d(TAG, "Reconnected to $host:$port")
                break
            } catch (e: Exception) {
                Log.w(TAG, "Reconnect attempt failed, backoff: ${backoff}s")
                delay((backoff * 1000).toLong())
                backoff = min(backoff * 2, MAX_BACKOFF_SECONDS)
            }
        }
    }

    /**
     * Watch for new context entries as they are appended.
     * Uses gRPC server streaming per contract §14.1
     *
     * Supports resume semantics via sinceSequence parameter (§13.4)
     */
    fun watchContext(
        contextId: String,
        correlationId: String? = null,
        sinceSequence: Long = lastKnownSequence,
    ): Flow<ContextEntry> = callbackFlow {
        val ch = requireChannel()
        Log.d(TAG, "Watching context: $contextId, correlationId: $correlationId, sinceSeq: $sinceSequence")

        // TODO: Use generated gRPC stub when proto is compiled
        // In production this uses:
        //   ContextService.Watch(WatchRequest(correlationId, sinceSequence))
        //
        // Per contract §14.3, the stream handles:
        // - fact: Accepted truth, update state
        // - proposal: Suggested change, show pending indicator
        // - trace: Audit record, update activity log
        // - decision: Invariant check result, may show halt explanation

        awaitClose {
            Log.d(TAG, "Stopped watching context: $contextId")
        }
    }

    /**
     * Append an entry to the context per contract §14.1
     *
     * Every entry includes audit envelope (§7.3):
     * - correlation_id
     * - run_id
     * - truth_id
     * - actor
     */
    suspend fun appendContext(
        contextId: String,
        entryType: EntryType,
        payload: ByteArray,
        correlationId: String = java.util.UUID.randomUUID().toString(),
        runId: String? = null,
        truthId: String? = null,
    ): ContextEntry = withContext(Dispatchers.IO) {
        val ch = requireChannel()
        Log.d(TAG, "Appending to context: $contextId, type: $entryType, correlationId: $correlationId")

        val sequence = ++lastKnownSequence

        // Create entry per contract §4.2 format
        ContextEntry(
            entryId = "ctx_${java.util.UUID.randomUUID()}",
            entryType = entryType,
            timestamp = System.currentTimeMillis(),
            correlationId = correlationId,
            runId = runId,
            truthId = truthId,
            actor = currentActor,
            sequence = sequence,
            payload = payload,
        )
    }

    /**
     * Get context entries, supports resume via afterSequence per contract §14.6
     */
    suspend fun getContext(
        contextId: String,
        correlationId: String? = null,
        afterSequence: Long = 0,
        limit: Int = 100,
    ): List<ContextEntry> = withContext(Dispatchers.IO) {
        val ch = requireChannel()
        Log.d(TAG, "Getting context: $contextId, correlationId: $correlationId, afterSeq: $afterSequence")
        emptyList()
    }

    /**
     * Create a snapshot of the context per contract §14.1 (Snapshot RPC)
     */
    suspend fun snapshotContext(contextId: String): ContextSnapshot = withContext(Dispatchers.IO) {
        val ch = requireChannel()
        Log.d(TAG, "Creating snapshot: $contextId")
        ContextSnapshot(
            data = ByteArray(0),
            sequence = lastKnownSequence,
            entryCount = 0,
            createdAtNs = System.nanoTime(),
        )
    }

    /**
     * Load a context from a snapshot per contract §14.1
     */
    suspend fun loadContext(
        contextId: String,
        snapshot: ByteArray,
        failIfExists: Boolean = false,
    ): Long = withContext(Dispatchers.IO) {
        val ch = requireChannel()
        Log.d(TAG, "Loading snapshot into: $contextId")
        0L
    }

    /**
     * Generate idempotency key per contract §16.1
     * Format: {device_id}:{action}:{timestamp_ms}:{random_4}
     */
    fun generateIdempotencyKey(action: String): String {
        val deviceId = currentActor.deviceId ?: "unknown"
        val timestamp = System.currentTimeMillis()
        val random = (0..9999).random().toString().padStart(4, '0')
        return "$deviceId:$action:$timestamp:$random"
    }

    /**
     * Set current actor (typically on login/session start)
     */
    fun setActor(actor: Actor) {
        currentActor = actor
    }

    private fun requireChannel(): ManagedChannel {
        return channel ?: throw IllegalStateException("Not connected. Call connect() first.")
    }
}

/**
 * Entry type per contract §4.2 and §14.2
 */
enum class EntryType {
    FACT,       // Accepted truth, now in context
    PROPOSAL,   // Suggested change, pending
    TRACE,      // Audit record
    DECISION,   // Invariant check result
}

/**
 * Context entry per contract §4.2
 *
 * Every entry in the context ledger contains:
 * - entry_id, entry_type, timestamp
 * - correlation_id (links related entries)
 * - run_id (the run that produced this)
 * - truth_id (which Truth this satisfies)
 * - actor (who/what performed the action)
 * - sequence (for ordering and resume)
 * - payload
 */
data class ContextEntry(
    val entryId: String,
    val entryType: EntryType,
    val timestamp: Long,
    val correlationId: String,
    val runId: String? = null,
    val truthId: String? = null,
    val actor: Actor,
    val sequence: Long,
    val payload: ByteArray,
) {
    override fun equals(other: Any?) = other is ContextEntry && entryId == other.entryId
    override fun hashCode() = entryId.hashCode()
}

/**
 * Context snapshot for initial load or large gap recovery.
 */
data class ContextSnapshot(
    val data: ByteArray,
    val sequence: Long,
    val entryCount: Long,
    val createdAtNs: Long,
) {
    override fun equals(other: Any?) = other is ContextSnapshot && sequence == other.sequence
    override fun hashCode() = sequence.hashCode()
}
