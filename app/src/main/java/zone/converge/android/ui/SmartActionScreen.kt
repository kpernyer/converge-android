// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zone.converge.android.data.*
import zone.converge.android.ml.*

/**
 * Smart Action Screen - The main UI that shows the most likely action prominently.
 *
 * Layout:
 * - Hero area: Primary predicted action (one tap)
 * - Quick actions: Secondary predictions (swipeable row)
 * - Activity feed: Recent jobs and artifacts
 * - Menu: Full navigation (drawer or bottom sheet)
 *
 * Goal: User reaches desired outcome with minimal taps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartActionScreen(
    viewModel: SmartActionViewModel,
    onExecuteJTBD: (JTBD) -> Unit,
    onStartBlueprint: (Blueprint) -> Unit,
    onViewArtifact: (String) -> Unit,
    onOpenMenu: () -> Unit,
) {
    val predictions by viewModel.predictions.collectAsState()
    val blueprintSuggestions by viewModel.blueprintSuggestions.collectAsState()
    val recentActivity by viewModel.recentActivity.collectAsState()

    val primaryPrediction = predictions.firstOrNull()
    val secondaryPredictions = predictions.drop(1).take(4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Converge") },
                actions = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                )
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero: Primary Action
            item {
                primaryPrediction?.let { prediction ->
                    PrimaryActionCard(
                        prediction = prediction,
                        onClick = { onExecuteJTBD(prediction.jtbd) },
                    )
                } ?: EmptyStateCard()
            }

            // Quick Actions Row
            if (secondaryPredictions.isNotEmpty()) {
                item {
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(secondaryPredictions) { prediction ->
                            QuickActionChip(
                                prediction = prediction,
                                onClick = { onExecuteJTBD(prediction.jtbd) },
                            )
                        }
                    }
                }
            }

            // Blueprints (if any suggested)
            if (blueprintSuggestions.isNotEmpty()) {
                item {
                    Text(
                        "Workflows",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(blueprintSuggestions.take(2)) { suggestion ->
                    BlueprintCard(
                        suggestion = suggestion,
                        onClick = { onStartBlueprint(suggestion.blueprint) },
                    )
                }
            }

            // Recent Activity
            if (recentActivity.isNotEmpty()) {
                item {
                    Text(
                        "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(recentActivity.take(5)) { activity ->
                    ActivityItem(
                        activity = activity,
                        onClick = { onViewArtifact(activity.id) },
                    )
                }
            }
        }
    }
}

/**
 * Primary action card - large, prominent, one-tap.
 */
@Composable
fun PrimaryActionCard(
    prediction: JTBDPrediction,
    onClick: () -> Unit,
) {
    val packColors = mapOf(
        "money" to listOf(Color(0xFF2E7D32), Color(0xFF4CAF50)),
        "customers" to listOf(Color(0xFF1565C0), Color(0xFF42A5F5)),
        "delivery" to listOf(Color(0xFFE65100), Color(0xFFFF9800)),
        "people" to listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC)),
        "trust" to listOf(Color(0xFF00695C), Color(0xFF26A69A)),
    )

    val colors = packColors[prediction.jtbd.packId] ?: listOf(Color(0xFF424242), Color(0xFF757575))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(colors)
                )
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Pack badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = getPackIcon(prediction.jtbd.packId),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        DomainKnowledgeBase.getPack(prediction.jtbd.packId)?.name ?: "",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // Title and outcome
                Column {
                    Text(
                        prediction.jtbd.title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        prediction.reason,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Confidence indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { prediction.confidence },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                    Text(
                        "${(prediction.confidence * 100).toInt()}% match",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            // Tap indicator
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = "Tap to start",
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp),
            )
        }
    }
}

/**
 * Quick action chip for secondary predictions.
 */
@Composable
fun QuickActionChip(
    prediction: JTBDPrediction,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = getPackIcon(prediction.jtbd.packId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                prediction.jtbd.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
        }
    }
}

/**
 * Blueprint card for multi-step workflows.
 */
@Composable
fun BlueprintCard(
    suggestion: BlueprintSuggestion,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    suggestion.blueprint.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    suggestion.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Progress dots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    suggestion.blueprint.jtbdSequence.forEach { jtbdId ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (jtbdId in suggestion.alreadyCompleted)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Activity item for recent activity feed.
 */
@Composable
fun ActivityItem(
    activity: ActivityRecord,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = getActivityIcon(activity.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                activity.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                activity.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            activity.timeAgo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Empty state when no predictions available.
 */
@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Ready to get started?",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                "Explore packs or start a workflow",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Helper functions
fun getPackIcon(packId: String): ImageVector = when (packId) {
    "money" -> Icons.Default.AttachMoney
    "customers" -> Icons.Default.People
    "delivery" -> Icons.Default.LocalShipping
    "people" -> Icons.Default.Badge
    "trust" -> Icons.Default.VerifiedUser
    else -> Icons.Default.Category
}

fun getActivityIcon(type: ActivityType): ImageVector = when (type) {
    ActivityType.JOB_COMPLETED -> Icons.Default.CheckCircle
    ActivityType.ARTIFACT_CREATED -> Icons.Default.Description
    ActivityType.DECISION_PENDING -> Icons.Default.HelpOutline
    ActivityType.NOTIFICATION -> Icons.Default.Notifications
}

/**
 * Activity record for feed.
 */
data class ActivityRecord(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: ActivityType,
    val timeAgo: String,
)

enum class ActivityType {
    JOB_COMPLETED, ARTIFACT_CREATED, DECISION_PENDING, NOTIFICATION
}
