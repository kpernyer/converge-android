// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import zone.converge.android.ui.SmartActionScreen
import zone.converge.android.ui.SmartActionViewModel
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel = remember {
                        SmartActionViewModel(
                            client = app.convergeClient,
                            jtbdPredictor = app.jtbdPredictor,
                            actionPredictor = app.actionPredictor,
                        )
                    }

                    SmartActionScreen(
                        viewModel = viewModel,
                        onExecuteJTBD = { jtbd ->
                            viewModel.executeJTBD(jtbd)
                        },
                        onStartBlueprint = { blueprint ->
                            viewModel.startBlueprint(blueprint)
                        },
                        onViewArtifact = { artifactId ->
                            // TODO: Navigate to artifact detail
                        },
                        onOpenMenu = {
                            // TODO: Open navigation drawer
                        },
                    )
                }
            }
        }
    }
}
