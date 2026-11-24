/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.friends.impl.model

import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.roomlist.LatestEventValue
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.isFriendRequestMarker

enum class FriendRequestDirection { INCOMING, OUTGOING }

/** A confirmed friend edge, from either the server REST list or the local `m.friend_list` mirror. */
data class FriendEntry(
    val userId: UserId,
    val displayName: String?,
    val addedAt: Long,
)

/** A pending friend request, incoming ("someone asked me") or outgoing ("I asked them"). */
data class FriendRequestEntry(
    val userId: UserId,
    val displayName: String?,
    val createdAt: Long,
    val direction: FriendRequestDirection,
)

enum class FriendRequestResult {
    /** The target had no pending request for us; the server recorded our request. */
    Requested,

    /** An edge already exists. */
    AlreadyFriends,

    /** The target had already asked us; the request auto-completed into an edge. */
    EdgeCreated,
}

/** Outcome of the POST /_synapse/client/friends/request call. */
enum class FriendRequestOutcome {
    REQUESTED, ALREADY_FRIENDS, ACCEPTED,
}

/**
 * Pure merge helpers shared by [io.element.android.features.friends.impl.data.FriendsService].
 *
 * The server REST list is authoritative; local `m.friend_list` / `m.friend_requests` account data
 * only contributes web-accepted edges and web-originated requests that the server has never seen.
 */
object FriendsMerge {
    fun friends(server: List<FriendEntry>, local: List<FriendEntry>): List<FriendEntry> {
        val serverIds = server.map { it.userId }.toSet()
        return server + local.filter { it.userId !in serverIds }
    }

    fun requests(
        serverIncoming: List<FriendRequestEntry>,
        serverOutgoing: List<FriendRequestEntry>,
        local: List<FriendRequestEntry>,
    ): List<FriendRequestEntry> {
        val serverIds = (serverIncoming + serverOutgoing).map { it.userId }.toSet()
        return serverIncoming + serverOutgoing + local.filter { it.userId !in serverIds }
    }
}

/** The `msgtype` carried by the friend-request DM marker message, shared with the web fork. */
const val FRIEND_REQUEST_MSGTYPE = "m.friend_request"

/**
 * Extract an incoming friend request from a room summary's latest event, mirroring web's
 * `observeFriendRequest`: a DM `m.room.message` friend-request marker (legacy
 * `msgtype: m.friend_request` or v2 `format: io.nagi.friend_request`) sent by someone else.
 * Returns null when the summary is not a friend-request marker.
 */
fun friendRequestFromSummary(summary: RoomSummary, ownUserId: UserId): FriendRequestEntry? {
    if (!summary.info.isDm) return null
    val latest = summary.latestEvent as? LatestEventValue.Remote ?: return null
    if (latest.isOwn) return null
    if (latest.senderId == ownUserId) return null
    val messageType = (latest.content as? MessageContent)?.type ?: return null
    if (!messageType.isFriendRequestMarker()) return null
    return FriendRequestEntry(
        userId = latest.senderId,
        displayName = null,
        createdAt = latest.timestamp,
        direction = FriendRequestDirection.INCOMING,
    )
}
