package com.dscorp.ispadmin.presentation.ui.features.subscription.pending

import androidx.annotation.VisibleForTesting
import com.dscorp.ispadmin.domain.usecase.subscription.ObservePendingSubscriptionsUseCase
import com.dscorp.ispadmin.observability.ObservabilityClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reports how many offline registrations are still queued, once per process.
 * The number tells us when the legacy queue can be removed.
 */
class PendingSubscriptionsTelemetry(
    private val observePending: ObservePendingSubscriptionsUseCase,
    private val observability: ObservabilityClient,
) {

    suspend fun reportOncePerProcess() {
        if (!reported.compareAndSet(false, true)) return
        var sent = false
        try {
            val count = observePending().getOrNull()?.first()?.size ?: return
            observability.reportLog(
                message = "pending_subscriptions.count",
                severity = "info",
                tags = mapOf("count" to count),
            )
            sent = true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Room can throw while the query runs; telemetry must never crash the app.
        } finally {
            // Cancelled, failed or unreadable: allow a later attempt in this process.
            if (!sent) reported.set(false)
        }
    }

    companion object {
        private val reported = AtomicBoolean(false)

        @VisibleForTesting
        internal fun resetForTest() = reported.set(false)
    }
}
