/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.scheduling.impl.R
import io.element.android.features.scheduling.impl.data.ScheduledMessageRecord
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.theme.components.DropdownMenu
import io.element.android.libraries.designsystem.theme.components.DropdownMenuItem
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Composer "Send later" dropdown: presets, custom time picker and the per-room scheduled list
 * entry, mirroring web's SchedulingMenu. Callers host it in a popup anchor and own dismissing.
 */
@Composable
fun SendLaterMenu(
    scheduledCount: Int,
    onScheduleAt: (Long) -> Unit,
    onShowScheduledList: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        SendLaterOptionList(
            onPickOption = { option ->
                onScheduleAt(option.sendAt())
            },
            onPickCustomTime = { showTimePicker = true },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.screen_scheduling_list_title)) },
            leadingIcon = { Icon(contentDescription = null, imageVector = CompoundIcons.Time()) },
            trailingIcon = { Text(scheduledCount.toString()) },
            onClick = {
                onDismiss()
                onShowScheduledList()
            },
        )
    }

    if (showTimePicker) {
        ScheduledTimePickerDialog(
            onConfirm = { sendAt ->
                showTimePicker = false
                onScheduleAt(sendAt)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

/**
 * Dropdown list of "Send later" options, mirroring web's SchedulingMenu semantics:
 * fixed presets plus a custom time picker. Pure list rendering; callers host it in a
 * popup and own dismissing.
 */
@Composable
fun SendLaterOptionList(
    onPickOption: (SendLaterOption) -> Unit,
    onPickCustomTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        listOf(SendLaterOption.OneHour, SendLaterOption.EightHours, SendLaterOption.TwentyFourHours).forEach { option ->
            ListItem(
                leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Time())),
                onClick = { onPickOption(option) },
                content = { Text(stringResource(option.labelRes())) },
            )
        }
        ListItem(
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Time())),
            onClick = onPickCustomTime,
            content = { Text(stringResource(R.string.screen_scheduling_option_custom)) },
        )
    }
}

internal fun SendLaterOption.labelRes(): Int = when (this) {
    SendLaterOption.OneHour -> R.string.screen_scheduling_option_in_1_hour
    SendLaterOption.EightHours -> R.string.screen_scheduling_option_in_8_hours
    SendLaterOption.TwentyFourHours -> R.string.screen_scheduling_option_in_24_hours
    SendLaterOption.Custom -> R.string.screen_scheduling_option_custom
}

/**
 * Time picker dialog for the Custom option. Resolves to an epoch-ms instant; a same-day
 * time that already passed rolls over to tomorrow (web's "tomorrow morning" behaviour).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledTimePickerDialog(
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = remember { java.time.LocalTime.now().plusMinutes(1) }
    val timePickerState = rememberTimePickerState(
        initialHour = now.hour,
        initialMinute = now.minute,
        is24Hour = true,
    )
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = modifier) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.screen_scheduling_send_later)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    text = stringResource(R.string.screen_scheduling_custom_time_confirm),
                    size = ButtonSize.Medium,
                    onClick = {
                        val picked = java.time.LocalDate.now().atTime(timePickerState.hour, timePickerState.minute)
                        val sendAt = picked.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val resolved = if (sendAt <= System.currentTimeMillis()) {
                            // Same-day time already passed: schedule for tomorrow.
                            sendAt + 24 * 60 * 60_000L
                        } else {
                            sendAt
                        }
                        onConfirm(resolved)
                    },
                )
            },
            dismissButton = {
                TextButton(
                    text = stringResource(R.string.screen_scheduling_custom_time_cancel),
                    size = ButtonSize.Medium,
                    onClick = onDismiss,
                )
            },
        )
    }
}

/** Clock affordance shown above the composer when scheduling is supported; shows the armed time when set. */
@Composable
fun ScheduleComposerButton(
    pendingSendAt: Long?,
    onOpenMenu: () -> Unit,
    onClearSchedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (pendingSendAt != null) {
            val sendAtText = remember(pendingSendAt) {
                formatter.format(Instant.ofEpochMilli(pendingSendAt).atZone(ZoneId.systemDefault()))
            }
            Text(
                text = stringResource(R.string.screen_scheduling_scheduled_for, sendAtText),
                style = ElementTheme.typography.fontBodyMdRegular,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClearSchedule) {
                Icon(
                    imageVector = CompoundIcons.Close(),
                    contentDescription = stringResource(R.string.screen_scheduling_action_cancel),
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        IconButton(onClick = onOpenMenu) {
            Icon(
                imageVector = CompoundIcons.Time(),
                contentDescription = stringResource(R.string.screen_scheduling_send_later),
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun ScheduleComposerButtonPreview() = ElementPreview {
    Column {
        ScheduleComposerButton(pendingSendAt = null, onOpenMenu = {}, onClearSchedule = {})
        ScheduleComposerButton(pendingSendAt = System.currentTimeMillis() + 60_000, onOpenMenu = {}, onClearSchedule = {})
    }
}

/** Per-room sheet listing scheduled messages, one row with send-now and cancel actions each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledMessagesSheet(
    scheduledMessages: List<ScheduledMessageRecord>,
    onSendNow: (String) -> Unit,
    onCancel: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        scrollable = true,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            if (scheduledMessages.isEmpty()) {
                ListItem(content = { Text(stringResource(R.string.screen_scheduling_empty)) })
            }
            scheduledMessages.forEach { record ->
                ScheduledMessageRow(
                    record = record,
                    onSendNow = { onSendNow(record.delayId) },
                    onCancel = { onCancel(record.delayId) },
                )
            }
        }
    }
}

@Composable
private fun ScheduledMessageRow(
    record: ScheduledMessageRecord,
    onSendNow: () -> Unit,
    onCancel: () -> Unit,
) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
    val sendAtText = remember(record.sendAt) {
        formatter.format(Instant.ofEpochMilli(record.sendAt).atZone(ZoneId.systemDefault()))
    }
    ListItem(
        content = {
            Text(record.content["body"]?.toString()?.trim('"').orEmpty())
        },
        supportingContent = { Text(stringResource(R.string.screen_scheduling_scheduled_for, sendAtText)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Time())),
    )
    ListItem(
        content = { Text(stringResource(R.string.screen_scheduling_action_send_now)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Send())),
        onClick = onSendNow,
    )
    ListItem(
        content = { Text(stringResource(R.string.screen_scheduling_action_cancel)) },
        leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Delete())),
        onClick = onCancel,
    )
}
