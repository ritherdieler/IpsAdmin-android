package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.RegistrationProgress
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PollRegistrationProgressUseCaseTest {

    private val repository = mockk<SubscriptionActionsRepository>()
    private var now = 0L
    private val useCase = PollRegistrationProgressUseCase(
        subscriptionActionsRepository = repository,
        delayMs = 1L,
        timeoutMs = 10L,
        clock = { now },
    )

    @Test
    fun `returns when progress is done`() = runTest {
        val done = RegistrationProgress(
            subscriptionId = 1,
            step = "DONE",
            message = "Listo",
            done = true,
            subscription = Subscription(subscriptionId = 1, tr069ProvisionStatus = "COMPLETE"),
        )
        coEvery { repository.getRegistrationProgress(1) } returns done

        val result = useCase(1)

        assertTrue(result.isSuccess)
        assertEquals("DONE", result.getOrThrow().step)
        coVerify(exactly = 1) { repository.getRegistrationProgress(1) }
    }

    @Test
    fun `polls until done`() = runTest {
        val pending = RegistrationProgress(
            subscriptionId = 1,
            step = "WAITING_ACS",
            message = "Esperando ACS…",
            done = false,
        )
        val done = pending.copy(step = "DONE", message = "Listo", done = true)
        coEvery { repository.getRegistrationProgress(1) } returnsMany listOf(pending, done)
        now = 0L

        val result = useCase(1) {
            now += 2L
        }

        assertTrue(result.isSuccess)
        assertEquals("DONE", result.getOrThrow().step)
        coVerify(exactly = 2) { repository.getRegistrationProgress(1) }
    }
}
