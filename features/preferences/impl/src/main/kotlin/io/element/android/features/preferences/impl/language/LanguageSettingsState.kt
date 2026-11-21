/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.preferences.impl.language

import io.element.android.features.preferences.impl.language.AppLanguage.English
import io.element.android.features.preferences.impl.language.AppLanguage.Japanese
import io.element.android.features.preferences.impl.language.AppLanguage.System
import io.element.android.features.preferences.impl.language.AppLanguage.Vietnamese

data class LanguageSettingsState(
    val selectedLanguage: AppLanguage,
    val eventSink: (LanguageSettingsEvent) -> Unit,
)

sealed interface AppLanguage {
    data object System : AppLanguage
    data object English : AppLanguage
    data object Japanese : AppLanguage
    data object Vietnamese : AppLanguage
}

val appLanguages = listOf(System, English, Japanese, Vietnamese)
