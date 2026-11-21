/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.preferences.impl.language

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.di.annotations.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Inject
class LanguageSettingsPresenter(
    @ApplicationContext private val context: Context,
) : Presenter<LanguageSettingsState> {
    @Composable
    override fun present(): LanguageSettingsState {
        var selectedLanguage by remember { mutableStateOf(currentLanguage()) }
        val coroutineScope = rememberCoroutineScope()
        return LanguageSettingsState(
            selectedLanguage = selectedLanguage,
            eventSink = { event ->
                when (event) {
                    is LanguageSettingsEvent.SetLanguage -> coroutineScope.launch {
                        selectedLanguage = event.language
                        applyLanguage(event.language)
                    }
                }
            },
        )
    }

    // Per-app locales are a system-managed setting on API 33+ (the app is gated
    // to it, see PreferencesRootNode); below that the picker is not reachable.
    private fun currentLanguage(): AppLanguage {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return AppLanguage.System
        }
        return when (context.getSystemService(LocaleManager::class.java)?.applicationLocales?.get(0)?.language) {
            "en" -> AppLanguage.English
            "ja" -> AppLanguage.Japanese
            "vi" -> AppLanguage.Vietnamese
            else -> AppLanguage.System
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val locales = when (language) {
            AppLanguage.System -> LocaleList.getEmptyLocaleList()
            AppLanguage.English -> LocaleList.forLanguageTags("en")
            AppLanguage.Japanese -> LocaleList.forLanguageTags("ja")
            AppLanguage.Vietnamese -> LocaleList.forLanguageTags("vi")
        }
        // The system persists per-app locales on API 33+, no app-side storage needed.
        context.getSystemService(LocaleManager::class.java)?.setApplicationLocales(locales)
    }
}
