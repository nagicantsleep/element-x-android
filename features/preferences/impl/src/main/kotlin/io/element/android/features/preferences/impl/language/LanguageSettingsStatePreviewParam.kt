/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.preferences.impl.language

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

open class LanguageSettingsStatePreviewParam : PreviewParameterProvider<LanguageSettingsState> {
    override val values: Sequence<LanguageSettingsState>
        get() = sequenceOf(
            aLanguageSettingsState(),
            aLanguageSettingsState(selectedLanguage = AppLanguage.Japanese),
        )
}

fun aLanguageSettingsState(
    selectedLanguage: AppLanguage = AppLanguage.System,
) = LanguageSettingsState(
    selectedLanguage = selectedLanguage,
    eventSink = {},
)
