/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.R
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.preferences.PreferenceRow
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.matrix.ui.model.getBestName
import io.element.android.libraries.ui.strings.CommonStrings

/**
 * Z/L layout: the Profile tab. Shows the current user's identity, the entry point to
 * "Edit profile" and the entry point to Settings.
 *
 * This is the single place where the signed-in identity is displayed. The Settings screen
 * no longer repeats it (Z/L convention: identity lives in the Profile tab).
 */
@Composable
fun HomeProfileView(
    currentUser: MatrixUser,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(
                avatarData = currentUser.getAvatarData(AvatarSize.UserHeader),
                avatarType = AvatarType.User,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = currentUser.getBestName(),
                style = ElementTheme.typography.fontHeadingLgBold,
                color = ElementTheme.colors.textPrimary,
            )
            Text(
                text = currentUser.userId.value,
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PreferenceRow(modifier = Modifier.clickable(onClick = onEditProfileClick)) {
                ProfileActionRowContent(
                    label = stringResource(R.string.screen_home_tab_edit_profile),
                    icon = CompoundIcons.Edit(),
                )
            }
            PreferenceRow(modifier = Modifier.clickable(onClick = onSettingsClick)) {
                ProfileActionRowContent(
                    label = stringResource(CommonStrings.common_settings),
                    icon = CompoundIcons.Settings(),
                )
            }
        }
    }
}

@Composable
private fun RowScope.ProfileActionRowContent(
    label: String,
    icon: ImageVector,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = ElementTheme.colors.iconSecondary,
    )
    Spacer(Modifier.size(12.dp))
    Text(
        text = label,
        style = ElementTheme.typography.fontBodyLgRegular,
        color = ElementTheme.colors.textPrimary,
        modifier = Modifier.weight(1f),
    )
    Icon(
        imageVector = CompoundIcons.ChevronRight(),
        contentDescription = null,
        tint = ElementTheme.colors.iconQuaternary,
    )
}
