// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.ui.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import zone.converge.android.data.SettingsRepository
import zone.converge.android.data.ThemeMode
import zone.converge.android.grpc.ConnectionState
import zone.converge.android.grpc.ConvergeClient

@DisplayName("SettingsViewModel")
class SettingsViewModelTest {

    private lateinit var mockRepository: SettingsRepository
    private lateinit var mockClient: ConvergeClient
    private lateinit var viewModel: SettingsViewModel

    private val hostFlow = MutableStateFlow("localhost")
    private val portFlow = MutableStateFlow(50051)
    private val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val connectionStateFlow = MutableStateFlow(ConnectionState.OFFLINE)

    @BeforeEach
    fun setup() {
        mockRepository = mockk(relaxed = true) {
            every { serverHost } returns hostFlow
            every { serverPort } returns portFlow
            every { themeMode } returns themeModeFlow
        }

        mockClient = mockk(relaxed = true) {
            every { connectionState } returns connectionStateFlow
        }

        viewModel = SettingsViewModel(mockRepository, mockClient)
    }

    @Nested
    @DisplayName("State exposure")
    inner class StateExposure {

        @Test
        @DisplayName("should expose server host from repository")
        fun exposesServerHost() = runTest {
            viewModel.serverHost.test {
                assertThat(awaitItem()).isEqualTo("localhost")

                hostFlow.value = "api.converge.zone"
                assertThat(awaitItem()).isEqualTo("api.converge.zone")

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("should expose server port from repository")
        fun exposesServerPort() = runTest {
            viewModel.serverPort.test {
                assertThat(awaitItem()).isEqualTo(50051)

                portFlow.value = 8080
                assertThat(awaitItem()).isEqualTo(8080)

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("should expose theme mode from repository")
        fun exposesThemeMode() = runTest {
            viewModel.themeMode.test {
                assertThat(awaitItem()).isEqualTo(ThemeMode.SYSTEM)

                themeModeFlow.value = ThemeMode.DARK
                assertThat(awaitItem()).isEqualTo(ThemeMode.DARK)

                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("should expose connection state from client")
        fun exposesConnectionState() = runTest {
            viewModel.connectionState.test {
                assertThat(awaitItem()).isEqualTo(ConnectionState.OFFLINE)

                connectionStateFlow.value = ConnectionState.STREAMING
                assertThat(awaitItem()).isEqualTo(ConnectionState.STREAMING)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Update methods")
    inner class UpdateMethods {

        @Test
        @DisplayName("should delegate host update to repository")
        fun delegatesHostUpdate() {
            viewModel.updateServerHost("new-host.local")

            verify { mockRepository.setServerHost("new-host.local") }
        }

        @Test
        @DisplayName("should delegate port update to repository")
        fun delegatesPortUpdate() {
            viewModel.updateServerPort(9090)

            verify { mockRepository.setServerPort(9090) }
        }

        @Test
        @DisplayName("should delegate theme mode update to repository")
        fun delegatesThemeModeUpdate() {
            viewModel.updateThemeMode(ThemeMode.LIGHT)

            verify { mockRepository.setThemeMode(ThemeMode.LIGHT) }
        }
    }

    @Nested
    @DisplayName("Connection state transitions")
    inner class ConnectionStateTransitions {

        @Test
        @DisplayName("should reflect STREAMING state")
        fun reflectsStreaming() = runTest {
            connectionStateFlow.value = ConnectionState.STREAMING
            assertThat(viewModel.connectionState.value).isEqualTo(ConnectionState.STREAMING)
        }

        @Test
        @DisplayName("should reflect RECONNECTING state")
        fun reflectsReconnecting() = runTest {
            connectionStateFlow.value = ConnectionState.RECONNECTING
            assertThat(viewModel.connectionState.value).isEqualTo(ConnectionState.RECONNECTING)
        }

        @Test
        @DisplayName("should reflect DEGRADED state")
        fun reflectsDegraded() = runTest {
            connectionStateFlow.value = ConnectionState.DEGRADED
            assertThat(viewModel.connectionState.value).isEqualTo(ConnectionState.DEGRADED)
        }

        @Test
        @DisplayName("should reflect OFFLINE state")
        fun reflectsOffline() = runTest {
            connectionStateFlow.value = ConnectionState.OFFLINE
            assertThat(viewModel.connectionState.value).isEqualTo(ConnectionState.OFFLINE)
        }
    }
}
