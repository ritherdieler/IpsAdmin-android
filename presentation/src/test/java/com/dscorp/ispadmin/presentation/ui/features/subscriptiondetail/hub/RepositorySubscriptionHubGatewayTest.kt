package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import com.dscorp.ispadmin.data.datasource.remote.ChargeApi
import com.dscorp.ispadmin.data.datasource.remote.PaymentBatchRequest
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.di.buildApiGson
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RepositorySubscriptionHubGatewayTest {

    private val chargeApi = mockk<ChargeApi>()
    private val gateway = RepositorySubscriptionHubGateway(
        repository = mockk<IRepository>(),
        restoreInternetConnection = mockk(),
        chargeApi = chargeApi,
    )

    @Test
    fun `the batch charge names the collector because the current Core requires it`() = runTest {
        val sent = slot<PaymentBatchRequest>()
        coEvery { chargeApi.registerBatch(capture(sent)) } returns emptyList()

        gateway.registerCharge(ChargeBatch(listOf(1, 2), "Yape", 0.0, null, "Jorge Ramos V.", responsibleId = 7))

        assertEquals(PaymentBatchRequest(listOf(1, 2), "Yape", responsibleId = 7, electronicPayerName = "Jorge Ramos V."), sent.captured)
        assertEquals(7, buildApiGson().toJsonTree(sent.captured).asJsonObject.get("responsibleId").asInt)
    }
}
