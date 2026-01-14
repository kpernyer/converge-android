// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Theme mode options for the app.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Repository for app settings using SharedPreferences.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _serverHost = MutableStateFlow(prefs.getString(KEY_SERVER_HOST, DEFAULT_HOST) ?: DEFAULT_HOST)
    val serverHost: StateFlow<String> = _serverHost.asStateFlow()

    private val _serverPort = MutableStateFlow(prefs.getInt(KEY_SERVER_PORT, DEFAULT_PORT))
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.getOrNull(prefs.getInt(KEY_THEME_MODE, 0)) ?: ThemeMode.SYSTEM,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setServerHost(host: String) {
        _serverHost.value = host
        prefs.edit().putString(KEY_SERVER_HOST, host).apply()
    }

    fun setServerPort(port: Int) {
        _serverPort.value = port
        prefs.edit().putInt(KEY_SERVER_PORT, port).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
    }

    companion object {
        private const val PREFS_NAME = "converge_settings"
        private const val KEY_SERVER_HOST = "server_host"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_THEME_MODE = "theme_mode"

        const val DEFAULT_HOST = "localhost"
        const val DEFAULT_PORT = 50051
    }
}
