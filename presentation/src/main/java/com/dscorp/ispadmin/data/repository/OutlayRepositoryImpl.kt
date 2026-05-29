package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.domain.repository.OutlayRepository
import java.io.File

class OutlayRepositoryImpl(
    private val iRepository: IRepository
) : OutlayRepository {

    override fun currentResponsibleUserId(): Int? = iRepository.getUserSession()?.id

    override suspend fun saveOutlay(outlay: Outlay, receiptFiles: List<File>) {
        iRepository.saveOutLay(outlay, receiptFiles)
    }
}
