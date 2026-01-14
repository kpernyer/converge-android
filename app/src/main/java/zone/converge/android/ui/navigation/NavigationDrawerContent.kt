// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import zone.converge.android.data.Blueprint
import zone.converge.android.data.DomainKnowledgeBase
import zone.converge.android.data.DomainPack
import zone.converge.android.ui.theme.ConvergeSpacing
import zone.converge.android.ui.theme.LocalConvergeColors

/**
 * Navigation drawer wrapper using Material 3 ModalNavigationDrawer.
 */
@Composable
fun ConvergeNavigationDrawer(
    drawerState: DrawerState,
    selectedPackId: String?,
    onPackSelected: (String) -> Unit,
    onBlueprintSelected: (Blueprint) -> Unit,
    onSettingsClicked: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            DrawerContent(
                selectedPackId = selectedPackId,
                drawerState = drawerState,
                onPackSelected = onPackSelected,
                onBlueprintSelected = onBlueprintSelected,
                onSettingsClicked = onSettingsClicked,
            )
        },
        content = content,
    )
}

/**
 * Drawer content with packs, blueprints, and settings.
 */
@Composable
private fun DrawerContent(
    selectedPackId: String?,
    drawerState: DrawerState,
    onPackSelected: (String) -> Unit,
    onBlueprintSelected: (Blueprint) -> Unit,
    onSettingsClicked: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val colors = LocalConvergeColors.current

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        // Header
        DrawerHeader()

        Divider(modifier = Modifier.padding(vertical = ConvergeSpacing.Space2))

        // Packs Section
        Text(
            text = "Packs",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.inkMuted,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = ConvergeSpacing.Space2),
        )

        DomainKnowledgeBase.packs.forEach { pack ->
            PackNavigationItem(
                pack = pack,
                selected = pack.id == selectedPackId,
                onClick = {
                    scope.launch { drawerState.close() }
                    onPackSelected(pack.id)
                },
            )
        }

        Divider(modifier = Modifier.padding(vertical = ConvergeSpacing.Space2))

        // Blueprints Section
        Text(
            text = "Blueprints",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.inkMuted,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = ConvergeSpacing.Space2),
        )

        DomainKnowledgeBase.blueprints.forEach { blueprint ->
            BlueprintNavigationItem(
                blueprint = blueprint,
                onClick = {
                    scope.launch { drawerState.close() }
                    onBlueprintSelected(blueprint)
                },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Divider()

        // Settings
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                )
            },
            label = { Text("Settings") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                onSettingsClicked()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )

        Spacer(modifier = Modifier.height(ConvergeSpacing.Space4))
    }
}

/**
 * Drawer header with app branding.
 */
@Composable
private fun DrawerHeader() {
    val colors = LocalConvergeColors.current

    Text(
        text = "Converge",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = colors.ink,
        modifier = Modifier.padding(
            horizontal = 28.dp,
            vertical = ConvergeSpacing.Space6,
        ),
    )
}

/**
 * Navigation item for a Pack with pack-specific colors.
 */
@Composable
private fun PackNavigationItem(pack: DomainPack, selected: Boolean, onClick: () -> Unit) {
    val packColor = getPackColorFromTheme(pack.id)

    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = getPackIcon(pack.id),
                contentDescription = null,
                tint = if (selected) packColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        label = {
            Text(
                text = pack.name,
                color = if (selected) packColor else MaterialTheme.colorScheme.onSurface,
            )
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = packColor.copy(alpha = 0.12f),
            selectedIconColor = packColor,
            selectedTextColor = packColor,
        ),
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

/**
 * Navigation item for a Blueprint.
 */
@Composable
private fun BlueprintNavigationItem(blueprint: Blueprint, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = getBlueprintIcon(blueprint.id),
                contentDescription = null,
            )
        },
        label = { Text(blueprint.name) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}
