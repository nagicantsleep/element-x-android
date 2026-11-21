/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl.data

import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.network.RetrofitFactory
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.di.SessionScope

/** REST error carrying the HTTP status so callers can map "no MSC4140 support" (404) to a fallback. */
class SchedulingRestException(val statusCode: Int, cause: Throwable) : Exception(cause)

/** Raised when the server has no MSC4140 support (404 or `org.matrix.msc4140` flag off). */
class SchedulingUnsupportedException(cause: Throwable) : Exception(cause)

@Serializable
data class VersionsResponse(
    val versions: List<String> = emptyList(),
    @SerialName("unstable_features") val unstableFeatures: Map<String, Boolean> = emptyMap(),
)

/** The raw MSC4140 delayed-events surface of the homeserver. */
interface RetrofitSchedulingApi {
    @PUT("rooms/{roomId}/send/{eventType}/{txnId}")
    suspend fun sendDelayedEvent(
        @Header("Authorization") authorization: String,
        @Path("roomId") roomId: String,
        @Path("eventType") eventType: String,
        @Path("txnId") txnId: String,
        @Query("org.matrix.msc4140.delay") delayMs: Long,
        @Body content: JsonObject,
    ): ScheduleSendResponse

    @GET("unstable/org.matrix.msc4140/delayed_events")
    suspend fun delayedEvents(@Header("Authorization") authorization: String): DelayedEventsResponse

    // MSC4140 update verb is POST; PUT on this path returns 405.
    @POST("unstable/org.matrix.msc4140/delayed_events/{delayId}")
    suspend fun updateDelayedEvent(
        @Header("Authorization") authorization: String,
        @Path("delayId") delayId: String,
        @Body body: UpdateDelayedEventBody,
    )

    @GET("versions")
    suspend fun versions(@Header("Authorization") authorization: String): VersionsResponse
}

/**
 * High-level view of the MSC4140 REST surface. Errors are wrapped in [SchedulingRestException]
 * so the service can distinguish "no support" (404) from real failures.
 */
interface SchedulingRestApi {
    /** PUT the event content with `?org.matrix.msc4140.delay=<ms>`, returns the server delay_id. */
    suspend fun sendDelayedEvent(
        roomId: RoomId,
        eventType: String,
        content: JsonObject,
        delayMs: Long,
        txnId: String,
    ): Result<ScheduleSendResponse>

    suspend fun delayedEvents(): Result<DelayedEventsResponse>

    /** POST `{action: send|cancel|restart}` for a delayed event. */
    suspend fun updateDelayedEvent(delayId: String, action: String): Result<Unit>

    /** True only when GET /versions reports `unstable_features["org.matrix.msc4140"] == true`. */
    suspend fun serverSupportsScheduling(): Result<Boolean>
}

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultSchedulingRestApi(
    private val matrixClient: MatrixClient,
    private val sessionStore: SessionStore,
    private val retrofitFactory: RetrofitFactory,
) : SchedulingRestApi {
    private val api: RetrofitSchedulingApi by lazy {
        retrofitFactory.create(matrixClient.homeserverUrl).create(RetrofitSchedulingApi::class.java)
    }

    private suspend fun authHeader(): Result<String> {
        val token = sessionStore.getSession(matrixClient.sessionId.value)?.accessToken
        return if (token.isNullOrBlank()) {
            Result.failure(IllegalStateException("No access token for scheduling request"))
        } else {
            Result.success("Bearer $token")
        }
    }

    private fun <T> mapFailure(cause: Throwable): Result<T> {
        val restException = (cause as? HttpException)?.let { SchedulingRestException(it.code(), it) } ?: cause
        return Result.failure(restException)
    }

    private suspend inline fun <T, R> Result<T>.flatMapCatching(block: (T) -> R): Result<R> {
        return runCatchingExceptions { block(getOrThrow()) }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { mapFailure(it) },
        )
    }

    override suspend fun sendDelayedEvent(
        roomId: RoomId,
        eventType: String,
        content: JsonObject,
        delayMs: Long,
        txnId: String,
    ): Result<ScheduleSendResponse> {
        return authHeader().flatMapCatching { auth ->
            api.sendDelayedEvent(auth, roomId.value, eventType, txnId, delayMs, content)
        }
    }

    override suspend fun delayedEvents(): Result<DelayedEventsResponse> {
        return authHeader().flatMapCatching { api.delayedEvents(it) }
    }

    override suspend fun updateDelayedEvent(delayId: String, action: String): Result<Unit> {
        return authHeader().flatMapCatching { api.updateDelayedEvent(it, delayId, UpdateDelayedEventBody(action)) }
    }

    override suspend fun serverSupportsScheduling(): Result<Boolean> {
        return authHeader().flatMapCatching { auth -> api.versions(auth).unstableFeatures[MSC4140_UNSTABLE_FEATURE] == true }
    }

    companion object {
        const val MSC4140_UNSTABLE_FEATURE = "org.matrix.msc4140"
    }
}
