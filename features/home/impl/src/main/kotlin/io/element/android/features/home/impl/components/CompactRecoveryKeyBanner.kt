/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

/**
 * Compact one-row key-backup banner (ADR 0016): icon, title, dismiss. Tapping the row
 * performs the existing "set up / confirm recovery key" action; the dismiss button keeps
 * the existing dismiss behaviour. Security behaviour is unchanged.
 */
@Composable
internal fun CompactRecoveryKeyBanner(
    title: String,
    onContinueClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .roomListBannerPadding()
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElementTheme.colors.bgSubtleSecondary)
            .clickable(onClick = onContinueClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = CompoundIcons.Key(),
            contentDescription = null,
            tint = ElementTheme.colors.iconAccentPrimary,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyMdMedium,
            color = ElementTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            modifier = Modifier
                .clickable(onClick = onDismissClick)
                .padding(4.dp),
            imageVector = CompoundIcons.Close(),
            contentDescription = stringResource(CommonStrings.action_close),
            tint = ElementTheme.colors.iconSecondary,
        )
    }
}

@Composable
internal fun SetUpRecoveryKeyBannerCompact(
    onContinueClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompactRecoveryKeyBanner(
        title = stringResource(CommonStrings.banner_recovery_compact_set_up),
        onContinueClick = onContinueClick,
        onDismissClick = onDismissClick,
        modifier = modifier,
    )
}

@Composable
internal fun ConfirmRecoveryKeyBannerCompact(
    onContinueClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompactRecoveryKeyBanner(
        title = stringResource(CommonStrings.banner_recovery_compact_confirm),
        onContinueClick = onContinueClick,
        onDismissClick = onDismissClick,
        modifier = modifier,
    )
}

@PreviewsDayNight
@Composable
internal fun CompactRecoveryKeyBannerPreview() = ElementPreview {
    SetUpRecoveryKeyBannerCompact(
        onContinueClick = {},
        onDismissClick = {},
    )
}
