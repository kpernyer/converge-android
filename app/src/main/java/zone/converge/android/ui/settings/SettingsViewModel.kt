// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import zone.converge.android.data.SettingsRepository
import zone.converge.android.data.ThemeMode
import zone.converge.android.grpc.ConnectionState
import zone.converge.android.grpc.ConvergeClient

/**
 * ViewModel for the Settings screen.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val convergeClient: ConvergeClient,
) : ViewModel() {

    val serverHost: StateFlow<String> = settingsRepository.serverHost
    val serverPort: StateFlow<Int> = settingsRepository.serverPort
    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
    val connectionState: StateFlow<ConnectionState> = convergeClient.connectionState

    fun updateServerHost(host: String) {
        settingsRepository.setServerHost(host)
    }

    fun updateServerPort(port: Int) {
        settingsRepository.setServerPort(port)
    }

    fun updateThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}
