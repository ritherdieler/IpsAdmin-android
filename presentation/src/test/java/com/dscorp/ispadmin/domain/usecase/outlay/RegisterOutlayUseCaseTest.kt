package com.dscorp.ispadmin.domain.usecase.outlay

import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.domain.repository.OutlayReceiptPreparer
import com.dscorp.ispadmin.domain.repository.OutlayRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RegisterOutlayUseCaseTest {

    private val outlayRepository = mockk<OutlayRepository>()
    private val receiptPreparer = mockk<OutlayReceiptPreparer>()
    private val useCase = RegisterOutlayUseCase(outlayRepository, receiptPreparer)

    @Test
    fun `invoke returns success when validation preparer and save succeed`() = runTest {
        val outlay = Outlay(
            amount = "10.5",
            description = "Test",
            category = "Cat",
            cost_center = "CC1"
        )
        val tempFile = File.createTempFile("receipt", ".jpg")
        coEvery { outlayRepository.currentResponsibleUserId() } returns 7
        coEvery { receiptPreparer.prepareCompressedReceipts(listOf("content://a")) } returns listOf(tempFile)
        coEvery { outlayRepository.saveOutlay(any(), any()) } returns Unit

        val result = useCase(outlay, listOf("content://a"))

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            outlayRepository.saveOutlay(match { it.responsibleId == 7 }, listOf(tempFile))
        }
    }

    @Test
    fun `invoke returns failure when outlay is invalid`() = runTest {
        val invalid = Outlay(amount = "0", description = "", category = "", cost_center = "")

        val result = useCase(invalid, listOf("content://a"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { receiptPreparer.prepareCompressedReceipts(any()) }
        coVerify(exactly = 0) { outlayRepository.saveOutlay(any(), any()) }
    }

    @Test
    fun `invoke returns failure when preparer throws`() = runTest {
        val outlay = Outlay(
            amount = "5",
            description = "D",
            category = "C",
            cost_center = "X"
        )
        coEvery { outlayRepository.currentResponsibleUserId() } returns 1
        coEvery { receiptPreparer.prepareCompressedReceipts(any()) } throws IllegalStateException("File not found")

        val result = useCase(outlay, listOf("content://x"))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { outlayRepository.saveOutlay(any(), any()) }
    }
}
