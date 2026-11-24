/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BrandThemeTest {
    @Test
    fun `fromStorage keeps existing explicit values`() {
        assertThat(BrandTheme.fromStorage("default")).isEqualTo(BrandTheme.Default)
        assertThat(BrandTheme.fromStorage("z")).isEqualTo(BrandTheme.Z)
        assertThat(BrandTheme.fromStorage("l")).isEqualTo(BrandTheme.L)
        assertThat(BrandTheme.fromStorage(null)).isEqualTo(BrandTheme.Default)
        assertThat(BrandTheme.fromStorage("unknown")).isEqualTo(BrandTheme.Default)
    }

    @Test
    fun `resolve - explicit choice wins over the language`() {
        assertThat(BrandTheme.resolve("default", "vi")).isEqualTo(BrandTheme.Default)
        assertThat(BrandTheme.resolve("z", "en")).isEqualTo(BrandTheme.Z)
        assertThat(BrandTheme.resolve("l", "vi")).isEqualTo(BrandTheme.L)
    }

    @Test
    fun `resolve - no stored choice follows the language`() {
        assertThat(BrandTheme.resolve(null, "vi")).isEqualTo(BrandTheme.Z)
        assertThat(BrandTheme.resolve(null, "ja")).isEqualTo(BrandTheme.L)
        assertThat(BrandTheme.resolve(null, "en")).isEqualTo(BrandTheme.Default)
        assertThat(BrandTheme.resolve(null, "fr")).isEqualTo(BrandTheme.Default)
    }

    @Test
    fun `resolve - language tag region and case are ignored`() {
        assertThat(BrandTheme.resolve(null, "vi-VN")).isEqualTo(BrandTheme.Z)
        assertThat(BrandTheme.resolve(null, "JA")).isEqualTo(BrandTheme.L)
    }

    @Test
    fun `resolve - null language falls back to Default`() {
        assertThat(BrandTheme.resolve(null, null)).isEqualTo(BrandTheme.Default)
    }
}
