// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.grpc

import android.util.Log
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import zone.converge.android.data.*
import java.util.concurrent.TimeUnit

/**
 * gRPC client for connecting to converge-runtime.
 *
 * Uses bidirectional streaming (HTTP/2) with coroutines for:
 * - Watching context updates in real-time
 * - Submitting jobs and receiving progress
 * - Fetching packs and templates
 */
class ConvergeClient(
    private val host: String = "localhost",
    private val port: Int = 50051,
) {
    private var channel: ManagedChannel? = null

    companion object {
        private const val TAG = "ConvergeClient"
    }

    /**
     * Connect to the converge-runtime server.
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext() // Use TLS in production
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .build()
        Log.d(TAG, "Connected to $host:$port")
    }

    /**
     * Disconnect from the server.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
        channel = null
    }

    /**
     * Watch for new context entries as they are appended.
     * Uses gRPC server streaming.
     */
    fun watchContext(contextId: String, key: String? = null): Flow<ContextEntry> = callbackFlow {
        val ch = requireChannel()
        // TODO: Use generated gRPC stub when proto is compiled
        // For now, provide a mock implementation
        Log.d(TAG, "Watching context: $contextId, key: $key")

        // Simulated streaming - in production this uses the ContextService.Watch RPC
        awaitClose {
            Log.d(TAG, "Stopped watching context: $contextId")
        }
    }

    /**
     * Append an entry to the context.
     */
    suspend fun appendContext(
        contextId: String,
        key: String,
        payload: ByteArray,
        metadata: Map<String, String> = emptyMap(),
    ): ContextEntry = withContext(Dispatchers.IO) {
        val ch = requireChannel()
        // TODO: Use generated gRPC stub
        Log.d(TAG, "Appending to context: $contextId, key: $key")

        // Simulated response
        ContextEntry(
            id = java.util.UUID.randomUUID().toString(),
            key = key,
            payload = payload,
            sequence = System.currentTimeMillis(),
            appendedAtNs = System.nanoTime(),
            metadata = metadata,
        )
    }

    /**
     * Get all entries for a context.
     */
    suspend fun getContext(
        contextId: String,
        key: String? = null,
        afterSequence: Long = 0,
        limit: Int = 100,
    ): List<ContextEntry> = withContext(Dispatchers.IO) {
        val ch = requireChannel()
        // TODO: Use generated gRPC stub
        Log.d(TAG, "Getting context: $contextId, key: $key, after: $afterSequence")
        emptyList()
    }

    /**
     * Create a snapshot of the context.
     */
    suspend fun snapshotContext(contextId: String): ContextSnapshot = withContext(Dispatchers.IO) {
        val ch = requireChannel()
        Log.d(TAG, "Creating snapshot: $contextId")
        ContextSnapshot(
            data = ByteArray(0),
            sequence = 0,
            entryCount = 0,
            createdAtNs = System.nanoTime(),
        )
    }

    /**
     * Load a context from a snapshot.
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

    private fun requireChannel(): ManagedChannel {
        return channel ?: throw IllegalStateException("Not connected. Call connect() first.")
    }
}

/**
 * Context entry from converge-runtime.
 */
data class ContextEntry(
    val id: String,
    val key: String,
    val payload: ByteArray,
    val sequence: Long,
    val appendedAtNs: Long,
    val metadata: Map<String, String>,
) {
    override fun equals(other: Any?) = other is ContextEntry && id == other.id
    override fun hashCode() = id.hashCode()
}

/**
 * Context snapshot.
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
