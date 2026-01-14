// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.grpc

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ConvergeClient")
class ConvergeClientTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var client: ConvergeClient

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        client = ConvergeClient("localhost", 50051)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial state")
    inner class InitialState {

        @Test
        @DisplayName("should be offline initially")
        fun offlineInitially() = runTest {
            assertThat(client.connectionState.value).isEqualTo(ConnectionState.OFFLINE)
        }
    }

    @Nested
    @DisplayName("generateIdempotencyKey")
    inner class GenerateIdempotencyKey {

        @Test
        @DisplayName("should generate key with action")
        fun generatesKeyWithAction() {
            val key = client.generateIdempotencyKey("submit-invoice")

            assertThat(key).contains(":submit-invoice:")
        }

        @Test
        @DisplayName("should generate unique keys for same action")
        fun generatesUniqueKeys() {
            val key1 = client.generateIdempotencyKey("test-action")
            val key2 = client.generateIdempotencyKey("test-action")

            assertThat(key1).isNotEqualTo(key2)
        }

        @Test
        @DisplayName("should have correct format")
        fun hasCorrectFormat() {
            val key = client.generateIdempotencyKey("my-action")

            // Format: {device_id}:{action}:{timestamp_ms}:{random_4}
            // Note: device_id contains "android:" prefix, so split results in more parts
            assertThat(key).contains(":my-action:")
            // Should end with 4-digit random
            val lastPart = key.split(":").last()
            assertThat(lastPart).hasLength(4)
            assertThat(lastPart.toIntOrNull()).isNotNull()
        }

        @Test
        @DisplayName("should handle action with special characters")
        fun handlesSpecialCharacters() {
            val key = client.generateIdempotencyKey("action/with\\special")

            assertThat(key).contains(":action/with\\special:")
        }

        @Test
        @DisplayName("should handle empty action")
        fun handlesEmptyAction() {
            val key = client.generateIdempotencyKey("")

            assertThat(key).contains("::")
        }
    }

    @Nested
    @DisplayName("setActor")
    inner class SetActor {

        @Test
        @DisplayName("should update actor for user")
        fun updatesActorForUser() {
            val actor = Actor(
                type = ActorType.USER,
                userId = "user-123",
                deviceId = "device-456",
                orgId = "org-789",
                roles = listOf("admin", "viewer"),
            )

            client.setActor(actor)

            // Idempotency key should now use new device ID
            val key = client.generateIdempotencyKey("test")
            assertThat(key).startsWith("device-456:")
        }

        @Test
        @DisplayName("should update actor for agent")
        fun updatesActorForAgent() {
            val actor = Actor(
                type = ActorType.AGENT,
                deviceId = "agent-001",
            )

            client.setActor(actor)

            val key = client.generateIdempotencyKey("test")
            assertThat(key).startsWith("agent-001:")
        }

        @Test
        @DisplayName("should handle null device ID")
        fun handlesNullDeviceId() {
            val actor = Actor(
                type = ActorType.SYSTEM,
                deviceId = null,
            )

            client.setActor(actor)

            val key = client.generateIdempotencyKey("test")
            assertThat(key).startsWith("unknown:")
        }
    }

    @Nested
    @DisplayName("ConnectionState flow")
    inner class ConnectionStateFlow {

        @Test
        @DisplayName("should emit connection state changes")
        fun emitsConnectionStateChanges() = runTest {
            client.connectionState.test {
                // Initial state
                assertThat(awaitItem()).isEqualTo(ConnectionState.OFFLINE)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("ConnectionState enum")
    inner class ConnectionStateEnum {

        @Test
        @DisplayName("should have all expected states")
        fun hasAllStates() {
            val states = ConnectionState.values()

            assertThat(states).hasLength(4)
            assertThat(states).asList().containsExactly(
                ConnectionState.STREAMING,
                ConnectionState.RECONNECTING,
                ConnectionState.DEGRADED,
                ConnectionState.OFFLINE,
            )
        }
    }

    @Nested
    @DisplayName("RunStatusType enum")
    inner class RunStatusTypeEnum {

        @Test
        @DisplayName("should have all expected status types")
        fun hasAllStatusTypes() {
            val types = RunStatusType.values()

            assertThat(types).hasLength(4)
            assertThat(types).asList().containsExactly(
                RunStatusType.RUNNING,
                RunStatusType.CONVERGED,
                RunStatusType.HALTED,
                RunStatusType.WAITING,
            )
        }
    }

    @Nested
    @DisplayName("ActorType enum")
    inner class ActorTypeEnum {

        @Test
        @DisplayName("should have all expected actor types")
        fun hasAllActorTypes() {
            val types = ActorType.values()

            assertThat(types).hasLength(3)
            assertThat(types).asList().containsExactly(
                ActorType.USER,
                ActorType.AGENT,
                ActorType.SYSTEM,
            )
        }
    }

    @Nested
    @DisplayName("EntryType enum")
    inner class EntryTypeEnum {

        @Test
        @DisplayName("should have all expected entry types")
        fun hasAllEntryTypes() {
            val types = EntryType.values()

            assertThat(types).hasLength(4)
            assertThat(types).asList().containsExactly(
                EntryType.FACT,
                EntryType.PROPOSAL,
                EntryType.TRACE,
                EntryType.DECISION,
            )
        }
    }

    @Nested
    @DisplayName("RunStatus data class")
    inner class RunStatusTests {

        @Test
        @DisplayName("should create with defaults")
        fun createsWithDefaults() {
            val status = RunStatus(
                runId = "run-123",
                status = RunStatusType.RUNNING,
            )

            assertThat(status.factsCount).isEqualTo(0)
            assertThat(status.pendingProposals).isEqualTo(0)
            assertThat(status.waitingFor).isEmpty()
            assertThat(status.haltReason).isNull()
            assertThat(status.haltTruthId).isNull()
        }

        @Test
        @DisplayName("should create HALTED status with reason")
        fun createsHaltedWithReason() {
            val status = RunStatus(
                runId = "run-456",
                status = RunStatusType.HALTED,
                haltReason = "Invariant violated: Insufficient balance",
                haltTruthId = "truth-balance-check",
            )

            assertThat(status.haltReason).isEqualTo("Invariant violated: Insufficient balance")
            assertThat(status.haltTruthId).isEqualTo("truth-balance-check")
        }

        @Test
        @DisplayName("should create WAITING status with waiting list")
        fun createsWaitingWithList() {
            val status = RunStatus(
                runId = "run-789",
                status = RunStatusType.WAITING,
                waitingFor = listOf("approval-manager", "approval-finance"),
            )

            assertThat(status.waitingFor).hasSize(2)
            assertThat(status.waitingFor).containsExactly("approval-manager", "approval-finance")
        }
    }

    @Nested
    @DisplayName("Actor data class")
    inner class ActorTests {

        @Test
        @DisplayName("should create USER actor with full details")
        fun createsUserActorWithDetails() {
            val actor = Actor(
                type = ActorType.USER,
                userId = "user-123",
                deviceId = "device-456",
                orgId = "org-789",
                roles = listOf("admin", "editor"),
            )

            assertThat(actor.type).isEqualTo(ActorType.USER)
            assertThat(actor.userId).isEqualTo("user-123")
            assertThat(actor.deviceId).isEqualTo("device-456")
            assertThat(actor.orgId).isEqualTo("org-789")
            assertThat(actor.roles).containsExactly("admin", "editor")
        }

        @Test
        @DisplayName("should create AGENT actor with defaults")
        fun createsAgentWithDefaults() {
            val actor = Actor(type = ActorType.AGENT)

            assertThat(actor.userId).isNull()
            assertThat(actor.deviceId).isNull()
            assertThat(actor.orgId).isNull()
            assertThat(actor.roles).isEmpty()
        }
    }

    @Nested
    @DisplayName("ContextEntry data class")
    inner class ContextEntryTests {

        private val testActor = Actor(type = ActorType.USER, userId = "test")

        @Test
        @DisplayName("should create FACT entry")
        fun createsFactEntry() {
            val entry = ContextEntry(
                entryId = "ctx_123",
                entryType = EntryType.FACT,
                timestamp = 1000L,
                correlationId = "corr-456",
                actor = testActor,
                sequence = 1L,
                payload = "test".toByteArray(),
            )

            assertThat(entry.entryType).isEqualTo(EntryType.FACT)
            assertThat(entry.runId).isNull()
            assertThat(entry.truthId).isNull()
        }

        @Test
        @DisplayName("should use entryId for equality")
        fun usesEntryIdForEquality() {
            val entry1 = ContextEntry(
                entryId = "ctx_same",
                entryType = EntryType.FACT,
                timestamp = 1000L,
                correlationId = "corr-1",
                actor = testActor,
                sequence = 1L,
                payload = "payload1".toByteArray(),
            )

            val entry2 = ContextEntry(
                entryId = "ctx_same",
                entryType = EntryType.PROPOSAL,
                timestamp = 2000L,
                correlationId = "corr-2",
                actor = testActor,
                sequence = 2L,
                payload = "payload2".toByteArray(),
            )

            assertThat(entry1).isEqualTo(entry2)
            assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode())
        }

        @Test
        @DisplayName("different entryIds should not be equal")
        fun differentEntryIdsNotEqual() {
            val entry1 = ContextEntry(
                entryId = "ctx_1",
                entryType = EntryType.FACT,
                timestamp = 1000L,
                correlationId = "corr-1",
                actor = testActor,
                sequence = 1L,
                payload = ByteArray(0),
            )

            val entry2 = ContextEntry(
                entryId = "ctx_2",
                entryType = EntryType.FACT,
                timestamp = 1000L,
                correlationId = "corr-1",
                actor = testActor,
                sequence = 1L,
                payload = ByteArray(0),
            )

            assertThat(entry1).isNotEqualTo(entry2)
        }
    }

    @Nested
    @DisplayName("ContextSnapshot data class")
    inner class ContextSnapshotTests {

        @Test
        @DisplayName("should create snapshot")
        fun createsSnapshot() {
            val snapshot = ContextSnapshot(
                data = "snapshot-data".toByteArray(),
                sequence = 100L,
                entryCount = 50L,
                createdAtNs = System.nanoTime(),
            )

            assertThat(snapshot.sequence).isEqualTo(100L)
            assertThat(snapshot.entryCount).isEqualTo(50L)
        }

        @Test
        @DisplayName("should use sequence for equality")
        fun usesSequenceForEquality() {
            val snapshot1 = ContextSnapshot(
                data = "data1".toByteArray(),
                sequence = 100L,
                entryCount = 10L,
                createdAtNs = 1000L,
            )

            val snapshot2 = ContextSnapshot(
                data = "data2".toByteArray(),
                sequence = 100L,
                entryCount = 20L,
                createdAtNs = 2000L,
            )

            assertThat(snapshot1).isEqualTo(snapshot2)
        }
    }

    @Nested
    @DisplayName("Property tests - Invariants")
    inner class PropertyTests {

        @Test
        @DisplayName("idempotency keys should contain action and end with random suffix")
        fun idempotencyKeysContainActionAndRandom() {
            repeat(100) {
                val key = client.generateIdempotencyKey("action-$it")
                assertThat(key).contains(":action-$it:")
                // Should end with 4-digit random
                val lastPart = key.split(":").last()
                assertThat(lastPart).hasLength(4)
            }
        }

        @Test
        @DisplayName("idempotency key random suffix should be 4 digits")
        fun idempotencyKeyRandomSuffix() {
            repeat(100) {
                val key = client.generateIdempotencyKey("action")
                val suffix = key.split(":").last()
                assertThat(suffix).hasLength(4)
                assertThat(suffix.toIntOrNull()).isNotNull()
            }
        }

        @Test
        @DisplayName("idempotency keys should be mostly unique")
        fun idempotencyKeysUnique() {
            // Keys include timestamp and random component
            // Due to fast execution, some may have same timestamp but random should differ
            val keys = (0 until 20).map { client.generateIdempotencyKey("action") }
            // Allow for some duplicates but expect high uniqueness
            assertThat(keys.toSet().size).isAtLeast(15)
        }
    }

    @Nested
    @DisplayName("Negative tests")
    inner class NegativeTests {

        @Test
        @DisplayName("should handle action with colons in idempotency key")
        fun handlesColonsInAction() {
            val key = client.generateIdempotencyKey("action:with:colons")

            // Colons in action will mess up the format - verify it still works
            assertThat(key).contains(":action:with:colons:")
        }

        @Test
        @DisplayName("should create Actor with empty roles list")
        fun emptyRolesList() {
            val actor = Actor(
                type = ActorType.USER,
                roles = emptyList(),
            )

            assertThat(actor.roles).isEmpty()
        }

        @Test
        @DisplayName("should create RunStatus with empty waiting list")
        fun emptyWaitingList() {
            val status = RunStatus(
                runId = "run-1",
                status = RunStatusType.WAITING,
                waitingFor = emptyList(),
            )

            assertThat(status.waitingFor).isEmpty()
        }
    }
}
