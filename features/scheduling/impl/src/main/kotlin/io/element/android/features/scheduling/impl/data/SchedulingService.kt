/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl.data

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject

data class SchedulingUiState(
    /** True when GET /versions reported `unstable_features["org.matrix.msc4140"] == true`. */
    val isSupported: Boolean = false,
    /** Records scheduled for the observed room, sorted by send time. */
    val scheduled: List<ScheduledMessageRecord> = emptyList(),
    val sendAction: AsyncAction<Unit> = AsyncAction.Uninitialized,
)

/**
 * Send-later orchestration: schedule via MSC4140 REST, persist per-device records,
 * expose per-room listings and send/cancel actions.
 */
interface SchedulingService {
    val state: StateFlow<SchedulingUiState>

    /** Query GET /versions once per session for the MSC4140 unstable feature flag. */
    suspend fun refreshSupport()

    /** PUT a delayed event and persist the returned delay_id against [sendAt]. */
    suspend fun schedule(roomId: RoomId, eventType: String, content: JsonObject, sendAt: Long): Result<Unit>

    /** POST action=send on the delay, then drop the record. */
    suspend fun sendNow(delayId: String): Result<Unit>

    /** POST action=cancel on the delay, then drop the record. */
    suspend fun cancel(delayId: String): Result<Unit>

    /** Point the per-room listing at [roomId]; call again when the room changes. */
    suspend fun observeRoom(roomId: RoomId)
}

@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
class DefaultSchedulingService(
    private val matrixClient: MatrixClient,
    private val restApi: SchedulingRestApi,
    private val store: ScheduledMessageStore,
) : SchedulingService {
    private val _state = MutableStateFlow(SchedulingUiState())
    override val state: StateFlow<SchedulingUiState> = _state.asStateFlow()

    private var supportCache: Boolean? = null
    private var observedRoomId: RoomId? = null

    override suspend fun refreshSupport() {
        val cached = supportCache
        if (cached != null) {
            _state.value = _state.value.copy(isSupported = cached)
            return
        }
        val supported = restApi.serverSupportsScheduling().getOrDefault(false)
        supportCache = supported
        _state.value = _state.value.copy(isSupported = supported)
    }

    override suspend fun schedule(roomId: RoomId, eventType: String, content: JsonObject, sendAt: Long): Result<Unit> {
        val delayMs = (sendAt - System.currentTimeMillis()).coerceAtLeast(1)
        val record = restApi.sendDelayedEvent(
            roomId = roomId,
            eventType = eventType,
            content = content,
            delayMs = delayMs,
            txnId = newTxnId(),
        ).fold(
            onSuccess = { response ->
                ScheduledMessageRecord(
                    delayId = response.delayId,
                    roomId = roomId.value,
                    sendAt = sendAt,
                    content = content,
                    eventType = eventType,
                )
            },
            onFailure = { error ->
                // MSC4140 absent on this server (404, or M_UNRECOGNIZED with the flag off).
                if ((error as? SchedulingRestException)?.statusCode == 404) {
                    return Result.failure(SchedulingUnsupportedException(error))
                }
                return Result.failure(error)
            },
        )
        store.upsert(record)
        refreshObservedRoom()
        return Result.success(Unit)
    }

    override suspend fun sendNow(delayId: String): Result<Unit> {
        return updateDelayed(delayId, ACTION_SEND)
    }

    override suspend fun cancel(delayId: String): Result<Unit> {
        return updateDelayed(delayId, ACTION_CANCEL)
    }

    override suspend fun observeRoom(roomId: RoomId) {
        observedRoomId = roomId
        _state.value = _state.value.copy(scheduled = store.forRoom(roomId.value))
    }

    /**
     * POST the [action] for a delay and drop the local record. A 404 means the delay is already
     * gone server-side (fired or never existed): the stale record is dropped and the action
     * still succeeds so the UI stays consistent.
     */
    private suspend fun updateDelayed(delayId: String, action: String): Result<Unit> {
        val record = store.get(delayId)
        val result = restApi.updateDelayedEvent(delayId, action).fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { error ->
                if ((error as? SchedulingRestException)?.statusCode == 404) {
                    Result.success(Unit)
                } else {
                    Result.failure(error)
                }
            },
        )
        if (result.isSuccess && record != null) {
            store.remove(delayId)
            refreshObservedRoom()
        }
        return result
    }

    private suspend fun refreshObservedRoom() {
        val roomId = observedRoomId ?: return
        _state.value = _state.value.copy(scheduled = store.forRoom(roomId.value))
    }

    private fun newTxnId(): String {
        return "sched.${System.nanoTime()}"
    }

    companion object {
        const val ACTION_SEND = "send"
        const val ACTION_CANCEL = "cancel"
        const val ACTION_RESTART = "restart"
    }
}
