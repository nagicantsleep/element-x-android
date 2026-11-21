/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.friends.impl.data

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.features.friends.impl.model.FRIEND_REQUEST_MSGTYPE
import io.element.android.features.friends.impl.model.FriendEntry
import io.element.android.features.friends.impl.model.FriendRequestDirection
import io.element.android.features.friends.impl.model.FriendRequestEntry
import io.element.android.features.friends.impl.model.FriendRequestOutcome
import io.element.android.features.friends.impl.model.FriendsMerge
import io.element.android.features.friends.impl.model.friendRequestFromSummary
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.StartDMResult
import io.element.android.libraries.matrix.api.room.startDM
import io.element.android.libraries.matrix.api.timeline.MsgType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

const val FRIENDS_EVENT_TYPE = "m.friend_list"
const val FRIEND_REQUESTS_EVENT_TYPE = "m.friend_requests"

data class FriendsUiState(
    val friends: List<FriendEntry> = emptyList(),
    val incoming: List<FriendRequestEntry> = emptyList(),
    val outgoing: List<FriendRequestEntry> = emptyList(),
    val addFriendAction: AsyncAction<FriendRequestOutcome> = AsyncAction.Uninitialized,
    val loading: Boolean = false,
)

interface FriendsService {
    val state: StateFlow<FriendsUiState>

    /** Refresh the server lists, falling back to local account data when the plugin is absent. */
    suspend fun refresh()

    /**
     * Keep observing the room list for incoming `m.friend_request` DM markers (web interop) and
     * record them into the local `m.friend_requests` account data. Runs until cancelled.
     */
    suspend fun observeIncomingRequests()

    /** Send a friend request via the server plugin, plus the DM marker for web interop. */
    suspend fun addFriend(userId: UserId)

    /** Accept an incoming request; on 404 (web-originated) fall back to a local edge. */
    suspend fun acceptFriend(userId: UserId)

    /** Cancel an outgoing request or remove a friend. Clears local account data too. */
    suspend fun removeFriend(userId: UserId)
}

@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
class DefaultFriendsService(
    private val matrixClient: MatrixClient,
    private val restApi: FriendsRestApi,
    private val jsonProvider: JsonProvider,
) : FriendsService {
    private val _state = MutableStateFlow(FriendsUiState())
    override val state: StateFlow<FriendsUiState> = _state.asStateFlow()

    private val json by lazy { jsonProvider() }
    private val knownRequestSenders = mutableSetOf<UserId>()

    override suspend fun refresh() {
        val serverFriends = restApi.friends().getOrNull().orEmpty()
        val serverRequests = restApi.requests().getOrNull()
        val friends = FriendsMerge.friends(
            server = serverFriends.map { FriendEntry(UserId(it.userId), null, it.addedAt) },
            local = readFriendList(),
        )
        val mergedRequests = FriendsMerge.requests(
            serverIncoming = serverRequests?.incoming.orEmpty()
                .map { FriendRequestEntry(UserId(it.userId), null, it.createdAt, FriendRequestDirection.INCOMING) },
            serverOutgoing = serverRequests?.outgoing.orEmpty()
                .map { FriendRequestEntry(UserId(it.userId), null, it.createdAt, FriendRequestDirection.OUTGOING) },
            local = readFriendRequests(),
        )
        _state.value = _state.value.copy(
            friends = friends,
            incoming = mergedRequests.filter { it.direction == FriendRequestDirection.INCOMING },
            outgoing = mergedRequests.filter { it.direction == FriendRequestDirection.OUTGOING },
            loading = false,
        )
    }

    override suspend fun observeIncomingRequests() {
        matrixClient.roomListService.allRooms.summaries.collect { summaries ->
            val incoming = summaries.mapNotNull { friendRequestFromSummary(it, matrixClient.sessionId) }
                .filter { it.userId !in knownRequestSenders && it.userId != matrixClient.sessionId }
            if (incoming.isEmpty()) return@collect
            knownRequestSenders += incoming.map { it.userId }
            incoming.forEach { rememberRequest(it) }
            refresh()
        }
    }

    override suspend fun addFriend(userId: UserId) {
        _state.value = _state.value.copy(addFriendAction = AsyncAction.Loading)
        val outcome = restApi.requestFriend(userId).fold(
            onSuccess = { it },
            onFailure = { error ->
                if ((error as? FriendsRestException)?.statusCode == 404) {
                    // No friends plugin on this server: use the client-authoritative model, exactly
                    // like web's sendFriendRequest.
                    FriendRequestOutcome.REQUESTED
                } else {
                    _state.value = _state.value.copy(addFriendAction = AsyncAction.Failure(error))
                    return
                }
            },
        )
        if (outcome == FriendRequestOutcome.ACCEPTED) {
            // The target had already asked us; the server created the edge and mirrors it into our
            // account data. Record it locally too so the UI updates immediately.
            upsertFriendListEntry(FriendEntry(userId, displayName = null, addedAt = System.currentTimeMillis()))
        } else if (outcome == FriendRequestOutcome.REQUESTED) {
            // Mobile-sent requests must ALSO send the DM marker message over a DM room, so web
            // users see the request.
            sendFriendRequestMarker(userId)
            rememberRequest(
                FriendRequestEntry(
                    userId = userId,
                    displayName = null,
                    createdAt = System.currentTimeMillis(),
                    direction = FriendRequestDirection.OUTGOING,
                ),
            )
        }
        _state.value = _state.value.copy(addFriendAction = AsyncAction.Success(outcome))
        refresh()
    }

    override suspend fun acceptFriend(userId: UserId) {
        val result = restApi.acceptFriend(userId)
        if (result.isFailure && (result.exceptionOrNull() as? FriendsRestException)?.statusCode == 404) {
            // Web-originated request: the server has no pending entry, write the edge into the
            // local m.friend_list the way web's addFriend does.
            upsertFriendListEntry(FriendEntry(userId, displayName = null, addedAt = System.currentTimeMillis()))
            removeRequestEntry(userId)
        }
        refresh()
    }

    override suspend fun removeFriend(userId: UserId) {
        // POST /remove deletes the edge and pending requests in BOTH directions; for
        // web-originated data the local delete is the only thing that exists.
        restApi.removeFriend(userId)
        removeFriendListEntry(userId)
        removeRequestEntry(userId)
        refresh()
    }

    private suspend fun rememberRequest(entry: FriendRequestEntry) {
        val content = readAccountDataContent(FRIEND_REQUESTS_EVENT_TYPE)
        content[entry.userId.value] = buildJsonObject {
            put("user_id", entry.userId.value)
            put("direction", if (entry.direction == FriendRequestDirection.INCOMING) "in" else "out")
            put("created_at", entry.createdAt)
        }
        writeAccountData(FRIEND_REQUESTS_EVENT_TYPE, JsonObject(content))
    }

    private suspend fun upsertFriendListEntry(entry: FriendEntry) {
        val content = readAccountDataContent(FRIENDS_EVENT_TYPE)
        content[entry.userId.value] = buildJsonObject {
            put("user_id", entry.userId.value)
            put("added_at", entry.addedAt)
        }
        writeAccountData(FRIENDS_EVENT_TYPE, JsonObject(content))
    }

    private suspend fun removeFriendListEntry(userId: UserId) {
        val content = readAccountDataContent(FRIENDS_EVENT_TYPE)
        if (content.remove(userId.value) != null) {
            writeAccountData(FRIENDS_EVENT_TYPE, JsonObject(content))
        }
    }

    private suspend fun removeRequestEntry(userId: UserId) {
        val content = readAccountDataContent(FRIEND_REQUESTS_EVENT_TYPE)
        if (content.remove(userId.value) != null) {
            writeAccountData(FRIEND_REQUESTS_EVENT_TYPE, JsonObject(content))
        }
    }

    private suspend fun readAccountDataContent(eventType: String): MutableMap<String, kotlinx.serialization.json.JsonElement> {
        val raw = matrixClient.getAccountData(eventType).getOrNull() ?: return mutableMapOf()
        return runCatching { json.parseToJsonElement(raw).jsonObject.toMutableMap() }.getOrDefault(mutableMapOf())
    }

    private suspend fun readFriendList(): List<FriendEntry> {
        return readAccountDataContent(FRIENDS_EVENT_TYPE).map { (key, value) ->
            val obj = value as? JsonObject
            FriendEntry(
                userId = UserId(key),
                displayName = obj?.stringField("display_name"),
                addedAt = obj?.longField("added_at") ?: 0L,
            )
        }
    }

    private suspend fun readFriendRequests(): List<FriendRequestEntry> {
        return readAccountDataContent(FRIEND_REQUESTS_EVENT_TYPE).mapNotNull { (key, value) ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            val direction = when (obj.stringField("direction")) {
                "in" -> FriendRequestDirection.INCOMING
                "out" -> FriendRequestDirection.OUTGOING
                else -> return@mapNotNull null
            }
            FriendRequestEntry(
                userId = UserId(key),
                displayName = obj.stringField("display_name"),
                createdAt = obj.longField("created_at") ?: 0L,
                direction = direction,
            )
        }
    }

    private fun JsonObject.stringField(name: String): String? {
        return (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }

    private fun JsonObject.longField(name: String): Long? {
        return (this[name] as? JsonPrimitive)?.content?.toLongOrNull()
    }

    private suspend fun writeAccountData(eventType: String, content: JsonObject) {
        matrixClient.setAccountData(eventType, content.toString())
    }

    private suspend fun sendFriendRequestMarker(userId: UserId) {
        val roomId = when (val result = matrixClient.startDM(
            userId = userId,
            createIfDmDoesNotExist = true,
            // Same default as the app's own DM creation flow.
            isEncryped = true,
        )) {
            is StartDMResult.Success -> result.roomId
            else -> return
        }
        val joinedRoom = matrixClient.getJoinedRoom(roomId) ?: return
        joinedRoom.liveTimeline.sendMessage(
            body = "👋 Friend request",
            htmlBody = null,
            intentionalMentions = emptyList(),
            msgType = MsgType.MSG_TYPE_FRIEND_REQUEST,
            asPlainText = true,
        )
    }

    companion object {
        /** Marker msgtype used by the web fork; kept next to [FRIEND_REQUEST_MSGTYPE] for clarity. */
        const val FRIEND_REQUEST_MSG_TYPE = FRIEND_REQUEST_MSGTYPE
    }
}
