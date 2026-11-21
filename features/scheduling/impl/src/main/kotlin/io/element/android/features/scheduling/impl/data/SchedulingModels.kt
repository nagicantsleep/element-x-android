/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ScheduleSendResponse(
    @SerialName("delay_id") val delayId: String,
)

@Serializable
data class DelayedEventDto(
    @SerialName("delay_id") val delayId: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("type") val eventType: String,
    @SerialName("delay_ms") val delayMs: Long = 0,
    val content: JsonObject? = null,
)

@Serializable
data class DelayedEventsResponse(
    @SerialName("delayed_events") val delayedEvents: List<DelayedEventDto> = emptyList(),
)

@Serializable
data class UpdateDelayedEventBody(
    val action: String,
)

/** Per-device record of a scheduled send, mirroring web's `ScheduledMessage` shape. */
@Serializable
data class ScheduledMessageRecord(
    @SerialName("delayId") val delayId: String,
    @SerialName("roomId") val roomId: String,
    /** Epoch ms at which the server will deliver the message. */
    @SerialName("sendAt") val sendAt: Long,
    @SerialName("content") val content: JsonObject,
    @SerialName("eventType") val eventType: String,
)
