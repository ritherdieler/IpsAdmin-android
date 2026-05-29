package com.dscorp.ispadmin.domain.usecase.outlay

import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.domain.repository.OutlayReceiptPreparer
import com.dscorp.ispadmin.domain.repository.OutlayRepository

class RegisterOutlayUseCase(
    private val outlayRepository: OutlayRepository,
    private val receiptPreparer: OutlayReceiptPreparer
) {

    suspend operator fun invoke(
        outlay: Outlay,
        photoUriStrings: List<String>
    ): Result<Unit> = runCatching {
        if (!outlay.isValid()) {
            throw IllegalArgumentException("Datos incompletos")
        }
        val outlayWithResponsible = outlay.apply {
            responsibleId = outlayRepository.currentResponsibleUserId()
        }
        val fileList = receiptPreparer.prepareCompressedReceipts(photoUriStrings)
        outlayRepository.saveOutlay(outlayWithResponsible, fileList)
    }
}
