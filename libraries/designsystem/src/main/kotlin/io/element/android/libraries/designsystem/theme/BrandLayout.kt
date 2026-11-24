/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How the room list header is laid out (ADR 0016). */
enum class RoomListHeaderLayout {
    /** Upstream Element X top bar. */
    Stock,

    /** Z: solid accent bar under the status bar, search inside it, no large title. */
    ZAccentBar,

    /** L: plain canvas, bold title left, plain action icons, grey search pill below. */
    LPlain,
}

/**
 * The per-theme presentation knobs (ADR 0016). One instance per [BrandTheme], provided
 * through [LocalBrandLayout] from `ElementThemeApp`; screens read knobs from here instead
 * of scattering `when (theme)` checks. `Color?` knobs fall back to the stock rendering
 * when null, so [BrandLayout.Stock] is byte-for-byte the upstream look.
 */
@Immutable
data class BrandLayout(
    val roomListHeader: RoomListHeaderLayout,
    val chatTopBarAccent: Boolean,
    val chatBackground: Color?,
    val outgoingBubbleColor: Color?,
    val incomingBubbleColor: Color?,
    val outgoingTextColor: Color?,
    val bubbleRadius: Dp,
    val readReceiptLabel: Boolean,
) {
    companion object {
        /** The upstream rendering; every knob is neutral. */
        val Stock = BrandLayout(
            roomListHeader = RoomListHeaderLayout.Stock,
            chatTopBarAccent = false,
            chatBackground = null,
            outgoingBubbleColor = null,
            incomingBubbleColor = null,
            outgoingTextColor = null,
            bubbleRadius = 12.dp,
            readReceiptLabel = false,
        )

        /**
         * The layout for a brand theme. Light values follow the ADR table; dark mode keeps
         * the stock dark surfaces and only carries the brand bubble colours.
         */
        fun forTheme(theme: BrandTheme, isLight: Boolean): BrandLayout {
            return when (theme) {
                BrandTheme.Default -> Stock
                BrandTheme.Z -> if (isLight) {
                    BrandLayout(
                        roomListHeader = RoomListHeaderLayout.ZAccentBar,
                        chatTopBarAccent = true,
                        chatBackground = Color(0xFFE2E9F1),
                        outgoingBubbleColor = Color(0xFFE5EFFF),
                        incomingBubbleColor = Color(0xFFFFFFFF),
                        outgoingTextColor = Color(0xFF081C36),
                        bubbleRadius = 12.dp,
                        readReceiptLabel = false,
                    )
                } else {
                    BrandLayout(
                        roomListHeader = RoomListHeaderLayout.ZAccentBar,
                        chatTopBarAccent = false,
                        chatBackground = null,
                        outgoingBubbleColor = Color(0xFF14263D),
                        incomingBubbleColor = Color(0xFF1F2A36),
                        outgoingTextColor = null,
                        bubbleRadius = 12.dp,
                        readReceiptLabel = false,
                    )
                }
                BrandTheme.L -> if (isLight) {
                    BrandLayout(
                        roomListHeader = RoomListHeaderLayout.LPlain,
                        chatTopBarAccent = false,
                        chatBackground = Color(0xFF8CABD8),
                        outgoingBubbleColor = Color(0xFF85E249),
                        incomingBubbleColor = Color(0xFFFFFFFF),
                        outgoingTextColor = Color(0xFF1A1A1A),
                        bubbleRadius = 18.dp,
                        readReceiptLabel = true,
                    )
                } else {
                    BrandLayout(
                        roomListHeader = RoomListHeaderLayout.LPlain,
                        chatTopBarAccent = false,
                        chatBackground = null,
                        outgoingBubbleColor = Color(0xFF2F6F3E),
                        incomingBubbleColor = Color(0xFF222821),
                        outgoingTextColor = null,
                        bubbleRadius = 18.dp,
                        readReceiptLabel = true,
                    )
                }
            }
        }
    }
}

val LocalBrandLayout = staticCompositionLocalOf { BrandLayout.Stock }
