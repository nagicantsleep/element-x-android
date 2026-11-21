/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl

import io.element.android.libraries.matrix.api.room.IntentionalMention
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** "Send later" presets, mirroring web's SchedulingMenu semantics (fixed offsets + custom picker). */
enum class SendLaterOption(val delayMs: Long) {
    OneHour(60 * 60_000L),
    EightHours(8 * 60 * 60_000L),
    TwentyFourHours(24 * 60 * 60_000L),
    Custom(0L);

    /** Epoch ms at which the server should deliver; ignores [now] for the custom option. */
    fun sendAt(now: Long = System.currentTimeMillis()): Long {
        return if (this == Custom) now else now + delayMs
    }
}

/**
 * Builds the `m.room.message` content JSON that the delayed PUT carries. Plain text wave 2:
 * markdown body plus optional HTML body and intentional mentions.
 */
fun scheduledMessageContent(
    body: String,
    htmlBody: String?,
    intentionalMentions: List<IntentionalMention> = emptyList(),
): JsonObject {
    val mentionedUserIds = intentionalMentions.filterIsInstance<IntentionalMention.User>().map { it.userId.value }
    val hasRoomMention = intentionalMentions.any { it is IntentionalMention.Room }
    return buildJsonObject {
        put("msgtype", "m.text")
        put("body", body)
        htmlBody?.let {
            put("format", "org.matrix.custom.html")
            put("formatted_body", it)
        }
        if (mentionedUserIds.isNotEmpty() || hasRoomMention) {
            put("m.mentions", buildJsonObject {
                if (mentionedUserIds.isNotEmpty()) {
                    put("user_ids", JsonArray(mentionedUserIds.map { JsonPrimitive(it) }))
                }
                if (hasRoomMention) {
                    put("room", true)
                }
            })
        }
    }
}
