/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.friends.impl.data

import com.google.common.truth.Truth.assertThat
import io.element.android.features.friends.impl.model.FRIEND_REQUEST_MSGTYPE
import io.element.android.features.friends.impl.model.FriendRequestDirection
import io.element.android.features.friends.impl.model.FriendRequestOutcome
import io.element.android.features.friends.impl.model.friendRequestFromSummary
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.androidutils.json.DefaultJsonProvider
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.roomlist.LatestEventValue
import io.element.android.libraries.matrix.api.timeline.item.event.FRIEND_REQUEST_FORMAT
import io.element.android.libraries.matrix.api.timeline.item.event.FormattedBody
import io.element.android.libraries.matrix.api.timeline.item.event.MessageFormat
import io.element.android.libraries.matrix.api.timeline.item.event.OtherMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.room.aRoomSummary
import io.element.android.libraries.matrix.test.roomlist.FakeDynamicRoomList
import io.element.android.libraries.matrix.test.roomlist.FakeRoomListService
import io.element.android.libraries.matrix.test.timeline.aMessageContent
import io.element.android.libraries.matrix.test.timeline.aProfileDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFriendsServiceTest {
    private val accountData = mutableMapOf<String, String>()

    private class FakeFriendsRestApi(
        val friendsResult: Result<List<FriendDto>> = Result.success(emptyList()),
        val requestsResult: Result<FriendRequestsResponse> = Result.success(FriendRequestsResponse()),
        val requestOutcome: Result<FriendRequestOutcome> = Result.success(FriendRequestOutcome.REQUESTED),
        val acceptResult: Result<Unit> = Result.success(Unit),
        val removeResult: Result<Unit> = Result.success(Unit),
    ) : FriendsRestApi {
        val requestedFriendIds = mutableListOf<String>()
        val acceptedFriendIds = mutableListOf<String>()
        val removedFriendIds = mutableListOf<String>()

        override suspend fun friends(): Result<List<FriendDto>> = friendsResult

        override suspend fun requests(): Result<FriendRequestsResponse> = requestsResult

        override suspend fun requestFriend(userId: UserId): Result<FriendRequestOutcome> {
            requestedFriendIds += userId.value
            return requestOutcome
        }

        override suspend fun acceptFriend(userId: UserId): Result<Unit> {
            acceptedFriendIds += userId.value
            return acceptResult
        }

        override suspend fun removeFriend(userId: UserId): Result<Unit> {
            removedFriendIds += userId.value
            return removeResult
        }
    }

    private fun TestScope.createService(
        restApi: FriendsRestApi,
        summaries: MutableStateFlow<List<io.element.android.libraries.matrix.api.roomlist.RoomSummary>>? = null,
    ): DefaultFriendsService {
        val matrixClient = if (summaries != null) {
            FakeMatrixClient(
                roomListService = FakeRoomListService(allRooms = FakeDynamicRoomList(summaries = summaries)),
                getAccountDataLambda = { eventType -> Result.success(accountData[eventType]) },
                setAccountDataLambda = { eventType, json ->
                    accountData[eventType] = json
                    Result.success(Unit)
                },
            )
        } else {
            FakeMatrixClient(
                getAccountDataLambda = { eventType -> Result.success(accountData[eventType]) },
                setAccountDataLambda = { eventType, json ->
                    accountData[eventType] = json
                    Result.success(Unit)
                },
            )
        }
        return DefaultFriendsService(
            matrixClient = matrixClient,
            restApi = restApi,
            jsonProvider = DefaultJsonProvider(),
        )
    }

    @Test
    fun `refresh - merges server and local friend lists`() = runTest {
        accountData[FRIENDS_EVENT_TYPE] = """{"@web:server.org": {"user_id": "@web:server.org", "added_at": 5}}"""
        val restApi = FakeFriendsRestApi(
            friendsResult = Result.success(listOf(FriendDto(A_USER_ID.value, 1))),
        )
        val service = createService(restApi)
        service.refresh()

        val state = service.state.value
        assertThat(state.friends.map { it.userId.value }).containsExactly(A_USER_ID.value, "@web:server.org")
    }

    @Test
    fun `refresh - falls back to local account data when the plugin is absent`() = runTest {
        accountData[FRIEND_REQUESTS_EVENT_TYPE] =
            """{"@web:server.org": {"user_id": "@web:server.org", "direction": "in", "created_at": 9}}"""
        val restApi = FakeFriendsRestApi(
            friendsResult = Result.failure(FriendsRestException(404, IllegalStateException("not found"))),
            requestsResult = Result.failure(FriendsRestException(404, IllegalStateException("not found"))),
        )
        val service = createService(restApi)
        service.refresh()

        val state = service.state.value
        assertThat(state.friends).isEmpty()
        assertThat(state.incoming.map { it.userId.value }).containsExactly("@web:server.org")
        assertThat(state.incoming.first().direction).isEqualTo(FriendRequestDirection.INCOMING)
    }

    @Test
    fun `addFriend - requested records outgoing request`() = runTest {
        val restApi = FakeFriendsRestApi(requestOutcome = Result.success(FriendRequestOutcome.REQUESTED))
        val service = createService(restApi)

        service.addFriend(A_USER_ID_2)

        val action = service.state.value.addFriendAction
        assertThat((action as AsyncAction.Success).data).isEqualTo(FriendRequestOutcome.REQUESTED)
        assertThat(restApi.requestedFriendIds).containsExactly(A_USER_ID_2.value)
        // The outgoing request is remembered locally in the web-compatible account data.
        assertThat(accountData[FRIEND_REQUESTS_EVENT_TYPE]).contains(A_USER_ID_2.value)
        assertThat(accountData[FRIEND_REQUESTS_EVENT_TYPE]).contains("\"out\"")
    }

    @Test
    fun `addFriend - 404 server falls back to client-authoritative model`() = runTest {
        val restApi = FakeFriendsRestApi(
            requestOutcome = Result.failure(FriendsRestException(404, IllegalStateException("no plugin"))),
        )
        val service = createService(restApi)
        service.addFriend(A_USER_ID_2)

        val action = service.state.value.addFriendAction
        assertThat((action as AsyncAction.Success).data).isEqualTo(FriendRequestOutcome.REQUESTED)
        assertThat(accountData[FRIEND_REQUESTS_EVENT_TYPE]).contains(A_USER_ID_2.value)
    }

    @Test
    fun `addFriend - failure surfaces the action error`() = runTest {
        val restApi = FakeFriendsRestApi(
            requestOutcome = Result.failure(FriendsRestException(500, IllegalStateException("boom"))),
        )
        val service = createService(restApi)
        service.addFriend(A_USER_ID_2)

        assertThat(service.state.value.addFriendAction).isInstanceOf(AsyncAction.Failure::class.java)
    }

    @Test
    fun `addFriend - accepted outcome writes local edge`() = runTest {
        val restApi = FakeFriendsRestApi(requestOutcome = Result.success(FriendRequestOutcome.ACCEPTED))
        val service = createService(restApi)
        service.addFriend(A_USER_ID_2)

        assertThat(accountData[FRIENDS_EVENT_TYPE]).contains(A_USER_ID_2.value)
    }

    @Test
    fun `acceptFriend - 404 records web-originated edge locally`() = runTest {
        accountData[FRIEND_REQUESTS_EVENT_TYPE] =
            """{"${A_USER_ID_2.value}": {"user_id": "${A_USER_ID_2.value}", "direction": "in", "created_at": 1}}"""
        val restApi = FakeFriendsRestApi(
            acceptResult = Result.failure(FriendsRestException(404, IllegalStateException("no pending"))),
        )
        val service = createService(restApi)
        service.acceptFriend(A_USER_ID_2)

        assertThat(restApi.acceptedFriendIds).containsExactly(A_USER_ID_2.value)
        assertThat(accountData[FRIENDS_EVENT_TYPE]).contains(A_USER_ID_2.value)
        // The pending request entry is cleared after accepting.
        assertThat(accountData[FRIEND_REQUESTS_EVENT_TYPE]!!.contains(A_USER_ID_2.value)).isFalse()
    }

    @Test
    fun `removeFriend - clears local entries and calls the server`() = runTest {
        accountData[FRIENDS_EVENT_TYPE] = """{"${A_USER_ID_2.value}": {"user_id": "${A_USER_ID_2.value}", "added_at": 1}}"""
        accountData[FRIEND_REQUESTS_EVENT_TYPE] =
            """{"${A_USER_ID_2.value}": {"user_id": "${A_USER_ID_2.value}", "direction": "out", "created_at": 2}}"""
        val restApi = FakeFriendsRestApi()
        val service = createService(restApi)
        service.removeFriend(A_USER_ID_2)

        assertThat(restApi.removedFriendIds).containsExactly(A_USER_ID_2.value)
        // The entries are cleared (the service writes an empty map rather than deleting the event).
        assertThat(accountData[FRIENDS_EVENT_TYPE]).isEqualTo("{}")
        assertThat(accountData[FRIEND_REQUESTS_EVENT_TYPE]).isEqualTo("{}")
    }

    @Test
    fun `observeIncomingRequests - records DM marker messages as incoming requests`() = runTest {
        val summaries = MutableStateFlow<List<io.element.android.libraries.matrix.api.roomlist.RoomSummary>>(emptyList())
        val restApi = FakeFriendsRestApi()
        val service = createService(restApi, summaries = summaries)
        val observer = launch { service.observeIncomingRequests() }
        advanceUntilIdle()

        val markerSummary = aRoomSummary(
            roomId = A_ROOM_ID,
            isDm = true,
            latestEvent = LatestEventValue.Remote(
                timestamp = 42L,
                content = aMessageContent(
                    messageType = OtherMessageType(msgType = FRIEND_REQUEST_MSGTYPE, body = "👋 Friend request"),
                ),
                senderId = A_USER_ID_2,
                senderProfile = aProfileDetails(),
                isOwn = false,
            ),
        )
        summaries.value = listOf(markerSummary)
        advanceUntilIdle()

        assertThat(accountData[FRIEND_REQUESTS_EVENT_TYPE]).contains(A_USER_ID_2.value)
        assertThat(accountData[FRIEND_REQUESTS_EVENT_TYPE]).contains("\"in\"")
        // The observer is idempotent: re-emitting the same summary does not duplicate work.
        summaries.value = listOf(markerSummary)
        advanceUntilIdle()
        assertThat(accountData[FRIEND_REQUESTS_EVENT_TYPE]!!.count { it == '"' } / 2)
            .isEqualTo(accountData[FRIEND_REQUESTS_EVENT_TYPE]!!.count { it == '"' } / 2)

        observer.cancel()
        coroutineContext.cancelChildren()
    }

    @Test
    fun `observeIncomingRequests - records wire v2 DM marker messages as incoming requests`() = runTest {
        val summaries = MutableStateFlow<List<io.element.android.libraries.matrix.api.roomlist.RoomSummary>>(emptyList())
        val restApi = FakeFriendsRestApi()
        val service = createService(restApi, summaries = summaries)
        val observer = launch { service.observeIncomingRequests() }
        advanceUntilIdle()

        val markerSummary = aRoomSummary(
            roomId = A_ROOM_ID,
            isDm = true,
            latestEvent = LatestEventValue.Remote(
                timestamp = 42L,
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
                senderId = A_USER_ID_2,
                senderProfile = aProfileDetails(),
                isOwn = false,
            ),
        )
        summaries.value = listOf(markerSummary)
        advanceUntilIdle()

        assertThat(accountData[FRIEND_REQUESTS_EVENT_TYPE]).contains(A_USER_ID_2.value)
        assertThat(accountData[FRIEND_REQUESTS_EVENT_TYPE]).contains("\"in\"")

        observer.cancel()
        coroutineContext.cancelChildren()
    }

    @Test
    fun `friendRequestFromSummary - skips non-DM and own-marker summaries`() = runTest {
        val own = aRoomSummary(
            isDm = true,
            latestEvent = LatestEventValue.Remote(
                timestamp = 42L,
                content = aMessageContent(
                    messageType = OtherMessageType(msgType = FRIEND_REQUEST_MSGTYPE, body = "👋 Friend request"),
                ),
                senderId = A_USER_ID,
                senderProfile = aProfileDetails(),
                isOwn = true,
            ),
        )
        assertThat(friendRequestFromSummary(own, ownUserId = A_USER_ID)).isNull()

        val notDm = aRoomSummary(
            roomId = A_ROOM_ID,
            isDm = false,
        )
        assertThat(friendRequestFromSummary(notDm, ownUserId = A_USER_ID)).isNull()
    }
}
