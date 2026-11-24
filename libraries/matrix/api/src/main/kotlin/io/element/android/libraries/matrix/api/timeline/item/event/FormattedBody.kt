/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.timeline.item.event

data class FormattedBody(
    val format: MessageFormat,
    val body: String,
    /**
     * Raw format string when [format] is [MessageFormat.UNKNOWN] (e.g. `io.nagi.friend_request`),
     * null otherwise. The FFI enum variant carries the unknown name; this keeps it detectable.
     */
    val rawFormat: String? = null,
)
