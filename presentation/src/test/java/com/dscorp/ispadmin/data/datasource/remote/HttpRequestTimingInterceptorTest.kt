package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.data.datasource.remote.logging.HttpRequestTimingInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpRequestTimingInterceptorTest {

    @Test
    fun logsRequestTimingAndSanitizedEndpointWithoutHeadersOrBodies() {
        val server = MockWebServer()
        val messages = mutableListOf<String>()
        server.enqueue(MockResponse().setResponseCode(201).setBody("response-secret"))
        server.start()

        try {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpRequestTimingInterceptor(messages::add))
                .build()
            val request = Request.Builder()
                .url(server.url("api/subscriptions/99881/onu/485754439F4BF950?access_token=query-secret"))
                .header("Authorization", "Bearer header-secret")
                .post("""{"password":"request-secret"}""".toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                assertTrue(response.isSuccessful)
            }

            val output = messages.joinToString("\n")
            assertTrue(output.contains("method=POST"))
            assertTrue(output.contains("path=/api/subscriptions/:id/onu/:id"))
            assertTrue(output.contains("status=201"))
            assertTrue(output.contains("duration_ms="))
            assertFalse(output.contains("query-secret"))
            assertFalse(output.contains("header-secret"))
            assertFalse(output.contains("request-secret"))
            assertFalse(output.contains("response-secret"))
            assertFalse(output.contains("485754439F4BF950"))
        } finally {
            server.shutdown()
        }
    }
}
