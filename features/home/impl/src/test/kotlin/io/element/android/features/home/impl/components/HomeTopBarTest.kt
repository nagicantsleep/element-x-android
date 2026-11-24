/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.home.impl.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import io.element.android.features.home.impl.HomeNavigationBarItem
import io.element.android.features.home.impl.filters.aRoomListFiltersState
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.libraries.designsystem.theme.BrandLayout
import io.element.android.libraries.designsystem.theme.BrandTheme
import io.element.android.libraries.designsystem.theme.LocalBrandLayout
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.ui.components.aMatrixUser
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.EnsureNeverCalledWithParam
import io.element.android.tests.testutils.pressTag
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.Test

class HomeTopBarTest : RobolectricTest() {
    @Test
    fun `L header renders the account avatar and opens settings on click`() = runAndroidComposeUiTest<ComponentActivity> {
        setContent {
            CompositionLocalProvider(LocalBrandLayout provides BrandLayout.forTheme(BrandTheme.L, isLight = true)) {
                HomeTopBar(
                    selectedNavigationItem = HomeNavigationBarItem.Messages,
                    currentUserAndNeighbors = listOf(aMatrixUser()).toPersistentList(),
                    showAvatarIndicator = false,
                    areSearchResultsDisplayed = false,
                    onToggleSearch = {},
                    onMenuActionClick = {},
                    onOpenSettings = {},
                    onAccountSwitch = EnsureNeverCalledWithParam<SessionId>(),
                    onStartChatClick = {},
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
                    canReportBug = true,
                    displayFilters = false,
                    filtersState = aRoomListFiltersState(),
                    spaceFiltersState = SpaceFiltersState.Disabled,
                )
            }
        }
        // The L header shows the account avatar with the settings test tag: tapping it opens Settings.
        pressTag(TestTags.homeScreenSettings.value)
        onNodeWithText("Messages").assertExists()
        onNodeWithContentDescription(activity!!.getString(CommonStrings.action_search)).assertExists()
    }

    @Test
    fun `L header does not crash when the user list is still empty`() = runAndroidComposeUiTest<ComponentActivity> {
        setContent {
            CompositionLocalProvider(LocalBrandLayout provides BrandLayout.forTheme(BrandTheme.L, isLight = true)) {
                HomeTopBar(
                    selectedNavigationItem = HomeNavigationBarItem.Messages,
                    currentUserAndNeighbors = persistentListOf(),
                    showAvatarIndicator = false,
                    areSearchResultsDisplayed = false,
                    onToggleSearch = {},
                    onMenuActionClick = {},
                    onOpenSettings = {},
                    onAccountSwitch = EnsureNeverCalledWithParam<SessionId>(),
                    onStartChatClick = {},
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
                    canReportBug = true,
                    displayFilters = false,
                    filtersState = aRoomListFiltersState(),
                    spaceFiltersState = SpaceFiltersState.Disabled,
                )
            }
        }
        // Before the fix, the empty list took the pager branch whose snapshotFlow crashed with
        // IndexOutOfBoundsException (initialPage = 1 on a size-0 list).
        onNodeWithText("Messages").assertExists()
    }
}
