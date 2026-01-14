// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.data

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SettingsRepository")
class SettingsRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repository: SettingsRepository

    @BeforeEach
    fun setup() {
        mockEditor = mockk(relaxed = true) {
            every { putString(any(), any()) } returns this
            every { putInt(any(), any()) } returns this
            every { apply() } returns Unit
        }

        mockPrefs = mockk {
            every { getString("server_host", any()) } returns "localhost"
            every { getInt("server_port", any()) } returns 50051
            every { getInt("theme_mode", any()) } returns 0
            every { edit() } returns mockEditor
        }

        mockContext = mockk {
            every { getSharedPreferences("converge_settings", Context.MODE_PRIVATE) } returns mockPrefs
        }

        repository = SettingsRepository(mockContext)
    }

    @Nested
    @DisplayName("Initial load")
    inner class InitialLoad {

        @Test
        @DisplayName("should load default host from preferences")
        fun loadsDefaultHost() = runTest {
            assertThat(repository.serverHost.value).isEqualTo("localhost")
        }

        @Test
        @DisplayName("should load default port from preferences")
        fun loadsDefaultPort() = runTest {
            assertThat(repository.serverPort.value).isEqualTo(50051)
        }

        @Test
        @DisplayName("should load theme mode SYSTEM by default")
        fun loadsDefaultTheme() = runTest {
            assertThat(repository.themeMode.value).isEqualTo(ThemeMode.SYSTEM)
        }

        @Test
        @DisplayName("should load LIGHT theme when ordinal is 1")
        fun loadsLightTheme() {
            every { mockPrefs.getInt("theme_mode", any()) } returns 1
            val repo = SettingsRepository(mockContext)
            assertThat(repo.themeMode.value).isEqualTo(ThemeMode.LIGHT)
        }

        @Test
        @DisplayName("should load DARK theme when ordinal is 2")
        fun loadsDarkTheme() {
            every { mockPrefs.getInt("theme_mode", any()) } returns 2
            val repo = SettingsRepository(mockContext)
            assertThat(repo.themeMode.value).isEqualTo(ThemeMode.DARK)
        }
    }

    @Nested
    @DisplayName("setServerHost")
    inner class SetServerHost {

        @Test
        @DisplayName("should update StateFlow value")
        fun updatesStateFlow() = runTest {
            repository.setServerHost("192.168.1.100")
            assertThat(repository.serverHost.value).isEqualTo("192.168.1.100")
        }

        @Test
        @DisplayName("should persist to SharedPreferences")
        fun persistsToPrefs() {
            repository.setServerHost("api.converge.zone")

            verify { mockEditor.putString("server_host", "api.converge.zone") }
            verify { mockEditor.apply() }
        }

        @Test
        @DisplayName("should handle empty string")
        fun handlesEmptyString() = runTest {
            repository.setServerHost("")
            assertThat(repository.serverHost.value).isEmpty()
        }
    }

    @Nested
    @DisplayName("setServerPort")
    inner class SetServerPort {

        @Test
        @DisplayName("should update StateFlow value")
        fun updatesStateFlow() = runTest {
            repository.setServerPort(8080)
            assertThat(repository.serverPort.value).isEqualTo(8080)
        }

        @Test
        @DisplayName("should persist to SharedPreferences")
        fun persistsToPrefs() {
            repository.setServerPort(443)

            verify { mockEditor.putInt("server_port", 443) }
            verify { mockEditor.apply() }
        }

        @Test
        @DisplayName("should handle port 0")
        fun handlesZeroPort() = runTest {
            repository.setServerPort(0)
            assertThat(repository.serverPort.value).isEqualTo(0)
        }

        @Test
        @DisplayName("should handle max port number")
        fun handlesMaxPort() = runTest {
            repository.setServerPort(65535)
            assertThat(repository.serverPort.value).isEqualTo(65535)
        }
    }

    @Nested
    @DisplayName("setThemeMode")
    inner class SetThemeMode {

        @Test
        @DisplayName("should update StateFlow to LIGHT")
        fun updatesLightTheme() = runTest {
            repository.setThemeMode(ThemeMode.LIGHT)
            assertThat(repository.themeMode.value).isEqualTo(ThemeMode.LIGHT)
        }

        @Test
        @DisplayName("should update StateFlow to DARK")
        fun updatesDarkTheme() = runTest {
            repository.setThemeMode(ThemeMode.DARK)
            assertThat(repository.themeMode.value).isEqualTo(ThemeMode.DARK)
        }

        @Test
        @DisplayName("should persist LIGHT as ordinal 1")
        fun persistsLightOrdinal() {
            repository.setThemeMode(ThemeMode.LIGHT)

            verify { mockEditor.putInt("theme_mode", 1) }
            verify { mockEditor.apply() }
        }

        @Test
        @DisplayName("should persist DARK as ordinal 2")
        fun persistsDarkOrdinal() {
            repository.setThemeMode(ThemeMode.DARK)

            verify { mockEditor.putInt("theme_mode", 2) }
            verify { mockEditor.apply() }
        }

        @Test
        @DisplayName("should persist SYSTEM as ordinal 0")
        fun persistsSystemOrdinal() {
            repository.setThemeMode(ThemeMode.SYSTEM)

            verify { mockEditor.putInt("theme_mode", 0) }
        }
    }

    @Nested
    @DisplayName("Negative tests")
    inner class NegativeTests {

        @Test
        @DisplayName("should fallback to SYSTEM when theme ordinal is invalid")
        fun fallbackOnInvalidThemeOrdinal() {
            every { mockPrefs.getInt("theme_mode", any()) } returns 99
            val repo = SettingsRepository(mockContext)
            assertThat(repo.themeMode.value).isEqualTo(ThemeMode.SYSTEM)
        }

        @Test
        @DisplayName("should use default host when preference returns null")
        fun fallbackOnNullHost() {
            every { mockPrefs.getString("server_host", any()) } returns null
            val repo = SettingsRepository(mockContext)
            assertThat(repo.serverHost.value).isEqualTo("localhost")
        }
    }
}
