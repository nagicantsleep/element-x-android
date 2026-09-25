/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl

import androidx.compose.runtime.Composable
import io.element.android.features.startchat.impl.userlist.UserListDataStore
import io.element.android.features.startchat.impl.userlist.UserListPresenter
import io.element.android.features.startchat.impl.userlist.UserListPresenterArgs
import io.element.android.features.startchat.impl.userlist.UserListState
import io.element.android.features.startchat.impl.userlist.aUserListState
import io.element.android.libraries.usersearch.api.UserRepository

/**
 * Local test fixture for the Contacts tab: the upstream fake lives in startchat's own
 * `src/test` source set, which is not consumable from this module.
 */
class FakeUserListPresenterFactory(
    private var state: UserListState = aUserListState(),
) : UserListPresenter.Factory {
    override fun create(
        args: UserListPresenterArgs,
        userRepository: UserRepository,
        userListDataStore: UserListDataStore,
    ): UserListPresenter = FakeUserListPresenter(state)
}

private class FakeUserListPresenter(
    private val state: UserListState,
) : UserListPresenter {
    @Composable
    override fun present(): UserListState = state
}
