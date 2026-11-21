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
import androidx.test.core.app.ApplicationProvider
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.WarmUpRule
import io.element.android.tests.testutils.test
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLocaleManager
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowLocaleManager::class])
class LanguageSettingsPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun localeManager(): LocaleManager =
        context().getSystemService(LocaleManager::class.java)

    @Test
    fun `present - maps stored application locales to the matching language`() = runTest {
        LanguageSettingsPresenter(context()).test {
            assertThat(awaitItem().selectedLanguage).isEqualTo(AppLanguage.System)
        }

        listOf(
            "en" to AppLanguage.English,
            "ja" to AppLanguage.Japanese,
            "vi" to AppLanguage.Vietnamese,
        ).forEach { (tag, expected) ->
            localeManager().setApplicationLocales(LocaleList.forLanguageTags(tag))
            LanguageSettingsPresenter(context()).test {
                assertThat(awaitItem().selectedLanguage).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `present - unknown locale falls back to System`() = runTest {
        localeManager().setApplicationLocales(LocaleList.forLanguageTags("fr"))
        LanguageSettingsPresenter(context()).test {
            assertThat(awaitItem().selectedLanguage).isEqualTo(AppLanguage.System)
        }
    }

    @Test
    fun `present - SetLanguage applies locales to the system LocaleManager`() = runTest {
        LanguageSettingsPresenter(context()).test {
            val initialState = awaitItem()
            assertThat(initialState.selectedLanguage).isEqualTo(AppLanguage.System)
            initialState.eventSink(LanguageSettingsEvent.SetLanguage(AppLanguage.Japanese))
            assertThat(awaitItem().selectedLanguage).isEqualTo(AppLanguage.Japanese)
        }
        assertThat(localeManager().applicationLocales.toLanguageTags()).isEqualTo("ja")
    }

    @Test
    fun `present - SetLanguage to System resets to the empty locale list`() = runTest {
        localeManager().setApplicationLocales(LocaleList.forLanguageTags("ja"))
        LanguageSettingsPresenter(context()).test {
            val initialState = awaitItem()
            assertThat(initialState.selectedLanguage).isEqualTo(AppLanguage.Japanese)
            initialState.eventSink(LanguageSettingsEvent.SetLanguage(AppLanguage.System))
            assertThat(awaitItem().selectedLanguage).isEqualTo(AppLanguage.System)
        }
        assertThat(localeManager().applicationLocales.isEmpty).isTrue()
    }

    @Test
    fun `present - below API 33 the presenter reads System and never writes`() = runTest {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.S)
        try {
            localeManager().setApplicationLocales(LocaleList.forLanguageTags("vi"))
            LanguageSettingsPresenter(context()).test {
                // The <33 guard short-circuits: the stored vi locale is not reported.
                val initialState = awaitItem()
                assertThat(initialState.selectedLanguage).isEqualTo(AppLanguage.System)
                // An event must be a no-op: the stored locales stay untouched.
                initialState.eventSink(LanguageSettingsEvent.SetLanguage(AppLanguage.Japanese))
                assertThat(awaitItem().selectedLanguage).isEqualTo(AppLanguage.Japanese)
            }
            assertThat(localeManager().applicationLocales.toLanguageTags()).isEqualTo("vi")
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.TIRAMISU)
        }
    }
}
