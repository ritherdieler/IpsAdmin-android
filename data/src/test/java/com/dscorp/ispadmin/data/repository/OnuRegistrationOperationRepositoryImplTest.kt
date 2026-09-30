package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.remote.OnuRegistrationOperationApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OnuRegistrationOperationRepositoryImplTest {
    @Test
    fun `posts full cleanup after cancelled linked registration`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"status":"COMPLETE"}"""))
            val api = Retrofit.Builder()
                .baseUrl(server.url("/ispadmin/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OnuRegistrationOperationApi::class.java)
            val repository = OnuRegistrationOperationRepositoryImpl(api)

            assertEquals("COMPLETE", repository.cleanupCancelled("registration-1").status)
            val request = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            assertEquals("POST", request.method)
            assertEquals("/ispadmin/onu-registration-operations/registration-1/cleanup", request.path)
        }
    }

    @Test
    fun `treats missing operation as already cleaned for response retry`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404))
            val api = Retrofit.Builder()
                .baseUrl(server.url("/ispadmin/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OnuRegistrationOperationApi::class.java)
            val repository = OnuRegistrationOperationRepositoryImpl(api)

            assertEquals("COMPLETE", repository.cleanupCancelled("registration-1").status)
        }
    }

    @Test
    fun `saves and reads a registration draft through Retrofit`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"firstName":"Test"}"""))
            server.enqueue(MockResponse().setBody("""{"firstName":"Test"}"""))
            val api = Retrofit.Builder()
                .baseUrl(server.url("/ispadmin/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OnuRegistrationOperationApi::class.java)
            val repository = OnuRegistrationOperationRepositoryImpl(api)

            repository.saveDraft("operation-1", mapOf("firstName" to "Test"))
            assertEquals("Test", repository.draft("operation-1")?.get("firstName"))

            val saveRequest = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            assertEquals("PUT", saveRequest.method)
            assertEquals("/ispadmin/onu-registration-operations/operation-1/draft", saveRequest.path)
            assertTrue(saveRequest.body.readUtf8().contains("\"firstName\":\"Test\""))
        }
    }
}
