/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import android.net.Uri
import io.element.android.libraries.textcomposer.mentions.ResolvedSuggestion
import io.element.android.libraries.textcomposer.model.MessageComposerMode
import io.element.android.libraries.textcomposer.model.Suggestion

sealed interface MessageComposerEvent {
    data object ToggleFullScreenState : MessageComposerEvent
    data object SendMessage : MessageComposerEvent
    data class SendUri(val uri: Uri) : MessageComposerEvent
    data object CloseSpecialMode : MessageComposerEvent
    data class SetMode(val composerMode: MessageComposerMode) : MessageComposerEvent
    data object AddAttachment : MessageComposerEvent
    data object DismissAttachmentMenu : MessageComposerEvent
    sealed interface PickAttachmentSource : MessageComposerEvent {
        data object FromGallery : PickAttachmentSource
        data object FromFiles : PickAttachmentSource
        data object PhotoFromCamera : PickAttachmentSource
        data object VideoFromCamera : PickAttachmentSource
        data object Location : PickAttachmentSource
        data object Poll : PickAttachmentSource
    }

    data class ToggleTextFormatting(val enabled: Boolean) : MessageComposerEvent
    data class Error(val error: Throwable) : MessageComposerEvent
    data class TypingNotice(val isTyping: Boolean) : MessageComposerEvent
    data class SuggestionReceived(val suggestion: Suggestion?) : MessageComposerEvent
    data class InsertSuggestion(val resolvedSuggestion: ResolvedSuggestion) : MessageComposerEvent
    data object SaveDraft : MessageComposerEvent
    data object ClearSlashError : MessageComposerEvent

    /** Arm ("Send later — <option>") or clear (null) the schedule applied to the next sent message. */
    data class ScheduleAt(val sendAt: Long?) : MessageComposerEvent
    data object ShowScheduledList : MessageComposerEvent
    data object DismissScheduledList : MessageComposerEvent

    /** POST action=send on the scheduled message and drop its local record. */
    data class SendScheduledNow(val delayId: String) : MessageComposerEvent

    /** POST action=cancel on the scheduled message and drop its local record. */
    data class CancelScheduled(val delayId: String) : MessageComposerEvent
}
