package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.RegistrationProgress
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class PollRegistrationProgressUseCase(
    private val subscriptionActionsRepository: SubscriptionActionsRepository,
    private val delayMs: Long = DEFAULT_POLL_INTERVAL_MS,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend operator fun invoke(
        subscriptionId: Int,
        onProgress: suspend (RegistrationProgress) -> Unit = {},
    ): Result<RegistrationProgress> {
        val deadline = clock() + timeoutMs
        var latest: RegistrationProgress? = null
        var consecutiveErrors = 0
        var polls = 0
        while (clock() <= deadline) {
            try {
                val progress = subscriptionActionsRepository.getRegistrationProgress(subscriptionId)
                consecutiveErrors = 0
                latest = progress
                onProgress(progress)
                if (progress.done) return Result.success(progress)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                consecutiveErrors++
                if (consecutiveErrors > MAX_CONSECUTIVE_ERRORS) return Result.failure(error)
            }
            polls++
            delay(nextDelay(polls))
        }
        return Result.failure(
            IllegalStateException(latest?.message?.takeIf { it.isNotBlank() } ?: "Timeout esperando aprovisionamiento")
        )
    }

    private fun nextDelay(polls: Int): Long =
        (delayMs + polls / POLLS_PER_STEP * DELAY_STEP_MS).coerceAtMost(maxOf(delayMs, MAX_POLL_INTERVAL_MS))

    companion object {
        const val MAX_CONSECUTIVE_ERRORS = 3
        const val MAX_POLL_INTERVAL_MS = 5_000L
        private const val POLLS_PER_STEP = 10
        private const val DELAY_STEP_MS = 1_000L
        const val DEFAULT_POLL_INTERVAL_MS = 2_000L
        const val DEFAULT_TIMEOUT_MS = 300_000L
    }
}

fun RegistrationProgress.toSubscriptionOrNull(
    registration: Subscription? = null,
): Subscription? {
    val base = subscription ?: return null
    return base.copy(
        wifiSsid24 = base.wifiSsid24 ?: registration?.wifiSsid24,
        wifiSsid5 = base.wifiSsid5 ?: registration?.wifiSsid5,
        wifiPassword24 = registration?.wifiPassword24 ?: base.wifiPassword24,
        wifiPassword5 = registration?.wifiPassword5 ?: base.wifiPassword5,
        pppoePassword = base.pppoePassword ?: registration?.pppoePassword,
    )
}
