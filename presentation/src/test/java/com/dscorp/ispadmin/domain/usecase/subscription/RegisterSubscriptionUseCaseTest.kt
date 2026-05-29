package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.data.repository.InstallationOrderRepository
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.repository.SubscriptionWriteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterSubscriptionUseCaseTest {

    private val subscriptionWriteRepository = mockk<SubscriptionWriteRepository>()
    private val installationOrderRepository = mockk<InstallationOrderRepository>()
    private val useCase = RegisterSubscriptionUseCase(subscriptionWriteRepository, installationOrderRepository)

    @Test
    fun `invoke registers subscription without closing order when orderId null`() = runTest {
        val subscription = Subscription()
        coEvery { subscriptionWriteRepository.registerSubscription(subscription) } returns subscription

        val result = useCase(subscription, null)

        assertTrue(result.isSuccess)
        assertEquals(subscription, result.getOrNull())
        coVerify(exactly = 1) { subscriptionWriteRepository.registerSubscription(subscription) }
        coVerify(exactly = 0) { installationOrderRepository.closeInstallationOrder(any()) }
    }

    @Test
    fun `invoke closes installation order then registers when orderId present`() = runTest {
        val subscription = Subscription()
        coEvery { installationOrderRepository.closeInstallationOrder(5) } returns InstallationOrder(id = 5)
        coEvery { subscriptionWriteRepository.registerSubscription(subscription) } returns subscription

        val result = useCase(subscription, 5)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { installationOrderRepository.closeInstallationOrder(5) }
        coVerify(exactly = 1) { subscriptionWriteRepository.registerSubscription(subscription) }
    }
}
