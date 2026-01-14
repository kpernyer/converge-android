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
import zone.converge.android.grpc.ConnectionState
import zone.converge.android.grpc.RunStatusType
import zone.converge.android.ml.*
import zone.converge.android.ui.theme.ConvergeColors
import zone.converge.android.ui.theme.ConvergeSpacing

/**
 * Smart Action Screen - The main UI that shows the most likely action prominently.
 *
 * Per CROSS_PLATFORM_CONTRACT.md:
 * - §1.5: Shows connection state indicator (streaming/reconnecting/degraded/offline)
 * - §5.1: Shows halt explanations when invariants violated
 * - §10: Treats predictions as proposals, not facts
 * - §12: UI state derived from context, not local flags
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
    // Per contract §10: These are PROPOSALS, not facts
    val proposals by viewModel.proposals.collectAsState()
    val blueprintSuggestions by viewModel.blueprintSuggestions.collectAsState()
    val recentActivity by viewModel.recentActivity.collectAsState()

    // Per contract §1.5: Connection state indicator
    val connectionState by viewModel.connectionState.collectAsState()

    // Per contract §1.8: Run status
    val runStatus by viewModel.runStatus.collectAsState()

    // Per contract §5.1: Halt explanation
    val haltExplanation by viewModel.haltExplanation.collectAsState()

    val primaryProposal = proposals.firstOrNull()
    val secondaryProposals = proposals.drop(1).take(4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Converge")
                        // Connection state indicator per contract §1.5
                        ConnectionIndicator(connectionState)
                    }
                },
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
            // Halt Explanation Banner per contract §5.1
            haltExplanation?.let { halt ->
                item {
                    HaltExplanationBanner(
                        explanation = halt,
                        onRetry = { viewModel.clearHaltAndRetry() },
                    )
                }
            }

            // Run Status Indicator per contract §1.8
            runStatus?.let { status ->
                if (status.status == RunStatusType.WAITING) {
                    item {
                        WaitingStatusBanner(
                            waitingFor = status.waitingFor,
                        )
                    }
                }
            }

            // Hero: Primary Action (per §10: this is a PROPOSAL, not a fact)
            item {
                primaryProposal?.let { proposal ->
                    PrimaryActionCard(
                        prediction = proposal,
                        onClick = { onExecuteJTBD(proposal.jtbd) },
                    )
                } ?: EmptyStateCard()
            }

            // Quick Actions Row (all proposals, not facts)
            if (secondaryProposals.isNotEmpty()) {
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
                        items(secondaryProposals) { proposal ->
                            QuickActionChip(
                                prediction = proposal,
                                onClick = { onExecuteJTBD(proposal.jtbd) },
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
    // Use theme colors for pack gradients
    val packColors = mapOf(
        "pack-money" to listOf(ConvergeColors.PackMoney, ConvergeColors.PackMoney.copy(alpha = 0.7f)),
        "pack-customers" to listOf(ConvergeColors.PackCustomers, ConvergeColors.PackCustomers.copy(alpha = 0.7f)),
        "pack-delivery" to listOf(ConvergeColors.PackDelivery, ConvergeColors.PackDelivery.copy(alpha = 0.7f)),
        "pack-people" to listOf(ConvergeColors.PackPeople, ConvergeColors.PackPeople.copy(alpha = 0.7f)),
        "pack-trust" to listOf(ConvergeColors.PackTrust, ConvergeColors.PackTrust.copy(alpha = 0.7f)),
    )

    val colors = packColors[prediction.jtbd.packId] ?: listOf(ConvergeColors.Ink, ConvergeColors.InkSecondary)

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
                        prediction.jtbd.name,
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
                        progress = prediction.confidence,
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
                prediction.jtbd.name,
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
    "pack-money" -> Icons.Default.AttachMoney
    "pack-customers" -> Icons.Default.People
    "pack-delivery" -> Icons.Default.LocalShipping
    "pack-people" -> Icons.Default.Badge
    "pack-trust" -> Icons.Default.VerifiedUser
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

/**
 * Connection state indicator per contract §1.5
 *
 * | State | Meaning | UI Indication |
 * |-------|---------|---------------|
 * | streaming | Live connection | Green indicator |
 * | reconnecting | Temporary disconnect | Yellow indicator |
 * | degraded | REST fallback | Orange indicator + "Limited mode" |
 * | offline | No connection | Red indicator + "Offline" |
 */
@Composable
fun ConnectionIndicator(state: ConnectionState) {
    when (state) {
        ConnectionState.STREAMING -> {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)) // Green
            )
        }
        ConnectionState.RECONNECTING -> {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEB3B)) // Yellow
            )
        }
        ConnectionState.DEGRADED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9800)) // Orange
                )
                Text(
                    "Limited",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9800),
                )
            }
        }
        ConnectionState.OFFLINE -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF44336)) // Red
                )
                Text(
                    "Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF44336),
                )
            }
        }
    }
}

/**
 * Halt explanation banner per contract §5.1
 * Shows: HALT → EXPLAIN → offer RESTART
 */
@Composable
fun HaltExplanationBanner(
    explanation: HaltExplanation,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    "Action blocked",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Text(
                explanation.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            explanation.truthId?.let { truthId ->
                Text(
                    "Truth: $truthId",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                )
            }
            if (explanation.canRetry) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onRetry) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

/**
 * Waiting status banner per contract §1.9
 * Shows what the run is waiting for.
 */
@Composable
fun WaitingStatusBanner(
    waitingFor: List<String>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
            Column {
                Text(
                    "Waiting for input",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                waitingFor.forEach { waiting ->
                    Text(
                        waiting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}
