/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl.data

import android.content.Context
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.androidutils.json.DefaultJsonProvider
import io.element.android.libraries.matrix.test.A_SESSION_ID
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DefaultScheduledMessageStoreTest {
    private lateinit var store: DefaultScheduledMessageStore

    private fun aRecord(delayId: String, roomId: String = "!room:server.org", sendAt: Long = 100) =
        ScheduledMessageRecord(
            delayId = delayId,
            roomId = roomId,
            sendAt = sendAt,
            content = buildJsonObject { put("body", JsonPrimitive("hello")) },
            eventType = "m.room.message",
        )

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication() as Context
        context.getSharedPreferences(DefaultScheduledMessageStore.prefsName(A_SESSION_ID), Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        store = DefaultScheduledMessageStore(context, A_SESSION_ID, DefaultJsonProvider())
    }

    @Test
    fun `upsert and get roundtrip a record`() = runTest {
        val record = aRecord("delay-1")

        store.upsert(record)

        assertThat(store.get("delay-1")).isEqualTo(record)
        assertThat(store.all()).containsExactly(record)
    }

    @Test
    fun `upsert replaces an existing record with the same delay_id`() = runTest {
        store.upsert(aRecord("delay-1", sendAt = 100))
        store.upsert(aRecord("delay-1", sendAt = 200))

        assertThat(store.all().single().sendAt).isEqualTo(200)
    }

    @Test
    fun `remove deletes the record`() = runTest {
        store.upsert(aRecord("delay-1"))

        store.remove("delay-1")

        assertThat(store.get("delay-1")).isNull()
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun `forRoom filters and sorts by sendAt`() = runTest {
        store.upsert(aRecord("b", roomId = "!room:server.org", sendAt = 200))
        store.upsert(aRecord("a", roomId = "!room:server.org", sendAt = 100))
        store.upsert(aRecord("other", roomId = "!other:server.org"))

        val records = store.forRoom("!room:server.org")

        assertThat(records.map { it.delayId }).containsExactly("a", "b").inOrder()
    }

    @Test
    fun `records survive a new instance of the store`() = runTest {
        val context = RuntimeEnvironment.getApplication() as Context
        store.upsert(aRecord("delay-1"))

        val secondStore = DefaultScheduledMessageStore(context, A_SESSION_ID, DefaultJsonProvider())

        assertThat(secondStore.get("delay-1")).isNotNull()
    }
}
