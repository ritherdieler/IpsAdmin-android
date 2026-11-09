package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.domain.model.OutlaySaveResult
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.repository.OutlayRepository
import java.io.File

class OutlayRepositoryImpl(
    private val iRepository: IRepository
) : OutlayRepository {

    override fun currentResponsibleUserId(): Int? = iRepository.getUserSession()?.id

    override fun canRegisterFixedOutlay(): Boolean =
        iRepository.getUserSession()?.type in setOf(User.UserType.ADMIN, User.UserType.ACCOUNTANT)

    override suspend fun saveOutlay(outlay: Outlay, receiptFiles: List<File>): OutlaySaveResult {
        return iRepository.saveOutLay(outlay, receiptFiles)
    }
}
