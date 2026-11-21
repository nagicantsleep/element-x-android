/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl.data

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_ROOM_ID_2
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test

class DefaultSchedulingServiceTest {
    private val content = buildJsonObject { put("msgtype", JsonPrimitive("m.text")) }

    private fun TestScope.createService(
        restApi: FakeSchedulingRestApi = FakeSchedulingRestApi(),
        store: FakeScheduledMessageStore = FakeScheduledMessageStore(),
    ): DefaultSchedulingService {
        return DefaultSchedulingService(
            matrixClient = io.element.android.libraries.matrix.test.FakeMatrixClient(),
            restApi = restApi,
            store = store,
        )
    }

    private fun aRecord(delayId: String = "delay-1", roomId: String = A_ROOM_ID.value, sendAt: Long = 123) =
        ScheduledMessageRecord(
            delayId = delayId,
            roomId = roomId,
            sendAt = sendAt,
            content = content,
            eventType = "m.room.message",
        )

    @Test
    fun `refreshSupport - reports the versions flag once per session`() = runTest {
        val restApi = FakeSchedulingRestApi(supportsScheduling = true)
        val service = createService(restApi)

        service.refreshSupport()
        assertThat(service.state.value.isSupported).isTrue()

        restApi.supportsScheduling = false
        service.refreshSupport()
        // Cached: still supported.
        assertThat(service.state.value.isSupported).isTrue()
    }

    @Test
    fun `refreshSupport - defaults to unsupported when the versions call fails`() = runTest {
        val restApi = FakeSchedulingRestApi()
        restApi.supportsScheduling = false
        val service = createService(restApi)

        service.refreshSupport()
        assertThat(service.state.value.isSupported).isFalse()
    }

    @Test
    fun `schedule - PUTs the delayed event and persists the record`() = runTest {
        val restApi = FakeSchedulingRestApi(
            sendResult = Result.success(ScheduleSendResponse("delay-42")),
        )
        val service = createService(restApi)
        service.observeRoom(A_ROOM_ID)

        val result = service.schedule(roomId = A_ROOM_ID, eventType = "m.room.message", content = content, sendAt = System.currentTimeMillis() + 60_000)

        assertThat(result.isSuccess).isTrue()
        val sent = restApi.sentEvents.single()
        assertThat(sent.roomId).isEqualTo(A_ROOM_ID)
        assertThat(sent.eventType).isEqualTo("m.room.message")
        assertThat(sent.delayMs).isGreaterThan(0)
        assertThat(restApi.updates).isEmpty()
        assertThat(service.state.value.scheduled.single().delayId).isEqualTo("delay-42")
    }

    @Test
    fun `schedule - maps a 404 to SchedulingUnsupportedException`() = runTest {
        val restApi = FakeSchedulingRestApi(
            sendResult = Result.failure(SchedulingRestException(404, IllegalStateException("no plugin"))),
        )
        val service = createService(restApi)

        val result = service.schedule(roomId = A_ROOM_ID, eventType = "m.room.message", content = content, sendAt = 1)

        assertThat(result.exceptionOrNull()).isInstanceOf(SchedulingUnsupportedException::class.java)
        assertThat(service.state.value.scheduled).isEmpty()
    }

    @Test
    fun `sendNow - POSTs action send and drops the record`() = runTest {
        val restApi = FakeSchedulingRestApi()
        val store = FakeScheduledMessageStore()
        store.upsert(aRecord(delayId = "delay-1"))
        val service = createService(restApi, store)
        service.observeRoom(A_ROOM_ID)

        val result = service.sendNow("delay-1")

        assertThat(result.isSuccess).isTrue()
        assertThat(restApi.updates.single()).isEqualTo(FakeSchedulingRestApi.UpdateCall("delay-1", "send"))
        assertThat(store.all()).isEmpty()
        assertThat(service.state.value.scheduled).isEmpty()
    }

    @Test
    fun `cancel - POSTs action cancel and drops the record`() = runTest {
        val restApi = FakeSchedulingRestApi()
        val store = FakeScheduledMessageStore()
        store.upsert(aRecord(delayId = "delay-2"))
        val service = createService(restApi, store)
        service.observeRoom(A_ROOM_ID)

        val result = service.cancel("delay-2")

        assertThat(result.isSuccess).isTrue()
        assertThat(restApi.updates.single()).isEqualTo(FakeSchedulingRestApi.UpdateCall("delay-2", "cancel"))
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun `sendNow - 404 still drops the stale record`() = runTest {
        val restApi = FakeSchedulingRestApi(
            updateResult = Result.failure(SchedulingRestException(404, IllegalStateException("gone"))),
        )
        val store = FakeScheduledMessageStore()
        store.upsert(aRecord(delayId = "delay-3"))
        val service = createService(restApi, store)

        val result = service.sendNow("delay-3")

        assertThat(result.isSuccess).isTrue()
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun `sendNow - non-404 failure keeps the record and fails`() = runTest {
        val restApi = FakeSchedulingRestApi(
            updateResult = Result.failure(SchedulingRestException(500, IllegalStateException("boom"))),
        )
        val store = FakeScheduledMessageStore()
        store.upsert(aRecord(delayId = "delay-4"))
        val service = createService(restApi, store)

        val result = service.sendNow("delay-4")

        assertThat(result.isFailure).isTrue()
        assertThat(store.all()).hasSize(1)
    }

    @Test
    fun `observeRoom - lists only the room's records sorted by sendAt`() = runTest {
        val store = FakeScheduledMessageStore()
        store.upsert(aRecord(delayId = "late", sendAt = 2_000))
        store.upsert(aRecord(delayId = "early", sendAt = 1_000))
        store.upsert(aRecord(delayId = "other-room", roomId = A_ROOM_ID_2.value))
        val service = createService(store = store)

        service.observeRoom(A_ROOM_ID)

        assertThat(service.state.value.scheduled.map { it.delayId }).containsExactly("early", "late").inOrder()
    }
}
