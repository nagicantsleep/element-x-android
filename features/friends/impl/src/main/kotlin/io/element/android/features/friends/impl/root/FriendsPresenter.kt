/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.friends.impl.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import dev.zacsweers.metro.Inject
import io.element.android.features.friends.impl.R
import io.element.android.features.friends.impl.data.FriendsService
import io.element.android.features.friends.impl.model.FriendRequestOutcome
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.MatrixPatterns
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.permalink.PermalinkBuilder
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Inject
class FriendsPresenter(
    private val friendsService: FriendsService,
    private val matrixClient: MatrixClient,
    private val permalinkBuilder: PermalinkBuilder,
    private val snackbarDispatcher: SnackbarDispatcher,
) : Presenter<FriendsState> {
    @Composable
    override fun present(): FriendsState {
        val serviceState by friendsService.state.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            friendsService.refresh()
        }
        LaunchedEffect(Unit) {
            friendsService.observeIncomingRequests()
        }

        fun postResultSnackbar(resId: Int) {
            snackbarDispatcher.post(SnackbarMessage(resId))
        }

        fun handleEvent(event: FriendsEvent) {
            when (event) {
                is FriendsEvent.AddFriend -> {
                    if (!MatrixPatterns.isUserId(event.userId)) {
                        postResultSnackbar(R.string.screen_friends_invalid_user_id)
                        return
                    }
                    coroutineScope.launch {
                        friendsService.addFriend(UserId(event.userId))
                    }
                }
                is FriendsEvent.AcceptRequest -> coroutineScope.launch {
                    friendsService.acceptFriend(event.userId)
                }
                is FriendsEvent.RejectRequest -> coroutineScope.launch {
                    friendsService.removeFriend(event.userId)
                }
                is FriendsEvent.CancelRequest -> coroutineScope.launch {
                    friendsService.removeFriend(event.userId)
                }
                is FriendsEvent.RemoveFriend -> coroutineScope.launch {
                    friendsService.removeFriend(event.userId)
                }
            }
        }

        LaunchedEffect(serviceState.addFriendAction) {
            when (val action = serviceState.addFriendAction) {
                is AsyncAction.Success -> {
                    when (action.data) {
                        FriendRequestOutcome.REQUESTED -> postResultSnackbar(R.string.screen_friends_result_requested)
                        FriendRequestOutcome.ALREADY_FRIENDS -> postResultSnackbar(R.string.screen_friends_result_already_friends)
                        FriendRequestOutcome.ACCEPTED -> postResultSnackbar(R.string.screen_friends_result_accepted)
                    }
                }
                is AsyncAction.Failure -> postResultSnackbar(R.string.screen_friends_request_failed)
                else -> Unit
            }
        }

        return FriendsState(
            friends = serviceState.friends.toImmutableList(),
            incomingRequests = serviceState.incoming.toImmutableList(),
            outgoingRequests = serviceState.outgoing.toImmutableList(),
            myMatrixToLink = permalinkBuilder.permalinkForUser(matrixClient.sessionId).getOrNull(),
            isLoading = serviceState.loading,
            addFriendAction = serviceState.addFriendAction,
            eventSink = ::handleEvent,
        )
    }
}
