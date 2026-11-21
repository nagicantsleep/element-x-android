/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl.data

import android.content.Context
import androidx.core.content.edit
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Per-device CRUD store for scheduled-send records, keyed by delay_id in SharedPreferences. */
interface ScheduledMessageStore {
    suspend fun upsert(record: ScheduledMessageRecord)

    suspend fun remove(delayId: String)

    suspend fun forRoom(roomId: String): List<ScheduledMessageRecord>

    suspend fun all(): List<ScheduledMessageRecord>

    suspend fun get(delayId: String): ScheduledMessageRecord?
}

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultScheduledMessageStore(
    @ApplicationContext context: Context,
    sessionId: SessionId,
    private val jsonProvider: JsonProvider,
) : ScheduledMessageStore {
    private val json: Json by lazy { jsonProvider() }
    private val prefs = context.getSharedPreferences(prefsName(sessionId), Context.MODE_PRIVATE)

    override suspend fun upsert(record: ScheduledMessageRecord) {
        prefs.edit { putString(record.delayId, json.encodeToString(record)) }
    }

    override suspend fun remove(delayId: String) {
        prefs.edit { remove(delayId) }
    }

    override suspend fun forRoom(roomId: String): List<ScheduledMessageRecord> {
        return all().filter { it.roomId == roomId }.sortedBy { it.sendAt }
    }

    override suspend fun all(): List<ScheduledMessageRecord> {
        return prefs.all.values
            .filterIsInstance<String>()
            .mapNotNull { raw ->
                runCatching { json.decodeFromString<ScheduledMessageRecord>(raw) }.getOrNull()
            }
            .sortedBy { it.sendAt }
    }

    override suspend fun get(delayId: String): ScheduledMessageRecord? {
        val raw = prefs.getString(delayId, null) ?: return null
        return runCatching { json.decodeFromString<ScheduledMessageRecord>(raw) }.getOrNull()
    }

    companion object {
        fun prefsName(sessionId: SessionId): String {
            // Match the per-session file naming used by the preferences store.
            return "session_${sessionId.value.hashCode()}_scheduled_messages"
        }
    }
}
