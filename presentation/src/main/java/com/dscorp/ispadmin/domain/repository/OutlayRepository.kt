package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.Outlay
import java.io.File

interface OutlayRepository {
    fun currentResponsibleUserId(): Int?

    suspend fun saveOutlay(outlay: Outlay, receiptFiles: List<File>)
}
