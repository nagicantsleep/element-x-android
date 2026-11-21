/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.friends.impl.root

import androidx.compose.runtime.Immutable
import io.element.android.features.friends.impl.model.FriendEntry
import io.element.android.features.friends.impl.model.FriendRequestEntry
import io.element.android.features.friends.impl.model.FriendRequestOutcome
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class FriendsState(
    val friends: ImmutableList<FriendEntry>,
    val incomingRequests: ImmutableList<FriendRequestEntry>,
    val outgoingRequests: ImmutableList<FriendRequestEntry>,
    /** `matrix.to` link to our own user id, encoded in the QR code; null when it cannot be built. */
    val myMatrixToLink: String?,
    val isLoading: Boolean,
    val addFriendAction: AsyncAction<FriendRequestOutcome>,
    val eventSink: (FriendsEvent) -> Unit,
) {
    val displayEmptyState = friends.isEmpty() && incomingRequests.isEmpty() && outgoingRequests.isEmpty() && !isLoading
}

sealed interface FriendsEvent {
    data class AddFriend(val userId: String) : FriendsEvent
    data class AcceptRequest(val userId: UserId) : FriendsEvent
    data class RejectRequest(val userId: UserId) : FriendsEvent
    data class CancelRequest(val userId: UserId) : FriendsEvent
    data class RemoveFriend(val userId: UserId) : FriendsEvent
}
