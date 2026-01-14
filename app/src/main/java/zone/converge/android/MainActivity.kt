// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import zone.converge.android.ui.SmartActionScreen
import zone.converge.android.ui.SmartActionViewModel
import zone.converge.android.ui.navigation.ConvergeNavigationDrawer
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
            ConvergeTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val viewModel = remember {
                    SmartActionViewModel(
                        client = app.convergeClient,
                        jtbdPredictor = app.jtbdPredictor,
                        actionPredictor = app.actionPredictor,
                    )
                }

                val selectedPackId by viewModel.selectedPackId.collectAsState()

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
                        // TODO: Navigate to settings screen
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
                            onViewArtifact = { artifactId ->
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
