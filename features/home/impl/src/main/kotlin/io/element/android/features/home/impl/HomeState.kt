/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl

import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import io.element.android.features.home.impl.roomlist.RoomListContentState
import io.element.android.features.home.impl.roomlist.RoomListState
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.home.impl.spaces.HomeSpacesState
import io.element.android.features.logout.api.direct.DirectLogoutState
import io.element.android.features.startchat.impl.userlist.UserListState
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.ImmutableList

data class HomeState(
    /**
     * The current user of this session, in case of multiple accounts, will contains 3 items, with the
     * current user in the middle.
     */
    val currentUserAndNeighbors: ImmutableList<MatrixUser>,
    val showAvatarIndicator: Boolean,
    val hasNetworkConnection: Boolean,
    val currentHomeNavigationBarItem: HomeNavigationBarItem,
    val roomListState: RoomListState,
    val homeSpacesState: HomeSpacesState,
    /** State of the user list shown in the Contacts tab. */
    val contactsUserListState: UserListState,
    /** Ongoing "start a DM" action triggered from the Contacts tab, if any. */
    val contactsStartDmAction: AsyncAction<RoomId>,
    val snackbarMessage: SnackbarMessage?,
    val canReportBug: Boolean,
    val directLogoutState: DirectLogoutState,
    val eventSink: (HomeEvent) -> Unit,
) {
    val isBackHandlerEnabled = currentHomeNavigationBarItem != HomeNavigationBarItem.Messages || roomListState.spaceFiltersState is SpaceFiltersState.Selected

    // Z/L layout: the row of filter chips (Unreads / People / Rooms / Favorites / Invites) is hidden
    // to keep the Messages tab uncluttered, matching the reference apps. Kept as a property so it is
    // a one-line revert.
    val displayRoomListFilters = false

    /** The current user, i.e. the middle entry of [currentUserAndNeighbors]. */
    val currentUser: MatrixUser
        get() = currentUserAndNeighbors.getOrNull(currentUserAndNeighbors.size / 2) ?: currentUserAndNeighbors.first()

    /**
     * Z/L layout: number of rooms with new content, shown as a badge on the Messages tab, mirroring
     * the unread counter the reference apps put on their chat tab.
     */
    val messagesBadgeCount: Int
        get() = roomListSummaries.count { it.displayType == RoomSummaryDisplayType.ROOM && it.hasNewContent }

    /**
     * Z/L layout: number of pending invites, shown as a badge on the Contacts tab, mirroring the
     * "new contact request" indicator the reference apps use.
     */
    val contactsBadgeCount: Int
        get() = roomListSummaries.count { it.displayType == RoomSummaryDisplayType.INVITE }

    private val roomListSummaries: List<RoomListRoomSummary>
        get() = (roomListState.contentState as? RoomListContentState.Rooms)?.summaries ?: emptyList()
}
