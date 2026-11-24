/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * The effective app language tag used to derive the default brand theme (ADR 0016):
 * the per-app locale set through the system [LocaleManager] when present,
 * otherwise the system locale the app resolves its resources with.
 *
 * Returns a bare language tag (`vi`, `ja`, `en`, …) or null when unknown.
 */
@Composable
@ReadOnlyComposable
fun effectiveAppLanguageTag(): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val locales = LocalContext.current.getSystemService(LocaleManager::class.java)?.applicationLocales
        locales?.get(0)?.language?.let { return it }
    }
    return LocalConfiguration.current.locales.get(0)?.language
}
