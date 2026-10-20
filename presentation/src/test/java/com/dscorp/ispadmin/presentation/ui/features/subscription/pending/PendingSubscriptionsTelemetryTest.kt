package com.dscorp.ispadmin.presentation.ui.features.subscription.pending

import com.dscorp.ispadmin.domain.model.PendingSubscription
import com.dscorp.ispadmin.domain.usecase.subscription.ObservePendingSubscriptionsUseCase
import com.dscorp.ispadmin.observability.ObservabilityClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PendingSubscriptionsTelemetryTest {

    private val observePending = mockk<ObservePendingSubscriptionsUseCase>()
    private val observability = mockk<ObservabilityClient>(relaxed = true)

    @Before
    fun setUp() {
        PendingSubscriptionsTelemetry.resetForTest()
    }

    private fun queued(count: Int) {
        val items = List(count) { index ->
            PendingSubscription(
                localId = "local-$index",
                clientRequestId = "request-$index",
                subscriptionJson = "{}",
                createdAt = 0L,
            )
        }
        every { observePending() } returns Result.success(flowOf(items))
    }

    @Test
    fun `reports the pending count once per process`() = runTest {
        queued(3)
        val telemetry = PendingSubscriptionsTelemetry(observePending, observability)

        telemetry.reportOncePerProcess()
        telemetry.reportOncePerProcess()

        verify(exactly = 1) {
            observability.reportLog(
                message = "pending_subscriptions.count",
                severity = "info",
                tags = mapOf("count" to 3),
            )
        }
        verify(exactly = 1) { observability.reportLog(any(), any(), any()) }
    }

    @Test
    fun `reports zero when nothing is queued`() = runTest {
        queued(0)

        PendingSubscriptionsTelemetry(observePending, observability).reportOncePerProcess()

        verify(exactly = 1) {
            observability.reportLog(
                message = "pending_subscriptions.count",
                severity = "info",
                tags = mapOf("count" to 0),
            )
        }
    }

    @Test
    fun `a different instance in the same process does not report again`() = runTest {
        queued(2)

        PendingSubscriptionsTelemetry(observePending, observability).reportOncePerProcess()
        PendingSubscriptionsTelemetry(observePending, observability).reportOncePerProcess()

        verify(exactly = 1) { observability.reportLog(any(), any(), any()) }
    }

    @Test
    fun `does not report and allows a later attempt when the queue cannot be read`() = runTest {
        every { observePending() } returns Result.failure(IllegalStateException("db unavailable"))
        val telemetry = PendingSubscriptionsTelemetry(observePending, observability)

        telemetry.reportOncePerProcess()
        verify(exactly = 0) { observability.reportLog(any(), any(), any()) }

        queued(1)
        telemetry.reportOncePerProcess()
        verify(exactly = 1) { observability.reportLog(any(), any(), any()) }
    }

    @Test
    fun `a flow that throws while collecting does not crash and allows a later attempt`() = runTest {
        every { observePending() } returns Result.success(flow { throw IllegalStateException("disk full") })
        val telemetry = PendingSubscriptionsTelemetry(observePending, observability)

        telemetry.reportOncePerProcess()
        verify(exactly = 0) { observability.reportLog(any(), any(), any()) }

        queued(1)
        telemetry.reportOncePerProcess()
        verify(exactly = 1) { observability.reportLog(any(), any(), any()) }
    }

    @Test
    fun `a cancelled collection releases the guard so the next process start retries`() = runTest {
        every { observePending() } returns Result.success(flow { awaitCancellation() })
        val telemetry = PendingSubscriptionsTelemetry(observePending, observability)

        val job = launch { telemetry.reportOncePerProcess() }
        runCurrent()
        job.cancel()
        runCurrent()
        verify(exactly = 0) { observability.reportLog(any(), any(), any()) }

        queued(2)
        telemetry.reportOncePerProcess()
        telemetry.reportOncePerProcess()

        verify(exactly = 1) {
            observability.reportLog(
                message = "pending_subscriptions.count",
                severity = "info",
                tags = mapOf("count" to 2),
            )
        }
    }
}
