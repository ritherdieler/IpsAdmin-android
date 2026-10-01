package com.dscorp.ispadmin.observability

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

class ObservabilityHttpInterceptorTest {

    @Test
    fun `no lee workflow tags del client`() {
        val client = mockk<ObservabilityClient>(relaxed = true)
        every { client.currentSessionId() } returns "session-1"
        val tracer = mockk<ObservabilityTracer>(relaxed = true)
        every { tracer.newTraceId() } returns "trace"
        every { tracer.newSpanId() } returns "span"
        every { tracer.traceparent(any(), any()) } returns "00-trace-span-01"

        val interceptor = ObservabilityHttpInterceptor(
            clientProvider = lazyOf(client),
            tracerProvider = lazyOf(tracer)
        )
        val request = Request.Builder().url("http://localhost/api/login").get().build()
        val proceeded = slot<Request>()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(capture(proceeded)) } answers {
            Response.Builder()
                .request(firstArg())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ByteArray(0).toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }

        interceptor.intercept(chain)

        verify(exactly = 0) { client.currentWorkflowTags() }
        verify(exactly = 1) {
            tracer.recordClientSpan(
                traceId = "trace",
                spanId = "span",
                name = "GET /api/login",
                httpMethod = "GET",
                httpRoute = "/api/login",
                httpStatus = 200,
                startEpochMs = any(),
                durationMs = any(),
                status = "OK",
                sessionId = "session-1",
                tags = null
            )
        }
        assertThat(proceeded.captured.header("X-Obs-Session-Id")).isEqualTo("session-1")
        assertThat(proceeded.captured.header("X-Operation-Id")).isNull()
    }

    @Test
    fun `propaga X-Operation-Id del workflow activo`() {
        val client = mockk<ObservabilityClient>(relaxed = true)
        every { client.currentSessionId() } returns "session-1"
        every { client.currentWorkflowId() } returns "op-123"
        val tracer = mockk<ObservabilityTracer>(relaxed = true)
        every { tracer.newTraceId() } returns "trace"
        every { tracer.newSpanId() } returns "span"
        every { tracer.traceparent(any(), any()) } returns "00-trace-span-01"
        val interceptor = ObservabilityHttpInterceptor(lazyOf(client), lazyOf(tracer))
        val proceeded = slot<Request>()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns Request.Builder().url("http://localhost/subscription").get().build()
        every { chain.proceed(capture(proceeded)) } answers {
            Response.Builder().request(firstArg()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(ByteArray(0).toResponseBody("application/json".toMediaTypeOrNull())).build()
        }

        interceptor.intercept(chain)

        assertThat(proceeded.captured.header("X-Operation-Id")).isEqualTo("op-123")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ObservabilityTracerEnrichmentTest {

    @Test
    fun `enriquece spans con tags del provider`() {
        val queue = InMemoryEventStore()
        val gson = Gson()
        val tracer = ObservabilityTracer(
            api = mockk(relaxed = true),
            queue = queue,
            contextProvider = ObservabilityContextProvider(
                object : ObsAppInfo {
                    override fun environment() = "dev"
                    override fun release() = "1.0.0"
                    override fun versionName() = "1.0.0"
                    override fun versionCode() = 1
                    override fun flavor() = "dev"
                },
                ObsUserProvider { null }
            ),
            gson = gson,
            apiKey = "",
            tagsProvider = { mapOf(ObsWorkflowTags.ID to "wf-1", ObsWorkflowTags.NAME to "login") },
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher())
        )

        tracer.recordClientSpan(
            traceId = "t",
            spanId = "s",
            name = "GET /x",
            httpMethod = "GET",
            httpRoute = "/x",
            httpStatus = 200,
            startEpochMs = 1L,
            durationMs = 2L,
            status = "OK",
            sessionId = "sess"
        )

        val span = gson.fromJson(queue.readAll().single(), ObsSpanDto::class.java)
        @Suppress("UNCHECKED_CAST")
        val tags = gson.fromJson(span.tagsJson, Map::class.java) as Map<String, Any?>
        assertThat(tags["workflowId"]).isEqualTo("wf-1")
        assertThat(tags["workflowName"]).isEqualTo("login")
    }
}
