// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import zone.converge.android.data.ThemeMode
import zone.converge.android.ui.SmartActionScreen
import zone.converge.android.ui.SmartActionViewModel
import zone.converge.android.ui.navigation.ConvergeNavigationDrawer
import zone.converge.android.ui.settings.SettingsScreen
import zone.converge.android.ui.settings.SettingsViewModel
import zone.converge.android.ui.theme.ConvergeTheme

/**
 * Main Activity - entry point for Converge Android.
 *
 * Uses Jetpack Compose for UI with the Smart Action Screen
 * that predicts and surfaces the most likely user action.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ConvergeApp

        setContent {
            // Collect theme mode from settings
            val themeMode by app.settingsRepository.themeMode.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            ConvergeTheme(darkTheme = darkTheme) {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                // Navigation state
                var showSettings by remember { mutableStateOf(false) }

                val viewModel = remember {
                    SmartActionViewModel(
                        client = app.convergeClient,
                        jtbdPredictor = app.jtbdPredictor,
                        actionPredictor = app.actionPredictor,
                    )
                }

                val settingsViewModel = remember {
                    SettingsViewModel(
                        settingsRepository = app.settingsRepository,
                        convergeClient = app.convergeClient,
                    )
                }

                val selectedPackId by viewModel.selectedPackId.collectAsState()

                if (showSettings) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { showSettings = false },
                    )
                } else {
                    ConvergeNavigationDrawer(
                        drawerState = drawerState,
                        selectedPackId = selectedPackId,
                        onPackSelected = { packId ->
                            viewModel.selectPack(packId)
                        },
                        onBlueprintSelected = { blueprint ->
                            viewModel.startBlueprint(blueprint)
                        },
                        onSettingsClicked = {
                            showSettings = true
                        },
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            SmartActionScreen(
                                viewModel = viewModel,
                                onExecuteJTBD = { jtbd ->
                                    viewModel.submitProposal(jtbd)
                                },
                                onStartBlueprint = { blueprint ->
                                    viewModel.startBlueprint(blueprint)
                                },
                                onViewArtifact = { _ ->
                                    // TODO: Navigate to artifact detail
                                },
                                onOpenMenu = {
                                    scope.launch { drawerState.open() }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
