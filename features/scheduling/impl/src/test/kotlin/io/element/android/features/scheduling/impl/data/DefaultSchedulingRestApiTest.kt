/*
 * Copyright 2026 nagicantsleep
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.scheduling.impl.data

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.androidutils.json.DefaultJsonProvider
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.network.RetrofitFactory
import io.element.android.libraries.sessionstorage.test.InMemorySessionStore
import io.element.android.libraries.sessionstorage.test.aSessionData
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class DefaultSchedulingRestApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: DefaultSchedulingRestApi

    private val content = buildJsonObject {
        put("msgtype", JsonPrimitive("m.text"))
        put("body", JsonPrimitive("hello"))
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val sessionStore = InMemorySessionStore(listOf(aSessionData(sessionId = "@alice:server.org")))
        val matrixClient = FakeMatrixClient(
            sessionId = io.element.android.libraries.matrix.test.A_SESSION_ID,
            homeserverUrl = server.url("/").toString(),
        )
        api = DefaultSchedulingRestApi(
            matrixClient = matrixClient,
            sessionStore = sessionStore,
            retrofitFactory = RetrofitFactory(
                callFactory = { OkHttpClient() },
                json = { DefaultJsonProvider() },
            ),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sendDelayedEvent - PUTs to the send path with the delay query param`() = runTest {
        server.enqueue(MockResponse().setBody("""{"delay_id": "delay-9"}""").setHeader("Content-Type", "application/json"))

        val result = api.sendDelayedEvent(A_ROOM_ID, "m.room.message", content, delayMs = 60_000, txnId = "txn-1")

        assertThat(result.getOrNull()?.delayId).isEqualTo("delay-9")
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("PUT")
        assertThat(recorded.path)
            .isEqualTo("/rooms/${A_ROOM_ID.value}/send/m.room.message/txn-1?org.matrix.msc4140.delay=60000")
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer anAccessToken")
        assertThat(recorded.body.readUtf8()).contains("\"msgtype\":\"m.text\"")
    }

    @Test
    fun `updateDelayedEvent - POSTs the action body`() = runTest {
        server.enqueue(MockResponse().setBody("{}").setHeader("Content-Type", "application/json"))

        val result = api.updateDelayedEvent("delay-9", "cancel")

        assertThat(result.isSuccess).isTrue()
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/unstable/org.matrix.msc4140/delayed_events/delay-9")
        assertThat(recorded.body.readUtf8()).isEqualTo("""{"action":"cancel"}""")
    }

    @Test
    fun `serverSupportsScheduling - true when the unstable feature flag is on`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"versions": ["v1.11"], "unstable_features": {"org.matrix.msc4140": true}}""")
                .setHeader("Content-Type", "application/json")
        )

        val result = api.serverSupportsScheduling()

        assertThat(result.getOrNull()).isTrue()
        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/versions")
    }

    @Test
    fun `serverSupportsScheduling - false when the flag is missing`() = runTest {
        server.enqueue(MockResponse().setBody("""{"versions": ["v1.11"]}""").setHeader("Content-Type", "application/json"))

        val result = api.serverSupportsScheduling()

        assertThat(result.getOrNull()).isFalse()
    }

    @Test
    fun `errors - map HTTP failures to SchedulingRestException with the status code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"errcode": "M_UNRECOGNIZED"}"""))

        val result = api.updateDelayedEvent("delay-9", "send")

        val error = result.exceptionOrNull()
        assertThat(error).isInstanceOf(SchedulingRestException::class.java)
        assertThat((error as SchedulingRestException).statusCode).isEqualTo(404)
    }
}
