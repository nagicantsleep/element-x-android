/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.components

import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.home.impl.HomeNavigationBarItem
import io.element.android.libraries.designsystem.theme.components.NavigationBar
import io.element.android.libraries.designsystem.theme.components.NavigationBarIcon
import io.element.android.libraries.designsystem.theme.components.NavigationBarItem
import io.element.android.libraries.designsystem.theme.components.NavigationBarText

/**
 * The Z/L bottom navigation bar.
 *
 * Follows the Zalo/LINE convention the layout is modelled on:
 * - a full-width bar with the icon **on top** and the label **underneath**;
 * - the selected destination is tinted with the brand accent (blue for Z, green for L) and uses a
 *   filled icon, so the active tab is obvious;
 * - a count badge on Messages (rooms with new content) and on Contacts (pending invites) signals new
 *   messages or new contacts.
 */
@Composable
fun HomeNavigationBar(
    selectedItem: HomeNavigationBarItem,
    messagesBadgeCount: Int,
    contactsBadgeCount: Int,
    onItemClick: (HomeNavigationBarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        HomeNavigationBarItem.entries.forEach { item ->
            val isSelected = item == selectedItem
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item) },
                icon = {
                    NavigationBarIcon(
                        imageVector = item.icon(isSelected = isSelected),
                        count = when (item) {
                            HomeNavigationBarItem.Messages -> messagesBadgeCount
                            HomeNavigationBarItem.Contacts -> contactsBadgeCount
                            else -> 0
                        },
                        // Zalo/LINE show unread counts as a red badge regardless of the accent
                        // colour, which also keeps the badge legible when the tab is selected
                        // (its icon is already tinted with the accent).
                        isCritical = true,
                    )
                },
                label = { NavigationBarText(text = stringResource(item.labelRes)) },
                colors = homeNavigationBarItemColors(),
            )
        }
    }
}

/**
 * Selected state is tinted with the brand accent family, which [io.element.android.libraries.designsystem.theme.BrandTheme]
 * swaps at runtime, so the highlight follows the selected Z/L theme.
 */
@Composable
private fun homeNavigationBarItemColors() = NavigationBarItemDefaults.colors().copy(
    selectedIconColor = ElementTheme.colors.iconAccentPrimary,
    selectedTextColor = ElementTheme.colors.textActionAccent,
    unselectedIconColor = ElementTheme.colors.iconTertiary,
    unselectedTextColor = ElementTheme.colors.textDisabled,
    selectedIndicatorColor = Color.Transparent,
)
