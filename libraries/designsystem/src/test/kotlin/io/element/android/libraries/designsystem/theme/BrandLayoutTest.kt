/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BrandLayoutTest {
    @Test
    fun `default theme keeps the stock rendering`() {
        for (isLight in listOf(true, false)) {
            val layout = BrandLayout.forTheme(BrandTheme.Default, isLight)
            assertThat(layout).isEqualTo(BrandLayout.Stock)
            assertThat(layout.roomListHeader).isEqualTo(RoomListHeaderLayout.Stock)
            assertThat(layout.chatBackground).isNull()
            assertThat(layout.outgoingBubbleColor).isNull()
            assertThat(layout.incomingBubbleColor).isNull()
            assertThat(layout.outgoingTextColor).isNull()
            assertThat(layout.bubbleRadius).isEqualTo(12.dp)
            assertThat(layout.readReceiptLabel).isFalse()
            assertThat(layout.chatTopBarAccent).isFalse()
        }
    }

    @Test
    fun `Z light layout matches the ADR values`() {
        val layout = BrandLayout.forTheme(BrandTheme.Z, isLight = true)
        assertThat(layout.roomListHeader).isEqualTo(RoomListHeaderLayout.ZAccentBar)
        assertThat(layout.chatTopBarAccent).isTrue()
        assertThat(layout.chatBackground).isEqualTo(Color(0xFFE2E9F1))
        assertThat(layout.outgoingBubbleColor).isEqualTo(Color(0xFFE5EFFF))
        assertThat(layout.incomingBubbleColor).isEqualTo(Color(0xFFFFFFFF))
        assertThat(layout.outgoingTextColor).isEqualTo(Color(0xFF081C36))
        assertThat(layout.bubbleRadius).isEqualTo(12.dp)
        assertThat(layout.readReceiptLabel).isFalse()
    }

    @Test
    fun `Z dark layout never paints the light slab`() {
        val layout = BrandLayout.forTheme(BrandTheme.Z, isLight = false)
        assertThat(layout.chatTopBarAccent).isFalse()
        assertThat(layout.chatBackground).isNull()
        assertThat(layout.outgoingBubbleColor).isEqualTo(Color(0xFF14263D))
        assertThat(layout.incomingBubbleColor).isEqualTo(Color(0xFF1F2A36))
        assertThat(layout.bubbleRadius).isEqualTo(12.dp)
    }

    @Test
    fun `L light layout matches the ADR values`() {
        val layout = BrandLayout.forTheme(BrandTheme.L, isLight = true)
        assertThat(layout.roomListHeader).isEqualTo(RoomListHeaderLayout.LPlain)
        assertThat(layout.chatTopBarAccent).isFalse()
        assertThat(layout.chatBackground).isEqualTo(Color(0xFF8CABD8))
        assertThat(layout.outgoingBubbleColor).isEqualTo(Color(0xFF85E249))
        assertThat(layout.incomingBubbleColor).isEqualTo(Color(0xFFFFFFFF))
        assertThat(layout.outgoingTextColor).isEqualTo(Color(0xFF1A1A1A))
        assertThat(layout.bubbleRadius).isEqualTo(18.dp)
        assertThat(layout.readReceiptLabel).isTrue()
    }

    @Test
    fun `L dark layout uses the ADR dark bubbles`() {
        val layout = BrandLayout.forTheme(BrandTheme.L, isLight = false)
        assertThat(layout.chatBackground).isNull()
        assertThat(layout.outgoingBubbleColor).isEqualTo(Color(0xFF2F6F3E))
        assertThat(layout.incomingBubbleColor).isEqualTo(Color(0xFF222821))
        assertThat(layout.bubbleRadius).isEqualTo(18.dp)
        assertThat(layout.readReceiptLabel).isTrue()
    }
}
