// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import zone.converge.android.data.DomainKnowledgeBase
import zone.converge.android.ml.BlueprintSuggestion
import zone.converge.android.ml.JTBDPrediction
import zone.converge.android.ml.PredictionStrategy
import zone.converge.android.ui.*
import zone.converge.android.ui.theme.ConvergeTheme

/**
 * Screenshot tests for SmartActionScreen components.
 *
 * Uses Paparazzi for fast JVM-based screenshot testing (no emulator needed).
 * Golden images are stored in app/src/test/snapshots.
 *
 * Run with: ./gradlew testDebug --tests "*SmartActionScreenshotTest*"
 * Record new goldens: ./gradlew recordPaparazziDebug
 * Verify against goldens: ./gradlew verifyPaparazziDebug
 */
class SmartActionScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material3.Light.NoActionBar",
    )

    @Test
    fun primaryActionCard_money() {
        val jtbd = DomainKnowledgeBase.getJTBD("jtbd-issue-invoice")!!
        val prediction = JTBDPrediction(
            jtbd = jtbd,
            confidence = 0.92f,
            reason = "Customer awaiting invoice",
            strategy = PredictionStrategy.ARTIFACT_FLOW,
        )

        paparazzi.snapshot {
            ConvergeTheme {
                PrimaryActionCard(
                    prediction = prediction,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun primaryActionCard_customers() {
        val jtbd = DomainKnowledgeBase.getJTBD("jtbd-capture-lead")!!
        val prediction = JTBDPrediction(
            jtbd = jtbd,
            confidence = 0.85f,
            reason = "Start your day",
            strategy = PredictionStrategy.ONBOARDING,
        )

        paparazzi.snapshot {
            ConvergeTheme {
                PrimaryActionCard(
                    prediction = prediction,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun primaryActionCard_delivery() {
        val jtbd = DomainKnowledgeBase.getJTBD("jtbd-make-promise")!!
        val prediction = JTBDPrediction(
            jtbd = jtbd,
            confidence = 0.78f,
            reason = "New deal closed",
            strategy = PredictionStrategy.ARTIFACT_FLOW,
        )

        paparazzi.snapshot {
            ConvergeTheme {
                PrimaryActionCard(
                    prediction = prediction,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun quickActionChip() {
        val jtbd = DomainKnowledgeBase.getJTBD("jtbd-collect-payment")!!
        val prediction = JTBDPrediction(
            jtbd = jtbd,
            confidence = 0.65f,
            reason = "Invoice sent 3 days ago",
            strategy = PredictionStrategy.FREQUENCY,
        )

        paparazzi.snapshot {
            ConvergeTheme {
                QuickActionChip(
                    prediction = prediction,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun blueprintCard_inProgress() {
        val blueprint = DomainKnowledgeBase.getBlueprint("blueprint-lead-to-cash")!!
        val suggestion = BlueprintSuggestion(
            blueprint = blueprint,
            confidence = 0.88f,
            reason = "You've completed 2/5 steps",
            alreadyCompleted = setOf("jtbd-capture-lead", "jtbd-qualify-opportunity"),
        )

        paparazzi.snapshot {
            ConvergeTheme {
                BlueprintCard(
                    suggestion = suggestion,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun activityItem_completed() {
        val activity = ActivityRecord(
            id = "activity-1",
            title = "Invoice #2024-001 issued",
            subtitle = "Customer: Acme Corp",
            type = ActivityType.JOB_COMPLETED,
            timeAgo = "5 min ago",
        )

        paparazzi.snapshot {
            ConvergeTheme {
                ActivityItem(
                    activity = activity,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun activityItem_artifact() {
        val activity = ActivityRecord(
            id = "activity-2",
            title = "Proposal generated",
            subtitle = "Enterprise Software License",
            type = ActivityType.ARTIFACT_CREATED,
            timeAgo = "1 hour ago",
        )

        paparazzi.snapshot {
            ConvergeTheme {
                ActivityItem(
                    activity = activity,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun emptyStateCard() {
        paparazzi.snapshot {
            ConvergeTheme {
                EmptyStateCard()
            }
        }
    }

    @Test
    fun emptyStateCard_dark() {
        paparazzi.snapshot {
            ConvergeTheme(darkTheme = true) {
                EmptyStateCard()
            }
        }
    }
}
