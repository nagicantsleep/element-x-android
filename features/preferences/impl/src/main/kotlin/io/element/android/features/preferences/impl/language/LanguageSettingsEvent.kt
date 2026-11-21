/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.preferences.impl.language

sealed interface LanguageSettingsEvent {
    data class SetLanguage(val language: AppLanguage) : LanguageSettingsEvent
}
