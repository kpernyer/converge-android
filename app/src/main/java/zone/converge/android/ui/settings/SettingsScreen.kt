// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import zone.converge.android.BuildConfig
import zone.converge.android.data.ThemeMode
import zone.converge.android.grpc.ConnectionState
import zone.converge.android.ui.theme.ConvergeSpacing
import zone.converge.android.ui.theme.LocalConvergeColors

/**
 * Settings screen with Connection, Appearance, and About sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit) {
    val colors = LocalConvergeColors.current
    val serverHost by viewModel.serverHost.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = ConvergeSpacing.Space4),
            verticalArrangement = Arrangement.spacedBy(ConvergeSpacing.Space4),
        ) {
            // Connection Section
            item {
                SectionHeader("Connection")
            }

            item {
                OutlinedTextField(
                    value = serverHost,
                    onValueChange = { viewModel.updateServerHost(it) },
                    label = { Text("Server Host") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = serverPort.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { viewModel.updateServerPort(it) }
                    },
                    label = { Text("Server Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                ConnectionStatusRow(connectionState)
            }

            item {
                Spacer(modifier = Modifier.height(ConvergeSpacing.Space4))
            }

            // Appearance Section
            item {
                SectionHeader("Appearance")
            }

            item {
                ThemeSelector(
                    selectedMode = themeMode,
                    onModeSelected = { viewModel.updateThemeMode(it) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(ConvergeSpacing.Space4))
            }

            // About Section
            item {
                SectionHeader("About")
            }

            item {
                InfoRow(label = "Version", value = BuildConfig.VERSION_NAME)
            }

            item {
                InfoRow(label = "Build", value = BuildConfig.VERSION_CODE.toString())
            }

            item {
                Spacer(modifier = Modifier.height(ConvergeSpacing.Space8))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalConvergeColors.current

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = colors.ink,
        modifier = Modifier.padding(vertical = ConvergeSpacing.Space2),
    )
}

@Composable
private fun ConnectionStatusRow(connectionState: ConnectionState) {
    val colors = LocalConvergeColors.current

    val (statusColor, statusText) = when (connectionState) {
        ConnectionState.STREAMING -> colors.success to "Connected"
        ConnectionState.RECONNECTING -> colors.warning to "Reconnecting..."
        ConnectionState.DEGRADED -> colors.warning to "Limited Mode"
        ConnectionState.OFFLINE -> MaterialTheme.colorScheme.error to "Offline"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = ConvergeSpacing.Space2),
    ) {
        Text(
            text = "Status",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(12.dp),
        )

        Spacer(modifier = Modifier.width(ConvergeSpacing.Space2))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
        )
    }
}

@Composable
private fun ThemeSelector(selectedMode: ThemeMode, onModeSelected: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == selectedMode,
                        onClick = { onModeSelected(mode) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = ConvergeSpacing.Space2),
            ) {
                RadioButton(
                    selected = mode == selectedMode,
                    onClick = null,
                )

                Spacer(modifier = Modifier.width(ConvergeSpacing.Space3))

                Text(
                    text = when (mode) {
                        ThemeMode.SYSTEM -> "System default"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = LocalConvergeColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ConvergeSpacing.Space2),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
        )
    }
}
