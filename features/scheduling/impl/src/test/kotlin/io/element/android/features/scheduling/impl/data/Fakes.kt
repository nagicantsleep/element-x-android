/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl.data

import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.serialization.json.JsonObject

class FakeSchedulingRestApi(
    var sendResult: Result<ScheduleSendResponse> = Result.failure(IllegalStateException("not stubbed")),
    var updateResult: Result<Unit> = Result.success(Unit),
    var supportsScheduling: Boolean = false,
) : SchedulingRestApi {
    val sentEvents = mutableListOf<SentEvent>()
    val updates = mutableListOf<UpdateCall>()

    data class SentEvent(val roomId: RoomId, val eventType: String, val content: JsonObject, val delayMs: Long, val txnId: String)
    data class UpdateCall(val delayId: String, val action: String)

    override suspend fun sendDelayedEvent(
        roomId: RoomId,
        eventType: String,
        content: JsonObject,
        delayMs: Long,
        txnId: String,
    ): Result<ScheduleSendResponse> {
        sentEvents += SentEvent(roomId, eventType, content, delayMs, txnId)
        return sendResult
    }

    override suspend fun delayedEvents(): Result<DelayedEventsResponse> {
        return Result.success(DelayedEventsResponse())
    }

    override suspend fun updateDelayedEvent(delayId: String, action: String): Result<Unit> {
        updates += UpdateCall(delayId, action)
        return updateResult
    }

    override suspend fun serverSupportsScheduling(): Result<Boolean> {
        return Result.success(supportsScheduling)
    }
}

class FakeScheduledMessageStore : ScheduledMessageStore {
    private val records = mutableMapOf<String, ScheduledMessageRecord>()

    override suspend fun upsert(record: ScheduledMessageRecord) {
        records[record.delayId] = record
    }

    override suspend fun remove(delayId: String) {
        records.remove(delayId)
    }

    override suspend fun forRoom(roomId: String): List<ScheduledMessageRecord> {
        return records.values.filter { it.roomId == roomId }.sortedBy { it.sendAt }
    }

    override suspend fun all(): List<ScheduledMessageRecord> {
        return records.values.sortedBy { it.sendAt }
    }

    override suspend fun get(delayId: String): ScheduledMessageRecord? = records[delayId]
}

class FakeSchedulingService(
    private val restApi: FakeSchedulingRestApi = FakeSchedulingRestApi(),
    private val store: FakeScheduledMessageStore = FakeScheduledMessageStore(),
) : SchedulingService by DefaultSchedulingService(
    matrixClient = io.element.android.libraries.matrix.test.FakeMatrixClient(),
    restApi = restApi,
    store = store,
)

