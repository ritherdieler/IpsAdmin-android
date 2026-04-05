package com.dscorp.ispadmin.domain.usecase

import com.dscorp.ispadmin.data.repository.IRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RebootFiberOnuUseCaseTest {

    private val repository = mockk<IRepository>()
    private val useCase = RebootFiberOnuUseCase(repository)

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        coEvery { repository.rebootFiberOnu(42) } returns Unit

        val result = useCase(42)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.rebootFiberOnu(42) }
    }

    @Test
    fun `invoke returns failure when repository throws`() = runTest {
        coEvery { repository.rebootFiberOnu(1) } throws Exception("error")

        val result = useCase(1)

        assertTrue(result.isFailure)
    }
}
