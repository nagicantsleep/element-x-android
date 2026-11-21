/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.friends.impl.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.friends.impl.R
import io.element.android.features.friends.impl.model.FriendEntry
import io.element.android.features.friends.impl.model.FriendRequestDirection
import io.element.android.features.friends.impl.model.FriendRequestEntry
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.FilledTextField
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.qrcode.QrCodeImage
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsView(
    state: FriendsState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                titleStr = stringResource(R.string.screen_friends_title),
                navigationIcon = {
                    BackButton(onClick = onBackClick)
                },
            )
        },
        snackbarHost = {
            SnackbarHost(rememberSnackbarHostState(null))
        },
        content = { padding ->
            FriendsContent(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        },
    )
}

@Composable
private fun FriendsContent(
    state: FriendsState,
    modifier: Modifier = Modifier,
) {
    var showMyQrCode by rememberSaveable { mutableStateOf(false) }
    var newFriendId by rememberSaveable { mutableStateOf("") }

    LazyColumn(modifier = modifier) {
        item(key = "my_qr") {
            ListItem(
                content = { Text(stringResource(R.string.screen_friends_my_qr_code)) },
                leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.QrCode())),
                trailingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.ChevronRight())),
                onClick = { showMyQrCode = !showMyQrCode },
            )
            if (showMyQrCode) {
                state.myMatrixToLink?.let { link ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        QrCodeImage(
                            data = link,
                            forceMaxBrightness = false,
                            modifier = Modifier
                                .size(220.dp)
                                .background(Color.White),
                        )
                    }
                }
            }
        }
        item(key = "add_friend") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                FilledTextField(
                    value = newFriendId,
                    onValueChange = { newFriendId = it },
                    placeholder = { Text(stringResource(R.string.screen_friends_add_hint)) },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    text = stringResource(R.string.screen_friends_add_friend),
                    enabled = newFriendId.isNotBlank() && state.addFriendAction !is AsyncAction.Loading,
                    onClick = {
                        state.eventSink(FriendsEvent.AddFriend(newFriendId.trim()))
                        newFriendId = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (state.incomingRequests.isNotEmpty()) {
            item(key = "incoming_title") {
                SectionTitle(stringResource(R.string.screen_friends_incoming_requests))
            }
            items(state.incomingRequests, key = { "in_${it.userId.value}" }) { request ->
                FriendRequestRow(
                    request = request,
                    leadingActionText = stringResource(CommonStrings.action_accept),
                    onLeadingAction = { state.eventSink(FriendsEvent.AcceptRequest(request.userId)) },
                    trailingActionText = stringResource(CommonStrings.action_reject),
                    onTrailingAction = { state.eventSink(FriendsEvent.RejectRequest(request.userId)) },
                )
            }
        }
        if (state.outgoingRequests.isNotEmpty()) {
            item(key = "outgoing_title") {
                SectionTitle(stringResource(R.string.screen_friends_outgoing_requests))
            }
            items(state.outgoingRequests, key = { "out_${it.userId.value}" }) { request ->
                FriendRequestRow(
                    request = request,
                    leadingActionText = stringResource(R.string.screen_friends_cancel_request),
                    onLeadingAction = { state.eventSink(FriendsEvent.CancelRequest(request.userId)) },
                    trailingActionText = null,
                    onTrailingAction = null,
                )
            }
        }
        if (state.friends.isNotEmpty()) {
            item(key = "friends_title") {
                SectionTitle(stringResource(R.string.screen_friends_title))
            }
            items(state.friends, key = { "friend_${it.userId.value}" }) { friend ->
                FriendRow(
                    friend = friend,
                    onRemoveClick = { state.eventSink(FriendsEvent.RemoveFriend(friend.userId)) },
                )
            }
        }
        if (state.displayEmptyState) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.screen_friends_empty),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun FriendRequestRow(
    request: FriendRequestEntry,
    leadingActionText: String,
    onLeadingAction: () -> Unit,
    trailingActionText: String?,
    onTrailingAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    UserRow(
        userId = request.userId,
        displayName = request.displayName,
        trailingContent = {
            Button(
                text = leadingActionText,
                onClick = onLeadingAction,
                size = io.element.android.libraries.designsystem.theme.components.ButtonSize.Medium,
                modifier = modifier,
            )
            if (trailingActionText != null && onTrailingAction != null) {
                Button(
                    text = trailingActionText,
                    onClick = onTrailingAction,
                    destructive = true,
                    size = io.element.android.libraries.designsystem.theme.components.ButtonSize.Medium,
                    modifier = modifier,
                )
            }
        },
    )
}

@Composable
private fun FriendRow(
    friend: FriendEntry,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UserRow(
        userId = friend.userId,
        displayName = friend.displayName,
        trailingContent = {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = CompoundIcons.Delete(),
                    contentDescription = stringResource(R.string.screen_friends_remove_friend),
                )
            }
        },
    )
}

@Composable
private fun UserRow(
    userId: UserId,
    displayName: String?,
    trailingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarData = AvatarData(
                id = userId.value,
                name = displayName ?: userId.value,
                size = AvatarSize.UserListItem,
            ),
            avatarType = AvatarType.User,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(displayName ?: userId.value)
            Text(userId.value)
        }
        trailingContent()
    }
}

@PreviewsDayNight
@Composable
internal fun FriendsViewPreview(@PreviewParameter(FriendsStatePreview::class) state: FriendsState) {
    ElementPreview {
        FriendsView(
            state = state,
            onBackClick = {},
        )
    }
}

internal class FriendsStatePreview : PreviewParameterProvider<FriendsState> {
    override val values: Sequence<FriendsState>
        get() = sequenceOf(aFriendsState())
}

internal fun aFriendsState(
    friends: ImmutableList<FriendEntry> = persistentListOf(
        FriendEntry(UserId("@alice:example.org"), "Alice", 1_000L),
    ),
    incoming: ImmutableList<FriendRequestEntry> = persistentListOf(
        FriendRequestEntry(UserId("@bob:example.org"), "Bob", 2_000L, FriendRequestDirection.INCOMING),
    ),
    outgoing: ImmutableList<FriendRequestEntry> = persistentListOf(
        FriendRequestEntry(UserId("@carol:example.org"), null, 3_000L, FriendRequestDirection.OUTGOING),
    ),
) = FriendsState(
    friends = friends,
    incomingRequests = incoming,
    outgoingRequests = outgoing,
    myMatrixToLink = "https://matrix.to/#/%40me%3Aexample.org",
    isLoading = false,
    addFriendAction = AsyncAction.Uninitialized,
    eventSink = {},
)
