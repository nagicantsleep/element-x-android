/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.friends.impl

import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.features.friends.api.FriendsEntryPoint
import io.element.android.features.friends.impl.root.FriendsNode
import io.element.android.libraries.architecture.createNode

@ContributesBinding(AppScope::class)
class DefaultFriendsEntryPoint : FriendsEntryPoint {
    override fun createNode(
        parentNode: Node,
        buildContext: BuildContext,
    ): Node {
        return parentNode.createNode<FriendsNode>(buildContext)
    }
}
