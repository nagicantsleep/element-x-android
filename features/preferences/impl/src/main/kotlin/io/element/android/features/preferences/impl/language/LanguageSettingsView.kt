/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.preferences.impl.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.designsystem.components.dialogs.ListDialog
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Text

@Composable
fun LanguageSettingsView(
    state: LanguageSettingsState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displaySelectorDialog by remember { mutableStateOf(false) }

    PreferencePage(
        modifier = modifier,
        onBackClick = onBackClick,
        title = stringResource(id = R.string.screen_language_settings_title),
    ) {
        ListItem(
            content = {
                Text(text = stringResource(id = R.string.screen_language_settings_app_language))
            },
            supportingContent = {
                Text(text = state.selectedLanguage.displayText())
            },
            onClick = { displaySelectorDialog = true },
        )

        if (displaySelectorDialog) {
            LanguageSelectorDialog(
                selectedLanguage = state.selectedLanguage,
                onSelect = { language ->
                    displaySelectorDialog = false
                    state.eventSink(LanguageSettingsEvent.SetLanguage(language))
                },
                onDismiss = { displaySelectorDialog = false },
            )
        }
    }
}

@Composable
private fun LanguageSelectorDialog(
    selectedLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    ListDialog(
        title = stringResource(R.string.screen_language_settings_app_language),
        onSubmit = onDismiss,
        onDismissRequest = onDismiss,
        applyPaddingToContents = false,
    ) {
        for (language in appLanguages) {
            item(key = language, contentType = language) {
                ListItem(
                    content = {
                        Text(text = language.displayText())
                    },
                    leadingContent = ListItemContent.RadioButton(
                        selected = language == selectedLanguage,
                    ),
                    onClick = { onSelect(language) },
                )
            }
        }
    }
}

@Composable
private fun AppLanguage.displayText(): String = when (this) {
    AppLanguage.System -> stringResource(R.string.screen_language_settings_system_default)
    AppLanguage.English -> stringResource(R.string.screen_language_settings_english)
    AppLanguage.Japanese -> stringResource(R.string.screen_language_settings_japanese)
    AppLanguage.Vietnamese -> stringResource(R.string.screen_language_settings_vietnamese)
}

@PreviewsDayNight
@Composable
internal fun LanguageSettingsViewPreview(@PreviewParameter(LanguageSettingsStatePreviewParam::class) state: LanguageSettingsState) =
    ElementPreview {
        LanguageSettingsView(
            state = state,
            onBackClick = {},
        )
    }
