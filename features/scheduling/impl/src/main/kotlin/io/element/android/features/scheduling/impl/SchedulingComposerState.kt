/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import io.element.android.features.scheduling.impl.data.ScheduledMessageRecord

/** Slice of [io.element.android.features.messages.impl.messagecomposer.MessageComposerState] owned by the scheduling feature. */
data class SchedulingComposerState(
    /** Server advertises `org.matrix.msc4140`; gates the whole affordance. */
    val isSupported: Boolean = false,
    /** Epoch ms chosen for the next message, or null when no schedule is armed. */
    val pendingSendAt: Long? = null,
    /** Records scheduled for the current room, sorted by send time. */
    val scheduledMessages: ImmutableList<ScheduledMessageRecord> = persistentListOf(),
    val showScheduledList: Boolean = false,
)
