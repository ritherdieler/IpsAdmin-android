package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.AccessMigrationProgress
import com.dscorp.ispadmin.domain.repository.AccessMigrationRepository
import kotlinx.coroutines.delay

class PollAccessMigrationUseCase(
    private val accessMigrationRepository: AccessMigrationRepository,
    private val delayMs: Long = DEFAULT_POLL_INTERVAL_MS,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend operator fun invoke(
        subscriptionId: Int,
        onProgress: suspend (AccessMigrationProgress) -> Unit = {},
    ): Result<AccessMigrationProgress> = runCatching {
        val deadline = clock() + timeoutMs
        var latest: AccessMigrationProgress? = null
        while (clock() <= deadline) {
            val progress = accessMigrationRepository.getProgress(subscriptionId)
            latest = progress
            onProgress(progress)
            if (progress.done || progress.stage?.isTerminal() == true) {
                return@runCatching progress
            }
            delay(delayMs)
        }
        error(
            latest?.message?.takeIf { it.isNotBlank() }
                ?: latest?.failureReason?.takeIf { it.isNotBlank() }
                ?: "Timeout esperando la migración a PPPoE"
        )
    }

    companion object {
        const val DEFAULT_POLL_INTERVAL_MS = 2_000L
        const val DEFAULT_TIMEOUT_MS = 300_000L
    }
}
