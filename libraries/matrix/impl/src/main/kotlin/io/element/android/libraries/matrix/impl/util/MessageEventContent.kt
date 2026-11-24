/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.util

import io.element.android.libraries.matrix.api.room.IntentionalMention
import io.element.android.libraries.matrix.api.timeline.MsgType
import io.element.android.libraries.matrix.impl.room.map
import org.matrix.rustcomponents.sdk.MessageContent
import org.matrix.rustcomponents.sdk.MessageType
import org.matrix.rustcomponents.sdk.RoomMessageEventContentWithoutRelation
import org.matrix.rustcomponents.sdk.TextMessageContent
import org.matrix.rustcomponents.sdk.contentWithoutRelationFromMessage
import org.matrix.rustcomponents.sdk.messageEventContentFromHtml
import org.matrix.rustcomponents.sdk.messageEventContentFromHtmlAsEmote
import org.matrix.rustcomponents.sdk.messageEventContentFromMarkdown
import org.matrix.rustcomponents.sdk.messageEventContentFromMarkdownAsEmote
import org.matrix.rustcomponents.sdk.FormattedBody as FfiFormattedBody
import org.matrix.rustcomponents.sdk.MessageFormat as FfiMessageFormat

/**
 * Creates a [RoomMessageEventContentWithoutRelation] from a body, an html body and a list of mentions.
 */
object MessageEventContent {
    /** Custom `format` value of a friend-request DM marker, wire format v2 (ADR 0016). */
    private const val FRIEND_REQUEST_FORMAT = "io.nagi.friend_request"

    fun from(
        body: String,
        htmlBody: String?,
        intentionalMentions: List<IntentionalMention>,
        msgType: MsgType = MsgType.MSG_TYPE_TEXT,
        asPlainText: Boolean = false,
    ): RoomMessageEventContentWithoutRelation {
        return when {
            // Friend requests are sent as plain text with a custom `format` value (wire format v2,
            // ADR 0016): the Rust SDK passes the unknown format through as MessageFormat.Unknown,
            // and m.text avoids `.m.rule.suppress_notices`. The formatted body mirrors the plain
            // body for interop with the web fork.
            msgType == MsgType.MSG_TYPE_FRIEND_REQUEST -> contentWithoutRelationFromMessage(
                MessageContent(
                    msgType = MessageType.Text(
                        TextMessageContent(
                            body = body,
                            formatted = FfiFormattedBody(
                                format = FfiMessageFormat.Unknown(format = FRIEND_REQUEST_FORMAT),
                                body = body,
                            ),
                        )
                    ),
                    body = body,
                    isEdited = false,
                    mentions = null,
                )
            )
            asPlainText -> contentWithoutRelationFromMessage(
                MessageContent(
                    msgType = MessageType.Text(
                        TextMessageContent(
                            body = body,
                            formatted = null,
                        )
                    ),
                    body = body,
                    isEdited = false,
                    mentions = null,
                )
            )
            htmlBody != null -> if (msgType == MsgType.MSG_TYPE_EMOTE) {
                messageEventContentFromHtmlAsEmote(body, htmlBody)
            } else {
                messageEventContentFromHtml(body, htmlBody)
            }
            else -> if (msgType == MsgType.MSG_TYPE_EMOTE) {
                messageEventContentFromMarkdownAsEmote(body)
            } else {
                messageEventContentFromMarkdown(body)
            }
        }
            .withMentions(intentionalMentions.map())
    }
}
