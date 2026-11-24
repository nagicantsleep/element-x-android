/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.friends.impl.model

import io.element.android.libraries.matrix.api.timeline.item.event.FRIEND_REQUEST_FORMAT
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.roomlist.LatestEventValue
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.timeline.item.event.FormattedBody
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageFormat
import io.element.android.libraries.matrix.api.timeline.item.event.OtherMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.room.aRoomSummary
import io.element.android.libraries.matrix.test.timeline.aMessageContent
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class FriendsMergeTest {
    @Test
    fun `friends - server entries win, local-only entries are kept`() {
        val server = listOf(
            FriendEntry(userId = A_USER_ID, displayName = null, addedAt = 1L),
        )
        val local = listOf(
            FriendEntry(userId = A_USER_ID, displayName = "server duplicate", addedAt = 100L),
            FriendEntry(userId = A_USER_ID_2, displayName = "web only", addedAt = 2L),
        )
        val result = FriendsMerge.friends(server, local)
        assertThat(result).hasSize(2)
        // The server entry is kept verbatim.
        assertThat(result.first { it.userId == A_USER_ID }.addedAt).isEqualTo(1L)
        // The local-only entry (a web-accepted edge) is merged in.
        assertThat(result.first { it.userId == A_USER_ID_2 }.displayName).isEqualTo("web only")
    }

    @Test
    fun `friends - empty server list keeps all local entries`() {
        val local = listOf(FriendEntry(A_USER_ID, null, 5L))
        assertThat(FriendsMerge.friends(emptyList(), local)).isEqualTo(local)
    }

    @Test
    fun `requests - server requests win, local-only requests are kept`() {
        val serverIncoming = listOf(FriendRequestEntry(A_USER_ID, null, 1L, FriendRequestDirection.INCOMING))
        val serverOutgoing = listOf(FriendRequestEntry(A_USER_ID_2, null, 2L, FriendRequestDirection.OUTGOING))
        val local = listOf(
            FriendRequestEntry(A_USER_ID, "duplicate", 100L, FriendRequestDirection.INCOMING),
            FriendRequestEntry(UserId("@carol:server.org"), "web only", 3L, FriendRequestDirection.OUTGOING),
        )
        val result = FriendsMerge.requests(serverIncoming, serverOutgoing, local)
        assertThat(result).hasSize(3)
        assertThat(result.filter { it.direction == FriendRequestDirection.INCOMING }).hasSize(1)
        assertThat(result.filter { it.direction == FriendRequestDirection.OUTGOING }).hasSize(2)
        assertThat(result.first { it.userId == A_USER_ID }.createdAt).isEqualTo(1L)
        assertThat(result.first { it.userId == UserId("@carol:server.org") }.displayName).isEqualTo("web only")
    }

    @Test
    fun `friendRequestFromSummary - maps a DM marker from another user`() {
        val summary = aRoomSummary(
            roomId = io.element.android.libraries.matrix.test.A_ROOM_ID,
            isDm = true,
            latestEvent = LatestEventValue.Remote(
                timestamp = 42L,
                content = aFriendRequestMessageContent(),
                senderId = A_USER_ID_2,
                senderProfile = io.element.android.libraries.matrix.test.timeline.aProfileDetails(),
                isOwn = false,
            ),
        )
        val request = friendRequestFromSummary(summary, ownUserId = A_USER_ID)
        assertThat(request).isNotNull()
        assertThat(request!!.userId).isEqualTo(A_USER_ID_2)
        assertThat(request.createdAt).isEqualTo(42L)
        assertThat(request.direction).isEqualTo(FriendRequestDirection.INCOMING)
    }

    @Test
    fun `friendRequestFromSummary - ignores own messages`() {
        val summary = aRoomSummary(
            isDm = true,
            latestEvent = LatestEventValue.Remote(
                timestamp = 42L,
                content = aFriendRequestMessageContent(),
                senderId = A_USER_ID,
                senderProfile = io.element.android.libraries.matrix.test.timeline.aProfileDetails(),
                isOwn = true,
            ),
        )
        assertThat(friendRequestFromSummary(summary, ownUserId = A_USER_ID)).isNull()
    }

    @Test
    fun `friendRequestFromSummary - ignores non-DM rooms and other msgtypes`() {
        val notDm = aRoomSummary(
            roomId = A_ROOM_ID,
            isDm = false,
            latestEvent = aRemoteEvent(senderId = A_USER_ID_2, content = aFriendRequestMessageContent()),
        )
        assertThat(friendRequestFromSummary(notDm, A_USER_ID)).isNull()

        val otherMsgType = aRoomSummary(
            isDm = true,
            latestEvent = aRemoteEvent(
                senderId = A_USER_ID_2,
                content = aMessageContent(messageType = TextMessageType("hello", null)),
            ),
        )
        assertThat(friendRequestFromSummary(otherMsgType, A_USER_ID)).isNull()
    }

    @Test
    fun `friendRequestFromSummary - ignores non-message events`() {
        val invite = aRoomSummary(
            isDm = true,
            latestEvent = LatestEventValue.RoomInvite(
                timestamp = 42L,
                inviterId = A_USER_ID_2,
                invitedProfile = io.element.android.libraries.matrix.test.timeline.aProfileDetails(),
            ),
        )
        assertThat(friendRequestFromSummary(invite, A_USER_ID)).isNull()
    }

    @Test
    fun `friendRequestFromSummary - maps a wire v2 text marker`() {
        val summary = aRoomSummary(
            roomId = io.element.android.libraries.matrix.test.A_ROOM_ID,
            isDm = true,
            latestEvent = aRemoteEvent(
                senderId = A_USER_ID_2,
                content = aMessageContent(
                    messageType = TextMessageType(
                        body = "👋 Friend request",
                        formatted = FormattedBody(
                            format = MessageFormat.UNKNOWN,
                            body = "👋 Friend request",
                            rawFormat = FRIEND_REQUEST_FORMAT,
                        ),
                    ),
                ),
            ),
        )
        val request = friendRequestFromSummary(summary, ownUserId = A_USER_ID)
        assertThat(request).isNotNull()
        assertThat(request!!.userId).isEqualTo(A_USER_ID_2)
        assertThat(request.createdAt).isEqualTo(42L)
        assertThat(request.direction).isEqualTo(FriendRequestDirection.INCOMING)
    }

    @Test
    fun `friendRequestFromSummary - ignores wire v2 text with an unrelated format`() {
        val summary = aRoomSummary(
            roomId = io.element.android.libraries.matrix.test.A_ROOM_ID,
            isDm = true,
            latestEvent = aRemoteEvent(
                senderId = A_USER_ID_2,
                content = aMessageContent(
                    messageType = TextMessageType(
                        body = "hello",
                        formatted = FormattedBody(
                            format = MessageFormat.UNKNOWN,
                            body = "hello",
                            rawFormat = "io.nagi.something_else",
                        ),
                    ),
                ),
            ),
        )
        assertThat(friendRequestFromSummary(summary, A_USER_ID)).isNull()
    }

    private fun aFriendRequestMessageContent(): MessageContent =
        aMessageContent(messageType = OtherMessageType(msgType = FRIEND_REQUEST_MSGTYPE, body = "👋 Friend request"))

    private fun aRemoteEvent(
        senderId: UserId,
        content: io.element.android.libraries.matrix.api.timeline.item.event.EventContent,
    ) = LatestEventValue.Remote(
        timestamp = 42L,
        content = content,
        senderId = senderId,
        senderProfile = io.element.android.libraries.matrix.test.timeline.aProfileDetails(),
        isOwn = false,
    )
}
