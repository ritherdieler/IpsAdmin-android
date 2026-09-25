package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.remote.ProvisioningApi
import com.dscorp.ispadmin.domain.model.ProvisioningAction
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ProvisioningRepositoryTest {
    @Test fun `loads latest operation and sends scoped cancellation to Core`() = runTest {
        MockWebServer().use { server ->
            val body = """{"operation":{"id":"op","subscriptionId":42,"revision":3,"state":"FAILED","checkpoints":[]},"canCancel":true}"""
            server.enqueue(MockResponse().setBody(body))
            server.enqueue(MockResponse().setResponseCode(202).setBody(body))
            val api = Retrofit.Builder().baseUrl(server.url("/ispadmin/")).addConverterFactory(GsonConverterFactory.create())
                .build().create(ProvisioningApi::class.java)
            val repository = ProvisioningRepositoryImpl(api)
            assertEquals("op", repository.latest(42).getOrThrow()?.operation?.id)
            assertTrue(repository.cancel(ProvisioningAction(subscriptionId = 42, operationId = "op", expectedRevision = 3)).isSuccess)
            assertEquals("/ispadmin/subscription/42/provisioning", server.takeRequest(1, TimeUnit.SECONDS)?.path)
            val request = requireNotNull(server.takeRequest(1, TimeUnit.SECONDS))
            assertEquals("/ispadmin/subscription/42/provisioning/cancel", request.path)
            assertEquals("""{"operationId":"op","expectedRevision":3}""", request.body.readUtf8())
        }
    }
}
