package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.RegistrationProgress
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository
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
    ): Result<RegistrationProgress> = runCatching {
        val deadline = clock() + timeoutMs
        var latest: RegistrationProgress? = null
        while (clock() <= deadline) {
            val progress = subscriptionActionsRepository.getRegistrationProgress(subscriptionId)
            latest = progress
            onProgress(progress)
            if (progress.done) return@runCatching progress
            delay(delayMs)
        }
        error(
            latest?.message?.takeIf { it.isNotBlank() }
                ?: "Timeout esperando aprovisionamiento"
        )
    }

    companion object {
        const val DEFAULT_POLL_INTERVAL_MS = 2_000L
        const val DEFAULT_TIMEOUT_MS = 120_000L
    }
}

fun RegistrationProgress.toSubscriptionOrNull(
    wifiFallback: Subscription? = null,
): Subscription? {
    val base = subscription ?: return null
    return base.copy(
        wifiSsid24 = base.wifiSsid24 ?: wifiFallback?.wifiSsid24,
        wifiSsid5 = base.wifiSsid5 ?: wifiFallback?.wifiSsid5,
        wifiPassword24 = wifiFallback?.wifiPassword24 ?: base.wifiPassword24,
        wifiPassword5 = wifiFallback?.wifiPassword5 ?: base.wifiPassword5,
    )
}
