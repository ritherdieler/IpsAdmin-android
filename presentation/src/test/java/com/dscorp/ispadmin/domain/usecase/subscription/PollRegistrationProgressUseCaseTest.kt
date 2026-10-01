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
    fun `tolerates transient network errors and keeps polling`() = runTest {
        val tolerant = PollRegistrationProgressUseCase(repository, delayMs = 1L, timeoutMs = 60_000L)
        val done = RegistrationProgress(subscriptionId = 1, step = "DONE", message = "Listo", done = true)
        coEvery { repository.getRegistrationProgress(1) } throws java.io.IOException("reset") andThenThrows
            java.io.IOException("reset") andThen done

        val result = tolerant(1)

        assertTrue(result.isSuccess)
        coVerify(exactly = 3) { repository.getRegistrationProgress(1) }
    }

    @Test
    fun `fails after too many consecutive errors`() = runTest {
        val tolerant = PollRegistrationProgressUseCase(repository, delayMs = 1L, timeoutMs = 60_000L)
        coEvery { repository.getRegistrationProgress(1) } throws java.io.IOException("offline")

        val result = tolerant(1)

        assertTrue(result.isFailure)
        coVerify(exactly = PollRegistrationProgressUseCase.MAX_CONSECUTIVE_ERRORS + 1) {
            repository.getRegistrationProgress(1)
        }
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `cancellation stops polling and is propagated`() = runTest {
        coEvery { repository.getRegistrationProgress(1) } throws kotlinx.coroutines.CancellationException("left")

        useCase(1)
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

    @Test
    fun `progress subscription keeps the pppoe password returned by the registration`() {
        val progress = RegistrationProgress(
            subscriptionId = 114,
            step = "DONE",
            message = "Listo",
            done = true,
            subscription = Subscription(subscriptionId = 114, pppoeUsername = "gf114", pppoePassword = null),
        )
        val registered = Subscription(subscriptionId = 114, pppoeUsername = "gf114", pppoePassword = "s3cret-pass")

        val merged = progress.toSubscriptionOrNull(registration = registered)

        assertEquals("s3cret-pass", merged?.pppoePassword)
    }
}
