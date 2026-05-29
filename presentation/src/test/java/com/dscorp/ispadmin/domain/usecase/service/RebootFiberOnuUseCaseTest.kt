package com.dscorp.ispadmin.domain.usecase.service

import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RebootFiberOnuUseCaseTest {

    private val subscriptionActionsRepository = mockk<SubscriptionActionsRepository>()
    private val useCase = RebootFiberOnuUseCase(subscriptionActionsRepository)

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        coEvery { subscriptionActionsRepository.rebootFiberOnu(42) } returns Unit

        val result = useCase(42)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { subscriptionActionsRepository.rebootFiberOnu(42) }
    }

    @Test
    fun `invoke returns failure when repository throws`() = runTest {
        coEvery { subscriptionActionsRepository.rebootFiberOnu(1) } throws Exception("error")

        val result = useCase(1)

        assertTrue(result.isFailure)
    }
}
