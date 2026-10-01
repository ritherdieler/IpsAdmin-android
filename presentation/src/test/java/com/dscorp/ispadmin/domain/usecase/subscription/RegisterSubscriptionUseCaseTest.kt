package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.connectivity.MikrotikReachabilityMonitor
import com.dscorp.ispadmin.domain.connectivity.NetworkConnectivityMonitor
import com.dscorp.ispadmin.domain.model.PendingSubscription
import com.dscorp.ispadmin.domain.model.PendingSubscriptionStatus
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.repository.SubscriptionWriteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class RegisterSubscriptionUseCaseTest {

    private val subscriptionWriteRepository = mockk<SubscriptionWriteRepository>()
    private val enqueuePendingSubscriptionUseCase = mockk<EnqueuePendingSubscriptionUseCase>()
    private val connectivityMonitor = mockk<NetworkConnectivityMonitor>()
    private val mikrotikReachabilityMonitor = mockk<MikrotikReachabilityMonitor>()
    private lateinit var useCase: RegisterSubscriptionUseCase
    private lateinit var facadePhotoFile: File

    @Before
    fun setUp() {
        facadePhotoFile = File.createTempFile("facade", ".jpg").apply { writeText("photo") }
        useCase = RegisterSubscriptionUseCase(
            subscriptionWriteRepository = subscriptionWriteRepository,
            enqueuePendingSubscriptionUseCase = enqueuePendingSubscriptionUseCase,
            connectivityMonitor = connectivityMonitor,
            mikrotikReachabilityMonitor = mikrotikReachabilityMonitor
        )
        every { connectivityMonitor.isConnected() } returns true
        coEvery { mikrotikReachabilityMonitor.isMikrotikReachable() } returns true
    }

    @Test
    fun `online with orderId sends clientRequestId and installationOrderId and never closes order locally`() = runTest {
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
        coVerify(exactly = 0) { enqueuePendingSubscriptionUseCase(any(), any(), any()) }
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `cancellation is propagated instead of becoming a failure result`() = runTest {
        coEvery { subscriptionWriteRepository.registerSubscription(any()) } throws
            kotlinx.coroutines.CancellationException("screen left")

        useCase(Subscription(firstName = "Ana", registrationOperationId = "op-1"), orderId = null)
    }

    @Test
    fun `preauthorized registration is always online and reuses operation idempotency key`() = runTest {
        every { connectivityMonitor.isConnected() } returns false
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
        coVerify(exactly = 0) { enqueuePendingSubscriptionUseCase(any(), any(), any()) }
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
    fun `online with facade photo sends multipart payload with installationOrderId`() = runTest {
        val captured = slot<Subscription>()
        val registered = Subscription(subscriptionId = 11)
        coEvery {
            subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(capture(captured), facadePhotoFile)
        } returns registered

        val result = useCase(Subscription(firstName = "Luis"), orderId = 9, facadePhotoFile = facadePhotoFile)

        assertTrue(result.isSuccess)
        assertEquals(9, captured.captured.installationOrderId)
        UUID.fromString(captured.captured.clientRequestId!!)
        coVerify(exactly = 1) {
            subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(any(), facadePhotoFile)
        }
        coVerify(exactly = 0) { subscriptionWriteRepository.registerSubscription(any()) }
    }

    @Test
    fun `offline enqueues pending subscription with photo and does not call remote`() = runTest {
        every { connectivityMonitor.isConnected() } returns false
        val jsonSlot = slot<String>()
        val pending = PendingSubscription(
            localId = "local-1",
            clientRequestId = "client-1",
            subscriptionJson = """{"firstName":"Ana"}""",
            facadePhotoPath = "/files/pending_subscriptions/local-1.jpg",
            installationOrderId = 8,
            status = PendingSubscriptionStatus.PENDING,
            createdAt = 1L
        )
        coEvery {
            enqueuePendingSubscriptionUseCase(capture(jsonSlot), facadePhotoFile, 8)
        } returns Result.success(pending)

        val result = useCase(
            Subscription(firstName = "Ana", clientIpAddress = "192.168.1.50"),
            orderId = 8,
            facadePhotoFile = facadePhotoFile
        )

        assertTrue(result.isSuccess)
        val outcome = result.getOrThrow() as RegisterSubscriptionResult.QueuedOffline
        assertEquals(pending, outcome.pending)
        assertTrue(jsonSlot.captured.contains("192.168.1.50"))
        assertTrue(jsonSlot.captured.contains("clientIpAddress"))
        coVerify(exactly = 1) { enqueuePendingSubscriptionUseCase(any(), facadePhotoFile, 8) }
        coVerify(exactly = 0) { subscriptionWriteRepository.registerSubscription(any()) }
        coVerify(exactly = 0) {
            subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(any(), any())
        }
    }

    @Test
    fun `connected but MikroTik unreachable enqueues offline instead of calling remote`() = runTest {
        every { connectivityMonitor.isConnected() } returns true
        coEvery { mikrotikReachabilityMonitor.isMikrotikReachable() } returns false
        val pending = PendingSubscription(
            localId = "local-2",
            clientRequestId = "client-2",
            subscriptionJson = """{"clientIpAddress":"10.0.0.8"}""",
            facadePhotoPath = "/files/pending_subscriptions/local-2.jpg",
            installationOrderId = 3,
            status = PendingSubscriptionStatus.PENDING,
            createdAt = 1L
        )
        coEvery {
            enqueuePendingSubscriptionUseCase(any(), facadePhotoFile, 3)
        } returns Result.success(pending)

        val result = useCase(
            Subscription(firstName = "Luis", clientIpAddress = "10.0.0.8"),
            orderId = 3,
            facadePhotoFile = facadePhotoFile
        )

        assertTrue(result.getOrThrow() is RegisterSubscriptionResult.QueuedOffline)
        coVerify(exactly = 0) { subscriptionWriteRepository.registerSubscription(any()) }
        coVerify(exactly = 0) {
            subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(any(), any())
        }
    }

    @Test
    fun `offline without clientIpAddress fails`() = runTest {
        every { connectivityMonitor.isConnected() } returns false

        val result = useCase(
            Subscription(firstName = "Ana"),
            orderId = 8,
            facadePhotoFile = facadePhotoFile
        )

        assertTrue(result.isFailure)
        assertEquals(
            "La IP del cliente es obligatoria en modo offline",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { enqueuePendingSubscriptionUseCase(any(), any(), any()) }
    }

    @Test
    fun `offline without facade photo fails`() = runTest {
        every { connectivityMonitor.isConnected() } returns false

        val result = useCase(
            Subscription(firstName = "Ana", clientIpAddress = "192.168.1.50"),
            orderId = 8,
            facadePhotoFile = null
        )

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { enqueuePendingSubscriptionUseCase(any(), any(), any()) }
    }

    @Test
    fun `offline json includes vlan`() = runTest {
        every { connectivityMonitor.isConnected() } returns false
        val jsonSlot = slot<String>()
        val pending = PendingSubscription(
            localId = "local-vlan",
            clientRequestId = "client-vlan",
            subscriptionJson = """{"vlan":"100"}""",
            facadePhotoPath = "/files/pending_subscriptions/local-vlan.jpg",
            installationOrderId = 8,
            status = PendingSubscriptionStatus.PENDING,
            createdAt = 1L
        )
        coEvery {
            enqueuePendingSubscriptionUseCase(capture(jsonSlot), facadePhotoFile, 8)
        } returns Result.success(pending)

        val result = useCase(
            Subscription(
                firstName = "Ana",
                clientIpAddress = "192.168.1.50",
                vlan = "100"
            ),
            orderId = 8,
            facadePhotoFile = facadePhotoFile
        )

        assertTrue(result.isSuccess)
        assertTrue(jsonSlot.captured.contains("\"vlan\""))
        assertTrue(jsonSlot.captured.contains("100"))
    }
}
