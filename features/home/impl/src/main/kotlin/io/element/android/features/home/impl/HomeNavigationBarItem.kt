/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.element.android.compound.tokens.generated.CompoundIcons

/**
 * The four destinations of the Z/L bottom navigation bar.
 *
 * The Z/L layout follows the Zalo/LINE convention: a full-width bar with an icon on top and a
 * label underneath, a filled ("solid") icon for the selected tab, and a badge for new activity.
 */
enum class HomeNavigationBarItem(
    @StringRes
    val labelRes: Int,
) {
    Messages(
        labelRes = R.string.screen_home_tab_messages
    ),
    Contacts(
        labelRes = R.string.screen_home_tab_contacts
    ),
    Discover(
        labelRes = R.string.screen_home_tab_discover
    ),
    Profile(
        labelRes = R.string.screen_home_tab_profile
    );

    /**
     * The icon for this destination. [isSelected] swaps in the filled variant where Compound has
     * one, so the selected tab reads as "active" the way Zalo/LINE do.
     */
    @Composable
    fun icon(
        isSelected: Boolean,
    ): ImageVector = when (this) {
        Messages -> if (isSelected) CompoundIcons.ChatSolid() else CompoundIcons.Chat()
        Contacts -> if (isSelected) CompoundIcons.UserSolid() else CompoundIcons.User()
        // Compound has no `ExploreSolid`, so the compass icon is used for both states.
        Discover -> CompoundIcons.Explore()
        Profile -> if (isSelected) CompoundIcons.UserProfileSolid() else CompoundIcons.UserProfile()
    }

    companion object {
        fun from(index: Int): HomeNavigationBarItem {
            return entries.getOrElse(index) { Messages }
        }
    }
}
