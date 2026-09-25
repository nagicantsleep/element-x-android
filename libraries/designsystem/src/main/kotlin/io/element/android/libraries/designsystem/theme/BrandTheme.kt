/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme

import androidx.compose.ui.graphics.Color
import io.element.android.compound.colors.SemanticColorsLightDark

/**
 * Runtime brand theme applied on top of the Compound semantic colors.
 *
 * The overrides below are kept in this single file so they are easy to audit. They mirror the
 * values defined in `config/themes/z.json` and `config/themes/l.json`; keep the two in step.
 *
 * Light and dark are handled deliberately differently. A brand palette describes a *light*
 * surface, so applying its canvas/text values to the dark variant would force a white background
 * and dark text onto dark mode. The dark variant therefore only overrides the accent family and
 * keeps the stock dark surfaces and text.
 */
enum class BrandTheme(val storageValue: String) {
    Default("default"),
    Z("z"),
    L("l");

    companion object {
        fun fromStorage(value: String?): BrandTheme {
            return entries.firstOrNull { it.storageValue == value } ?: Default
        }
    }
}

/**
 * Returns the [SemanticColorsLightDark] to use for this [BrandTheme].
 *
 * [BrandTheme.Default] returns [base] unchanged, so the stock rendering is byte-for-byte identical.
 */
fun BrandTheme.semanticColors(base: SemanticColorsLightDark): SemanticColorsLightDark {
    return when (this) {
        BrandTheme.Default -> base
        BrandTheme.Z -> SemanticColorsLightDark(
            light = base.light.copy(
                // surfaces and text tuned for the Z theme light UI
                textPrimary = Color(0xFF081C36),
                textSecondary = Color(0xFF7589A3),
                bgCanvasDefault = Color(0xFFFFFFFF),
                bgSubtlePrimary = Color(0xFFF7F9FC),
                bgSubtleSecondary = Color(0xFFF2F4F7),
                separatorPrimary = Color(0xFFE5EAF0),
                // brand accent family
                bgAccentRest = Color(0xFF0068FF),
                bgAccentSelected = Color(0xFFE6F0FF),
                bgActionPrimaryRest = Color(0xFF0068FF),
                bgActionPrimaryHovered = Color(0xFF0057D6),
                bgActionPrimaryPressed = Color(0xFF0046AD),
                bgActionSecondaryRest = Color(0xFFE6F0FF),
                bgActionSecondaryHovered = Color(0xFFD6E6FF),
                textActionAccent = Color(0xFF0068FF),
                iconAccentPrimary = Color(0xFF0068FF),
                iconAccentTertiary = Color(0xFF0068FF),
                borderInteractivePrimary = Color(0xFF0068FF),
                borderFocused = Color(0xFF0068FF),
                // status colors stay semantically meaningful, not brand-colored
                bgCriticalPrimary = Color(0xFFE5484D),
                bgCriticalHovered = Color(0xFFC93B40),
                bgSuccessSubtle = Color(0xFFE7F7EF),
            ),
            dark = base.dark.copy(
                // dark surfaces/text for this brand, not the light values
                textPrimary = Color(0xFFE9EDF2),
                textSecondary = Color(0xFF8A9BA8),
                bgCanvasDefault = Color(0xFF0E1621),
                bgSubtlePrimary = Color(0xFF17212B),
                bgSubtleSecondary = Color(0xFF1F2A36),
                separatorPrimary = Color(0xFF2A3947),
                bgActionSecondaryRest = Color(0xFF1C2A3A),
                bgActionSecondaryHovered = Color(0xFF24374B),
                // accent, brightened for contrast on dark surfaces
                bgAccentRest = Color(0xFF0068FF),
                bgAccentSelected = Color(0xFF14263D),
                bgActionPrimaryRest = Color(0xFF0068FF),
                bgActionPrimaryHovered = Color(0xFF0057D6),
                bgActionPrimaryPressed = Color(0xFF0046AD),
                textActionAccent = Color(0xFF66A5FF),
                iconAccentPrimary = Color(0xFF4D94FF),
                iconAccentTertiary = Color(0xFF66A5FF),
                borderInteractivePrimary = Color(0xFF4D94FF),
                borderFocused = Color(0xFF4D94FF),
                bgCriticalPrimary = Color(0xFFE5484D),
                bgCriticalHovered = Color(0xFFC93B40),
                bgSuccessSubtle = Color(0xFF10321F),
            ),
        )
        BrandTheme.L -> SemanticColorsLightDark(
            light = base.light.copy(
                // surfaces and text tuned for the L theme light UI
                textPrimary = Color(0xFF1A1A1A),
                textSecondary = Color(0xFF6E6E6E),
                bgCanvasDefault = Color(0xFFFFFFFF),
                bgSubtlePrimary = Color(0xFFF5F6F7),
                bgSubtleSecondary = Color(0xFFEBEDEF),
                separatorPrimary = Color(0xFFE6E6E6),
                // brand accent family
                bgAccentRest = Color(0xFF06C755),
                bgAccentSelected = Color(0xFFE6F9EE),
                bgActionPrimaryRest = Color(0xFF06C755),
                bgActionPrimaryHovered = Color(0xFF05A948),
                bgActionPrimaryPressed = Color(0xFF048A3B),
                bgActionSecondaryRest = Color(0xFFE6F9EE),
                bgActionSecondaryHovered = Color(0xFFD3F3E0),
                textActionAccent = Color(0xFF04A445),
                iconAccentPrimary = Color(0xFF06C755),
                iconAccentTertiary = Color(0xFF04A445),
                borderInteractivePrimary = Color(0xFF06C755),
                borderFocused = Color(0xFF06C755),
                // status colors stay semantically meaningful, not brand-colored
                bgCriticalPrimary = Color(0xFFFA4B42),
                bgCriticalHovered = Color(0xFFD93E36),
                bgSuccessSubtle = Color(0xFFE6F9EE),
            ),
            dark = base.dark.copy(
                // dark surfaces/text for this brand, not the light values
                textPrimary = Color(0xFFE9EDF2),
                textSecondary = Color(0xFF8A9BA8),
                bgCanvasDefault = Color(0xFF10140F),
                bgSubtlePrimary = Color(0xFF191E18),
                bgSubtleSecondary = Color(0xFF222821),
                separatorPrimary = Color(0xFF2C332B),
                bgActionSecondaryRest = Color(0xFF1C2A22),
                bgActionSecondaryHovered = Color(0xFF24352B),
                // accent, brightened for contrast on dark surfaces
                bgAccentRest = Color(0xFF06C755),
                bgAccentSelected = Color(0xFF12301F),
                bgActionPrimaryRest = Color(0xFF06C755),
                bgActionPrimaryHovered = Color(0xFF05A948),
                bgActionPrimaryPressed = Color(0xFF048A3B),
                textActionAccent = Color(0xFF33D377),
                iconAccentPrimary = Color(0xFF06C755),
                iconAccentTertiary = Color(0xFF33D377),
                borderInteractivePrimary = Color(0xFF06C755),
                borderFocused = Color(0xFF06C755),
                bgCriticalPrimary = Color(0xFFFA4B42),
                bgCriticalHovered = Color(0xFFD93E36),
                bgSuccessSubtle = Color(0xFF10321F),
            ),
        )
    }
}
