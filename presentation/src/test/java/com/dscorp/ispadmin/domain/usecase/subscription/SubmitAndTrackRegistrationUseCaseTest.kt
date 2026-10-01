package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.OnuRegistrationOutcome
import com.dscorp.ispadmin.domain.model.RegistrationConflictException
import com.dscorp.ispadmin.domain.model.Subscription
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class SubmitAndTrackRegistrationUseCaseTest {

    private val register = mockk<RegisterSubscriptionUseCase>()
    private val operations = mockk<OnuRegistrationOperationUseCase>()
    private val useCase = SubmitAndTrackRegistrationUseCase(register, operations)
    private val fiber = Subscription(firstName = "Ana", registrationOperationId = "op-1", clientRequestId = "op-1")

    @Test
    fun `a confirmed registration is accepted`() = runTest {
        coEvery { register(any(), any(), any()) } returns
            Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 5)))

        val result = useCase.submit(fiber, orderId = null, facadePhotoFile = null)

        assertEquals(5, (result as RegistrationSubmission.Accepted).subscription.resolvedSubscriptionId())
    }

    @Test
    fun `a lost response is reconciled with the server outcome`() = runTest {
        coEvery { register(any(), any(), any()) } returns Result.failure(SocketTimeoutException("timeout"))
        coEvery { operations.outcome("op-1") } returns OnuRegistrationOutcome("op-1", 90, "PROVISIONING", "RUNNING", "RUNNING")
        var reconciling = false

        val result = useCase.submit(fiber, orderId = null, facadePhotoFile = null) { reconciling = true }

        val accepted = result as RegistrationSubmission.Accepted
        assertEquals(90, accepted.subscription.resolvedSubscriptionId())
        assertTrue(accepted.subscription.provisioningPending)
        assertTrue(accepted.reconciled)
        assertTrue(reconciling)
    }

    @Test
    fun `an already processed conflict is reconciled too`() = runTest {
        coEvery { register(any(), any(), any()) } returns
            Result.failure(RegistrationConflictException("REGISTRATION_ALREADY_EXISTS", "ya procesada"))
        coEvery { operations.outcome("op-1") } returns OnuRegistrationOutcome("op-1", 91, "PROVISIONING", "SUCCEEDED", "SUCCEEDED")

        val result = useCase.submit(fiber, orderId = null, facadePhotoFile = null)

        assertEquals(91, (result as RegistrationSubmission.Accepted).subscription.resolvedSubscriptionId())
    }

    @Test
    fun `an unconfirmed result stays uncertain`() = runTest {
        coEvery { register(any(), any(), any()) } returns Result.failure(SocketTimeoutException("timeout"))
        coEvery { operations.outcome("op-1") } returns null

        assertTrue(useCase.submit(fiber, orderId = null, facadePhotoFile = null) is RegistrationSubmission.Uncertain)
    }

    @Test
    fun `wireless network failures are uncertain without a server lookup`() = runTest {
        coEvery { register(any(), any(), any()) } returns Result.failure(SocketTimeoutException("timeout"))

        val result = useCase.submit(Subscription(firstName = "Ana", clientRequestId = "form-1"), null, null)

        assertTrue(result is RegistrationSubmission.Uncertain)
        coVerify(exactly = 0) { operations.outcome(any()) }
    }

    @Test
    fun `validation errors are rejected`() = runTest {
        coEvery { register(any(), any(), any()) } returns Result.failure(IllegalArgumentException("DNI inválido"))

        assertTrue(useCase.submit(fiber, null, null) is RegistrationSubmission.Rejected)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation during reconciliation is propagated`() = runTest {
        coEvery { register(any(), any(), any()) } returns Result.failure(SocketTimeoutException("timeout"))
        coEvery { operations.outcome("op-1") } throws CancellationException("left")

        useCase.submit(fiber, null, null)
    }
}
