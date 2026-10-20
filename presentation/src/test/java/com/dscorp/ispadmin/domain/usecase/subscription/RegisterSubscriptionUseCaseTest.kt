package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.repository.SubscriptionWriteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.UUID

class RegisterSubscriptionUseCaseTest {

    private val subscriptionWriteRepository = mockk<SubscriptionWriteRepository>()
    private lateinit var useCase: RegisterSubscriptionUseCase
    private lateinit var facadePhotoFile: File

    @Before
    fun setUp() {
        facadePhotoFile = File.createTempFile("facade", ".jpg").apply { writeText("photo") }
        useCase = RegisterSubscriptionUseCase(subscriptionWriteRepository = subscriptionWriteRepository)
    }

    @Test
    fun `without an operation it registers online with the photo`() = runTest {
        val captured = slot<Subscription>()
        val registered = Subscription(subscriptionId = 11)
        coEvery {
            subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(capture(captured), facadePhotoFile)
        } returns registered

        val result = useCase(Subscription(firstName = "Luis"), orderId = 9, facadePhotoFile = facadePhotoFile)

        assertTrue(result.isSuccess)
        assertEquals(registered, (result.getOrThrow() as RegisterSubscriptionResult.Registered).subscription)
        assertEquals(9, captured.captured.installationOrderId)
        UUID.fromString(captured.captured.clientRequestId!!)
        coVerify(exactly = 1) {
            subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(any(), facadePhotoFile)
        }
        coVerify(exactly = 0) { subscriptionWriteRepository.registerSubscription(any()) }
    }

    @Test
    fun `without an operation or photo it registers online and sends installationOrderId`() = runTest {
        val captured = slot<Subscription>()
        val registered = Subscription(subscriptionId = 10)
        coEvery { subscriptionWriteRepository.registerSubscription(capture(captured)) } returns registered

        val result = useCase(Subscription(firstName = "Ana"), orderId = 5)

        assertTrue(result.isSuccess)
        val outcome = result.getOrThrow() as RegisterSubscriptionResult.Registered
        assertEquals(registered, outcome.subscription)
        assertEquals(5, captured.captured.installationOrderId)
        assertNotNull(captured.captured.clientRequestId)
        UUID.fromString(captured.captured.clientRequestId!!)
        coVerify(exactly = 1) { subscriptionWriteRepository.registerSubscription(any()) }
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `cancellation is propagated instead of becoming a failure result`() = runTest {
        coEvery { subscriptionWriteRepository.registerSubscription(any()) } throws
            kotlinx.coroutines.CancellationException("screen left")

        useCase(Subscription(firstName = "Ana", registrationOperationId = "op-1"), orderId = null)
    }

    @Test
    fun `with a registration operation id it registers online without the photo`() = runTest {
        val captured = slot<Subscription>()
        coEvery { subscriptionWriteRepository.registerSubscription(capture(captured)) } returns Subscription(subscriptionId = 81)

        val result = useCase(
            Subscription(firstName = "Ana", registrationOperationId = "preauth-op-81", facadePhotoUrl = "https://storage/photo.jpg"),
            orderId = null,
            facadePhotoFile = facadePhotoFile,
        )

        assertTrue(result.isSuccess)
        assertEquals("preauth-op-81", captured.captured.clientRequestId)
        assertEquals("preauth-op-81", captured.captured.registrationOperationId)
        assertEquals("https://storage/photo.jpg", captured.captured.facadePhotoUrl)
        coVerify(exactly = 1) { subscriptionWriteRepository.registerSubscription(any()) }
        coVerify(exactly = 0) { subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(any(), any()) }
    }

    @Test
    fun `online success with provisioningPending still returns Registered`() = runTest {
        val registered = Subscription(subscriptionId = 22, provisioningPending = true)
        coEvery { subscriptionWriteRepository.registerSubscription(any()) } returns registered

        val result = useCase(Subscription(firstName = "Ana"), orderId = null)

        assertTrue(result.isSuccess)
        val outcome = result.getOrThrow() as RegisterSubscriptionResult.Registered
        assertTrue(outcome.subscription.provisioningPending)
    }

    @Test
    fun `a network failure returns a failure and never queues`() = runTest {
        coEvery {
            subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(any(), any())
        } throws IOException("no route to host")

        val result = useCase(Subscription(firstName = "Ana"), orderId = 8, facadePhotoFile = facadePhotoFile)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        coVerify(exactly = 1) {
            subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(any(), facadePhotoFile)
        }
    }

    @Test
    fun `it keeps the given clientRequestId and generates one when missing`() = runTest {
        val captured = mutableListOf<Subscription>()
        coEvery { subscriptionWriteRepository.registerSubscription(capture(captured)) } returns Subscription(subscriptionId = 1)
        val fixed = RegisterSubscriptionUseCase(subscriptionWriteRepository, uuidGenerator = { "generated-id" })

        fixed(Subscription(firstName = "Ana", clientRequestId = "form-req-1"), orderId = null)
        fixed(Subscription(firstName = "Ana"), orderId = null)

        assertEquals("form-req-1", captured[0].clientRequestId)
        assertEquals("generated-id", captured[1].clientRequestId)
    }
}
