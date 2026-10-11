package com.dscorp.ispadmin.presentation.ui.features.financial

import com.dscorp.ispadmin.data.datasource.remote.financial.*
import com.dscorp.ispadmin.data.repository.FinancialRepositoryImpl
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class FinancialRepositoryTest {
    private val api = mockk<FinancialApi>()
    private val repository = FinancialRepositoryImpl(api)
    @Test fun `typed report preserves nullable unavailable amounts and exact decimal money`() = runTest {
        val dto = Gson().fromJson("""{"id":"cut-1","serviceMonth":"2026-09","movementsMonth":"2026-10","cutoff":"2026-10-09T12:00:00-05:00","movements":{"received":100.10,"expenses":null,"partialBalance":null,"previousServiceRecovery":90.05}}""", FinancialReportDto::class.java)
        coEvery { api.report(any(), any()) } returns Response.success(dto)
        val result = repository.load("2026-09", "2026-10").getOrThrow()
        assertEquals("100.10".toBigDecimal(), result.movements!!.received)
        assertNull(result.movements.expenses)
        assertNull(result.budgetTotal)
    }
    @Test fun `forbidden response stays a failure and malformed cut is rejected`() = runTest {
        coEvery { api.report(any(), any()) } returns Response.error(403, "".toResponseBody())
        assertTrue(repository.load("2026-09", "2026-10").isFailure)
        coEvery { api.report(any(), any()) } returns Response.success(FinancialReportDto())
        assertTrue(repository.load("2026-09", "2026-10").isFailure)
    }
}
