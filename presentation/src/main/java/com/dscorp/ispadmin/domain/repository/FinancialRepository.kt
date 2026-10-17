package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import com.dscorp.ispadmin.domain.model.financial.FinancialReport

interface FinancialRepository {
    suspend fun load(serviceMonth: String, movementsMonth: String): Result<FinancialReport>
    suspend fun export(cutId: String): Result<DownloadDocumentResponse>
}
