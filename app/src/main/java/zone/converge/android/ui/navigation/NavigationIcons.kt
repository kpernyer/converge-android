// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import zone.converge.android.ui.theme.ConvergeColors
import zone.converge.android.ui.theme.LocalConvergeColors

/**
 * Icon mapping for Packs.
 * Maps iOS SF Symbols to Material Design icons.
 */
fun getPackIcon(packId: String): ImageVector = when (packId) {
    "pack-money" -> Icons.Default.AttachMoney
    "pack-customers" -> Icons.Default.People
    "pack-delivery" -> Icons.Default.LocalShipping
    "pack-people" -> Icons.Default.Badge
    "pack-trust" -> Icons.Default.VerifiedUser
    else -> Icons.Default.LocalShipping
}

/**
 * Icon mapping for Blueprints.
 */
fun getBlueprintIcon(blueprintId: String): ImageVector = when (blueprintId) {
    "blueprint-lead-to-cash" -> Icons.Default.TrendingUp
    "blueprint-invoice-to-cash" -> Icons.Default.Receipt
    "blueprint-promise-to-delivery" -> Icons.Default.LocalShipping
    "blueprint-hire-to-productive" -> Icons.Default.PersonAdd
    "blueprint-compliance-cycle" -> Icons.Default.Security
    else -> Icons.Default.PlayArrow
}

/**
 * Get pack color from theme.
 */
fun getPackColor(packId: String): Color = when (packId) {
    "pack-money" -> ConvergeColors.PackMoney
    "pack-customers" -> ConvergeColors.PackCustomers
    "pack-delivery" -> ConvergeColors.PackDelivery
    "pack-people" -> ConvergeColors.PackPeople
    "pack-trust" -> ConvergeColors.PackTrust
    else -> ConvergeColors.Accent
}

/**
 * Get pack color from composition local (supports dark mode).
 */
@Composable
fun getPackColorFromTheme(packId: String): Color {
    val colors = LocalConvergeColors.current
    return when (packId) {
        "pack-money" -> colors.packMoney
        "pack-customers" -> colors.packCustomers
        "pack-delivery" -> colors.packDelivery
        "pack-people" -> colors.packPeople
        "pack-trust" -> colors.packTrust
        else -> colors.accent
    }
}
