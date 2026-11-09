package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.domain.model.OutlaySaveResult
import java.io.File

interface OutlayRepository {
    fun currentResponsibleUserId(): Int?

    /** Only administrators and accountants register FIXED outlays. */
    fun canRegisterFixedOutlay(): Boolean

    suspend fun saveOutlay(outlay: Outlay, receiptFiles: List<File>): OutlaySaveResult
}
