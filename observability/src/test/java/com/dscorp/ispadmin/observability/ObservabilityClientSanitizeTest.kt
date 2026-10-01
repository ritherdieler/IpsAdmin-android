package com.dscorp.ispadmin.observability

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObservabilityClientSanitizeTest {

    private val store = InMemoryEventStore()
    private val gson = Gson()
    private val appInfo = object : ObsAppInfo {
        override fun environment() = "prod"
        override fun release() = "1.0.0 (1)"
        override fun versionName() = "1.0.0"
        override fun versionCode() = 1
        override fun flavor() = "prod"
    }
    private val client = ObservabilityClient(
        api = mockk(relaxed = true),
        queue = store,
        contextProvider = ObservabilityContextProvider(
            appInfo,
            ObsUserProvider { mapOf("id" to 1, "phone" to "987654321") }
        ),
        gson = gson,
        apiKey = "",
        workScheduler = ObservabilityFlushScheduler { },
        config = ObservabilityConfig(apiKey = "", sanitizePayloads = true),
        coroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    )

    @Test
    fun `tags breadcrumbs and user are sanitized before queueing`() {
        client.addBreadcrumb("state", "register_click", mapOf("dni" to "12345678", "planId" to "p1"))

        client.reportLog("registro", tags = mapOf("wifiPassword24" to "clave-real", "screen" to "register"))

        val raw = store.readAll().single()
        assertThat(raw).doesNotContain("12345678")
        assertThat(raw).doesNotContain("clave-real")
        assertThat(raw).doesNotContain("987654321")
        assertThat(raw).contains("register_click")
        assertThat(raw).contains("\"screen\":\"register\"")
    }
}
