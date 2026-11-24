/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.element.android.appconfig.RoomListConfig
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.HomeNavigationBarItem
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.filters.RoomListFiltersState
import io.element.android.features.home.impl.filters.RoomListFiltersView
import io.element.android.features.home.impl.filters.aRoomListFiltersState
import io.element.android.features.home.impl.spacefilters.SpaceFiltersEvent
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.home.impl.spacefilters.aSelectedSpaceFiltersState
import io.element.android.features.home.impl.spacefilters.anUnselectedSpaceFiltersState
import io.element.android.libraries.designsystem.atomic.atoms.RedIndicatorAtom
import io.element.android.libraries.designsystem.components.TopAppBarScrollBehaviorLayout
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.modifiers.backgroundVerticalGradient
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.preview.USER_NAME_ALICE
import io.element.android.libraries.designsystem.text.AdaptativeTitle
import io.element.android.libraries.designsystem.theme.LocalBrandLayout
import io.element.android.libraries.designsystem.theme.RoomListHeaderLayout
import io.element.android.libraries.designsystem.theme.components.DropdownMenu
import io.element.android.libraries.designsystem.theme.components.DropdownMenuItem
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.user.DisplayedStatus
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.api.user.UserStatus
import io.element.android.libraries.matrix.ui.components.aMatrixUser
import io.element.android.libraries.matrix.ui.components.aMatrixUserList
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.matrix.ui.model.toEmojiText
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    selectedNavigationItem: HomeNavigationBarItem,
    currentUserAndNeighbors: ImmutableList<MatrixUser>,
    showAvatarIndicator: Boolean,
    areSearchResultsDisplayed: Boolean,
    onToggleSearch: () -> Unit,
    onMenuActionClick: (RoomListMenuAction) -> Unit,
    onOpenSettings: () -> Unit,
    onAccountSwitch: (SessionId) -> Unit,
    onStartChatClick: () -> Unit,
    onFriendsClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
    canReportBug: Boolean,
    displayFilters: Boolean,
    filtersState: RoomListFiltersState,
    spaceFiltersState: SpaceFiltersState,
    modifier: Modifier = Modifier,
) {
    val contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
    val brandLayout = LocalBrandLayout.current
    when (brandLayout.roomListHeader) {
        RoomListHeaderLayout.ZAccentBar -> {
            ZAccentRoomListHeader(
                selectedNavigationItem = selectedNavigationItem,
                currentUserAndNeighbors = currentUserAndNeighbors,
                spaceFiltersState = spaceFiltersState,
                onStartChatClick = onStartChatClick,
                onOpenSettings = onOpenSettings,
                onToggleSearch = onToggleSearch,
            )
            return
        }
        RoomListHeaderLayout.LPlain -> {
            LPlainRoomListHeader(
                selectedNavigationItem = selectedNavigationItem,
                currentUserAndNeighbors = currentUserAndNeighbors,
                showAvatarIndicator = showAvatarIndicator,
                onAccountSwitch = onAccountSwitch,
                onStartChatClick = onStartChatClick,
                onOpenSettings = onOpenSettings,
                onToggleSearch = onToggleSearch,
                contentPadding = contentPadding,
            )
            return
        }
        RoomListHeaderLayout.Stock -> Unit
    }
    Column(modifier) {
        TopAppBar(
            modifier = Modifier
                .backgroundVerticalGradient(
                    isVisible = !areSearchResultsDisplayed,
                )
                .statusBarsPadding()
                .padding(contentPadding),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            title = {
                val displayTitle = when (selectedNavigationItem) {
                    HomeNavigationBarItem.Messages -> {
                        when (spaceFiltersState) {
                            is SpaceFiltersState.Selected -> spaceFiltersState.selectedFilter.spaceRoom.displayName
                            else -> stringResource(selectedNavigationItem.labelRes)
                        }
                    }
                    // Z/L layout: every tab displays its label; upstream only titles the chats list.
                    HomeNavigationBarItem.Contacts,
                    HomeNavigationBarItem.Discover,
                    HomeNavigationBarItem.Profile,
                    -> stringResource(selectedNavigationItem.labelRes)
                }
                displayTitle?.let {
                    val style = when (spaceFiltersState) {
                        // Space name
                        is SpaceFiltersState.Selected -> ElementTheme.typography.fontHeadingSmMedium
                        // "Messages"
                        else -> ElementTheme.typography.fontHeadingLgBold
                    }
                    AdaptativeTitle(
                        title = displayTitle,
                        style = style,
                        twoLinesStyle = ElementTheme.typography.fontHeadingSmMedium,
                    )
                }
            },
            navigationIcon = {
                NavigationIcon(
                    currentUserAndNeighbors = currentUserAndNeighbors,
                    showAvatarIndicator = showAvatarIndicator,
                    onAccountSwitch = onAccountSwitch,
                    onClick = onOpenSettings,
                )
            },
            actions = {
                if (selectedNavigationItem == HomeNavigationBarItem.Messages) {
                    // Z/L layout: the compose action lives here now that the FAB has been removed.
                    IconButton(onClick = onStartChatClick) {
                        Icon(
                            imageVector = CompoundIcons.Compose(),
                            contentDescription = stringResource(CommonStrings.action_create_room),
                        )
                    }
                    RoomListMenuItems(
                        onToggleSearch = onToggleSearch,
                        onMenuActionClick = onMenuActionClick,
                        canReportBug = canReportBug,
                        spaceFiltersState = spaceFiltersState,
                    )
                } else if (selectedNavigationItem == HomeNavigationBarItem.Contacts) {
                    // Z/L layout: the Friends surface is reachable from the Contacts tab.
                    IconButton(onClick = onFriendsClick) {
                        Icon(
                            imageVector = CompoundIcons.UserAdd(),
                            contentDescription = stringResource(CommonStrings.screen_friends_title),
                        )
                    }
                }
            },
            // We want a 16dp left padding for the navigationIcon :
            // 4dp from default TopAppBarHorizontalPadding
            // 8dp from AccountIcon default padding (48dp touch target around 32dp avatar)
            // 4dp extra padding using left insets
            windowInsets = WindowInsets(left = 4.dp),
        )
        if (displayFilters) {
            TopAppBarScrollBehaviorLayout(scrollBehavior = scrollBehavior) {
                RoomListFiltersView(
                    state = filtersState,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(contentPadding)
                )
            }
        }
    }
}

/**
 * Z theme room list header (light mode): a solid accent bar under the status bar with the
 * search field inside it and white QR / compose actions; no large title (ADR 0016).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZAccentRoomListHeader(
    selectedNavigationItem: HomeNavigationBarItem,
    currentUserAndNeighbors: ImmutableList<MatrixUser>,
    spaceFiltersState: SpaceFiltersState,
    onStartChatClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgAccentRest)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Z header: the account avatar opens Settings, as in the stock bar.
            val headerUser = currentUserAndNeighbors.firstOrNull()
            if (headerUser != null) {
                NavigationIcon(
                    currentUserAndNeighbors = persistentListOf(headerUser),
                    showAvatarIndicator = false,
                    onAccountSwitch = {},
                    onClick = onOpenSettings,
                )
            } else {
                Spacer(Modifier.width(4.dp))
            }
            Spacer(Modifier.width(8.dp))
            val displayTitle = when (spaceFiltersState) {
                is SpaceFiltersState.Selected -> spaceFiltersState.selectedFilter.spaceRoom.displayName
                else -> stringResource(selectedNavigationItem.labelRes)
            }
            Text(
                text = displayTitle,
                style = ElementTheme.typography.fontHeadingLgBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = CompoundIcons.Search(),
                    contentDescription = stringResource(CommonStrings.action_search),
                    tint = Color.White,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = CompoundIcons.QrCode(),
                    contentDescription = stringResource(CommonStrings.screen_friends_my_qr_code),
                    tint = Color.White,
                )
            }
            IconButton(onClick = onStartChatClick) {
                Icon(
                    imageVector = CompoundIcons.Plus(),
                    contentDescription = stringResource(CommonStrings.action_start_chat),
                    tint = Color.White,
                )
            }
        }
    }
}

/**
 * L theme room list header: plain canvas, bold title on the left, plain (not circular)
 * action icons on the right and a grey search pill below (ADR 0016).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LPlainRoomListHeader(
    selectedNavigationItem: HomeNavigationBarItem,
    currentUserAndNeighbors: ImmutableList<MatrixUser>,
    showAvatarIndicator: Boolean,
    onAccountSwitch: (SessionId) -> Unit,
    onStartChatClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSearch: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationIcon(
                currentUserAndNeighbors = currentUserAndNeighbors,
                showAvatarIndicator = showAvatarIndicator,
                onAccountSwitch = onAccountSwitch,
                onClick = onOpenSettings,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(selectedNavigationItem.labelRes),
                style = ElementTheme.typography.fontHeadingLgBold,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = CompoundIcons.Search(),
                    contentDescription = stringResource(CommonStrings.action_search),
                )
            }
            IconButton(onClick = onStartChatClick) {
                Icon(
                    imageVector = CompoundIcons.Plus(),
                    contentDescription = stringResource(CommonStrings.action_start_chat),
                )
            }
        }
        SearchPill(
            onClick = onToggleSearch,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun SearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ElementTheme.colors.bgSubtlePrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = CompoundIcons.Search(),
            contentDescription = null,
            tint = ElementTheme.colors.iconSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(CommonStrings.action_search),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun RowScope.RoomListMenuItems(
    onToggleSearch: () -> Unit,
    onMenuActionClick: (RoomListMenuAction) -> Unit,
    canReportBug: Boolean,
    spaceFiltersState: SpaceFiltersState,
) {
    IconButton(
        onClick = onToggleSearch,
    ) {
        Icon(
            imageVector = CompoundIcons.Search(),
            contentDescription = stringResource(CommonStrings.action_search),
        )
    }
    SpaceFilterButton(spaceFiltersState = spaceFiltersState)
    if (RoomListConfig.HAS_DROP_DOWN_MENU) {
        var showMenu by remember { mutableStateOf(false) }
        IconButton(
            onClick = { showMenu = !showMenu }
        ) {
            Icon(
                imageVector = CompoundIcons.OverflowVertical(),
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            if (RoomListConfig.SHOW_INVITE_MENU_ITEM) {
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        onMenuActionClick(RoomListMenuAction.InviteFriends)
                    },
                    text = { Text(stringResource(id = CommonStrings.action_invite)) },
                    leadingIcon = {
                        Icon(
                            imageVector = CompoundIcons.ShareAndroid(),
                            tint = ElementTheme.colors.iconSecondary,
                            contentDescription = null,
                        )
                    }
                )
            }
            if (RoomListConfig.SHOW_REPORT_PROBLEM_MENU_ITEM && canReportBug) {
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        onMenuActionClick(RoomListMenuAction.ReportBug)
                    },
                    text = { Text(stringResource(id = CommonStrings.common_report_a_problem)) },
                    leadingIcon = {
                        Icon(
                            imageVector = CompoundIcons.ChatProblem(),
                            tint = ElementTheme.colors.iconSecondary,
                            contentDescription = null,
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SpaceFilterButton(
    spaceFiltersState: SpaceFiltersState,
) {
    if (spaceFiltersState == SpaceFiltersState.Disabled) return

    fun onClick() {
        when (spaceFiltersState) {
            is SpaceFiltersState.Unselected -> spaceFiltersState.eventSink(SpaceFiltersEvent.Unselected.ShowFilters)
            is SpaceFiltersState.Selected -> spaceFiltersState.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
            else -> Unit
        }
    }

    val isSelected = spaceFiltersState is SpaceFiltersState.Selected
    IconButton(
        onClick = ::onClick,
        colors = if (isSelected) {
            IconButtonDefaults.iconButtonColors(
                containerColor = ElementTheme.colors.bgActionPrimaryRest,
                contentColor = ElementTheme.colors.iconOnSolidPrimary,
            )
        } else {
            IconButtonDefaults.iconButtonColors()
        },
    ) {
        Icon(
            imageVector = CompoundIcons.Filter(),
            contentDescription = stringResource(R.string.screen_roomlist_your_spaces),
        )
    }
}

@Composable
private fun NavigationIcon(
    currentUserAndNeighbors: ImmutableList<MatrixUser>,
    showAvatarIndicator: Boolean,
    onAccountSwitch: (SessionId) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (currentUserAndNeighbors.size == 1) {
        AccountIcon(
            matrixUser = currentUserAndNeighbors.single(),
            isCurrentAccount = true,
            showAvatarIndicator = showAvatarIndicator,
            onClick = onClick,
            modifier = modifier,
        )
    } else {
        // Render a vertical pager
        val pagerState = rememberPagerState(initialPage = 1) { currentUserAndNeighbors.size }
        // Listen to page changes and switch account if needed
        val latestOnAccountSwitch by rememberUpdatedState(onAccountSwitch)
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                currentUserAndNeighbors.getOrNull(page)?.let { user ->
                    latestOnAccountSwitch(SessionId(user.userId.value))
                }
            }
        }
        VerticalPager(
            state = pagerState,
            modifier = modifier.height(48.dp),
        ) { page ->
            AccountIcon(
                matrixUser = currentUserAndNeighbors[page],
                isCurrentAccount = page == 1,
                showAvatarIndicator = page == 1 && showAvatarIndicator,
                onClick = if (page == 1) {
                    onClick
                } else {
                    {}
                },
            )
        }
    }
}

@Composable
private fun AccountIcon(
    matrixUser: MatrixUser,
    isCurrentAccount: Boolean,
    showAvatarIndicator: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val testTag = if (isCurrentAccount) Modifier.testTag(TestTags.homeScreenSettings) else Modifier
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .then(testTag)
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = interactionSource,
                onClick = onClick,
                indication = ripple(bounded = false),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val avatarData by remember(matrixUser) {
            derivedStateOf {
                matrixUser.getAvatarData(size = AvatarSize.CurrentUserTopBar)
            }
        }
        val statusEmoji = matrixUser.displayedStatus?.toEmojiText()
        val avatarModifier = if (statusEmoji != null) {
            Modifier.eraseStatusEmojiBackground(
                parentSize = AvatarSize.CurrentUserTopBar.dp,
                layoutDirection = LocalLayoutDirection.current,
            )
        } else {
            Modifier
        }
        Avatar(
            avatarData = avatarData,
            avatarType = AvatarType.User,
            modifier = avatarModifier,
            contentDescription = if (isCurrentAccount) {
                if (showAvatarIndicator) {
                    stringResource(CommonStrings.a11y_settings_with_required_action)
                } else {
                    stringResource(CommonStrings.common_settings)
                }
            } else {
                null
            },
        )
        if (statusEmoji != null) {
            StatusEmojiBadge(
                emoji = statusEmoji,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
        if (showAvatarIndicator) {
            RedIndicatorAtom(
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

private val statusEmojiBadgeSize = 20.dp
private val statusEmojiBadgeRadius = statusEmojiBadgeSize / 2
private val statusEmojiBadgeOffset = 8.dp

private fun Modifier.eraseStatusEmojiBackground(
    parentSize: Dp,
    layoutDirection: LayoutDirection,
): Modifier = this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        drawCircle(
            color = Color.Black,
            center = Offset(
                x = if (layoutDirection == LayoutDirection.Ltr) {
                    (parentSize - statusEmojiBadgeRadius + statusEmojiBadgeOffset).toPx()
                } else {
                    (statusEmojiBadgeRadius - statusEmojiBadgeOffset).toPx()
                },
                y = size.height - statusEmojiBadgeRadius.toPx(),
            ),
            radius = statusEmojiBadgeRadius.toPx(),
            blendMode = BlendMode.Clear,
        )
    }

@Composable
private fun StatusEmojiBadge(
    emoji: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // Ensure the emoji size is not affected by the user's font scale settings
    val fixedFontScaleDensity = remember(density.density) {
        Density(density = density.density, fontScale = 1f)
    }
    Box(
        modifier = modifier
            .offset(x = statusEmojiBadgeOffset)
            .size(statusEmojiBadgeSize),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalDensity provides fixedFontScaleDensity) {
            Text(
                text = emoji,
                style = ElementTheme.typography.fontBodyMdRegular,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Messages,
        currentUserAndNeighbors = persistentListOf(aMatrixUser(id = "@id:domain", displayName = USER_NAME_ALICE)),
        showAvatarIndicator = false,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onStartChatClick = {},
        onToggleSearch = {},
        canReportBug = true,
        displayFilters = true,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = anUnselectedSpaceFiltersState(),
        onMenuActionClick = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarSpaceFiltersSelectedPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Messages,
        currentUserAndNeighbors = persistentListOf(aMatrixUser(id = "@id:domain", displayName = USER_NAME_ALICE)),
        showAvatarIndicator = false,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onStartChatClick = {},
        onToggleSearch = {},
        canReportBug = true,
        displayFilters = true,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = aSelectedSpaceFiltersState(),
        onMenuActionClick = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarSpacesPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Discover,
        currentUserAndNeighbors = persistentListOf(aMatrixUser(id = "@id:domain", displayName = USER_NAME_ALICE)),
        showAvatarIndicator = false,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onStartChatClick = {},
        onToggleSearch = {},
        canReportBug = true,
        displayFilters = false,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = anUnselectedSpaceFiltersState(),
        onMenuActionClick = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarWithIndicatorPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Messages,
        currentUserAndNeighbors = persistentListOf(aMatrixUser(id = "@id:domain", displayName = USER_NAME_ALICE)),
        showAvatarIndicator = true,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onStartChatClick = {},
        onToggleSearch = {},
        canReportBug = true,
        displayFilters = true,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = anUnselectedSpaceFiltersState(),
        onMenuActionClick = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarWithStatusPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Messages,
        currentUserAndNeighbors = persistentListOf(
            aMatrixUser(
                id = "@id:domain",
                displayName = USER_NAME_ALICE,
                displayedStatus = DisplayedStatus.UserSet(UserStatus(emoji = "🌴", text = "Away")),
            )
        ),
        showAvatarIndicator = false,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onStartChatClick = {},
        onToggleSearch = {},
        canReportBug = true,
        displayFilters = true,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = anUnselectedSpaceFiltersState(),
        onMenuActionClick = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewsDayNight
@Composable
internal fun HomeTopBarMultiAccountPreview() = ElementPreview {
    HomeTopBar(
        selectedNavigationItem = HomeNavigationBarItem.Messages,
        currentUserAndNeighbors = aMatrixUserList().take(3).toImmutableList(),
        showAvatarIndicator = false,
        areSearchResultsDisplayed = false,
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
        onOpenSettings = {},
        onAccountSwitch = {},
        onStartChatClick = {},
        onToggleSearch = {},
        canReportBug = true,
        displayFilters = true,
        filtersState = aRoomListFiltersState(),
        spaceFiltersState = anUnselectedSpaceFiltersState(),
        onMenuActionClick = {},
    )
}
