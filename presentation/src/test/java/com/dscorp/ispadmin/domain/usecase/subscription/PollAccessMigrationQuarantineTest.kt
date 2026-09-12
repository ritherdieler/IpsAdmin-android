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

class PollAccessMigrationQuarantineTest {

    @Test
    fun `stops polling at QUARANTINE even if done flag is missing`() = runTest {
        val repository = mockk<AccessMigrationRepository>()
        val useCase = PollAccessMigrationUseCase(
            accessMigrationRepository = repository,
            delayMs = 1L,
            timeoutMs = 10L,
            clock = { 0L },
        )
        coEvery { repository.getProgress(11) } returns AccessMigrationProgress(
            subscriptionId = 11,
            stage = AccessMigrationStage.QUARANTINE,
            pppoeUsername = "gf-11",
            done = false,
        )

        val result = useCase(11)

        assertTrue(result.isSuccess)
        assertEquals(AccessMigrationStage.QUARANTINE, result.getOrThrow().stage)
        coVerify(exactly = 1) { repository.getProgress(11) }
    }
}
