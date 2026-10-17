package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.datasource.remote.financial.FinancialApi
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import com.dscorp.ispadmin.domain.model.financial.FinancialReport
import com.dscorp.ispadmin.domain.repository.FinancialRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class FinancialRepositoryImpl(private val api: FinancialApi) : FinancialRepository {
    override suspend fun load(serviceMonth: String, movementsMonth: String): Result<FinancialReport> = request {
        val response = api.report(serviceMonth, movementsMonth)
        if (!response.isSuccessful) throw HttpException(response)
        val body = response.body() ?: error("Empty financial report")
        require(body.id.isNotBlank() && body.cutoff.isNotBlank() && body.serviceMonth == serviceMonth && body.movementsMonth == movementsMonth) { "Invalid financial cut" }
        body.toDomain()
    }
    override suspend fun export(cutId: String): Result<DownloadDocumentResponse> = request {
        val response = api.export(cutId)
        if (!response.isSuccessful) throw HttpException(response)
        response.body() ?: error("Empty financial export")
    }
    private suspend fun <T> request(block: suspend () -> T): Result<T> = try { Result.success(block()) }
    catch (e: CancellationException) { throw e }
    catch (e: Exception) { Result.failure(e) }
}
