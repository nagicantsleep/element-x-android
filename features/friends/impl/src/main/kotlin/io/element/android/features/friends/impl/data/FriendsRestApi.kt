/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.friends.impl.data

import io.element.android.features.friends.impl.model.FriendRequestOutcome
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.network.RetrofitFactory
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.SessionScope

/** REST error carrying the HTTP status so callers can implement fallbacks (e.g. 404 on /accept). */
class FriendsRestException(val statusCode: Int, cause: Throwable) : Exception(cause)

@Serializable
data class FriendsListResponse(
    @SerialName("friends") val friends: List<FriendDto> = emptyList(),
)

@Serializable
data class FriendDto(
    @SerialName("user_id") val userId: String,
    @SerialName("added_at") val addedAt: Long = 0,
)

@Serializable
data class FriendRequestsResponse(
    @SerialName("incoming") val incoming: List<FriendRequestDto> = emptyList(),
    @SerialName("outgoing") val outgoing: List<FriendRequestDto> = emptyList(),
)

@Serializable
data class FriendRequestDto(
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: Long = 0,
)

@Serializable
data class FriendUserBody(
    @SerialName("user_id") val userId: String,
)

@Serializable
data class FriendRequestPostResponse(
    @SerialName("result") val result: String = "requested",
)

/** The raw `/_synapse/client/friends*` surface of the server friends plugin. */
interface RetrofitFriendsApi {
    @GET("_synapse/client/friends")
    suspend fun friends(@Header("Authorization") authorization: String): FriendsListResponse

    @GET("_synapse/client/friends/requests")
    suspend fun requests(@Header("Authorization") authorization: String): FriendRequestsResponse

    @POST("_synapse/client/friends/request")
    suspend fun request(
        @Header("Authorization") authorization: String,
        @Body body: FriendUserBody,
    ): FriendRequestPostResponse

    @POST("_synapse/client/friends/accept")
    suspend fun accept(@Header("Authorization") authorization: String, @Body body: FriendUserBody)

    @POST("_synapse/client/friends/remove")
    suspend fun remove(@Header("Authorization") authorization: String, @Body body: FriendUserBody)
}

/**
 * High-level, failure-tolerant view of the friends REST surface. Errors are wrapped in
 * [FriendsRestException] so the service can distinguish "no plugin on this server" (404) from
 * real failures.
 */
interface FriendsRestApi {
    suspend fun friends(): Result<List<FriendDto>>
    suspend fun requests(): Result<FriendRequestsResponse>
    suspend fun requestFriend(userId: UserId): Result<FriendRequestOutcome>
    suspend fun acceptFriend(userId: UserId): Result<Unit>
    suspend fun removeFriend(userId: UserId): Result<Unit>
}

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultFriendsRestApi(
    private val matrixClient: MatrixClient,
    private val sessionStore: SessionStore,
    private val retrofitFactory: RetrofitFactory,
) : FriendsRestApi {
    private val api: RetrofitFriendsApi by lazy {
        retrofitFactory.create(matrixClient.homeserverUrl).create(RetrofitFriendsApi::class.java)
    }

    private suspend fun authHeader(): Result<String> {
        val token = sessionStore.getSession(matrixClient.sessionId.value)?.accessToken
        return if (token.isNullOrBlank()) {
            Result.failure(IllegalStateException("No access token for friends request"))
        } else {
            Result.success("Bearer $token")
        }
    }

    private fun <T> mapFailure(cause: Throwable): Result<T> {
        val restException = (cause as? HttpException)?.let { FriendsRestException(it.code(), it) } ?: cause
        return Result.failure(restException)
    }

    override suspend fun friends(): Result<List<FriendDto>> {
        return authHeader().flatMapCatching { api.friends(it).friends }
    }

    override suspend fun requests(): Result<FriendRequestsResponse> {
        return authHeader().flatMapCatching { api.requests(it) }
    }

    override suspend fun requestFriend(userId: UserId): Result<FriendRequestOutcome> {
        return authHeader().flatMapCatching { auth ->
            when (api.request(auth, FriendUserBody(userId.value)).result) {
                "already_friends" -> FriendRequestOutcome.ALREADY_FRIENDS
                "accepted" -> FriendRequestOutcome.ACCEPTED
                else -> FriendRequestOutcome.REQUESTED
            }
        }
    }

    override suspend fun acceptFriend(userId: UserId): Result<Unit> {
        return authHeader().flatMapCatching { api.accept(it, FriendUserBody(userId.value)) }
    }

    override suspend fun removeFriend(userId: UserId): Result<Unit> {
        return authHeader().flatMapCatching { api.remove(it, FriendUserBody(userId.value)) }
    }

    private suspend inline fun <T, R> Result<T>.flatMapCatching(block: (T) -> R): Result<R> {
        return runCatchingExceptions { block(getOrThrow()) }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { mapFailure(it) },
        )
    }
}
