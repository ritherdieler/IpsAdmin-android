package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.AccessMigrationProgress
import com.dscorp.ispadmin.domain.model.AccessMigrationStage
import com.dscorp.ispadmin.domain.repository.AccessMigrationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PollAccessMigrationUseCaseTest {

    private val repository = mockk<AccessMigrationRepository>()
    private var now = 0L
    private val useCase = PollAccessMigrationUseCase(
        accessMigrationRepository = repository,
        delayMs = 1L,
        timeoutMs = 10L,
        clock = { now },
    )

    @Test
    fun `returns when progress is terminal DONE`() = runTest {
        val done = AccessMigrationProgress(
            subscriptionId = 1,
            stage = AccessMigrationStage.DONE,
            done = true,
            pppoeUsername = "gf-1",
        )
        coEvery { repository.getProgress(1) } returns done

        val result = useCase(1)

        assertTrue(result.isSuccess)
        assertEquals(AccessMigrationStage.DONE, result.getOrThrow().stage)
        coVerify(exactly = 1) { repository.getProgress(1) }
    }

    @Test
    fun `polls until FAILED_STRANDED`() = runTest {
        val pending = AccessMigrationProgress(
            subscriptionId = 1,
            stage = AccessMigrationStage.OLT_READY,
            done = false,
        )
        val stranded = pending.copy(
            stage = AccessMigrationStage.FAILED_STRANDED,
            failureReason = "CPE no responde Inform",
            done = true,
        )
        coEvery { repository.getProgress(1) } returnsMany listOf(pending, stranded)
        now = 0L

        val result = useCase(1) {
            now += 2L
        }

        assertTrue(result.isSuccess)
        assertEquals(AccessMigrationStage.FAILED_STRANDED, result.getOrThrow().stage)
        coVerify(exactly = 2) { repository.getProgress(1) }
    }
}
