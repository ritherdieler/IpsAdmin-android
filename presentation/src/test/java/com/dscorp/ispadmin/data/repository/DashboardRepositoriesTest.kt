package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.datasource.remote.dashboard.ClientsDashboardApi
import com.dscorp.ispadmin.data.datasource.remote.dashboard.CollectionSummaryDto
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class DashboardRepositoriesTest {
    private val api = mockk<ClientsDashboardApi>(relaxed = true)

    @Test fun `clients dashboard asks for the last six months of ticket history`() = runTest {
        ClientsDashboardRepositoryImpl(api).load()

        coVerify { api.assistanceTickets(6) }
    }

    @Test fun `collection summary keeps exact money and unknown ratios`() = runTest {
        val dto = Gson().fromJson(
            """{"dueSoonDays":7,"dueSoonAmount":6840.10,"dueSoonInvoices":214,"activeDeferrals":9,"deferralsExpiringToday":3,
               "deferralWindowDays":90,"deferralsPaid":0,"deferralsExpired":0,"deferralsKeptPercent":null,"cutsForDebt":46,
               "restoredAfterPayment":39,"overdueAmount":9412.00,"delinquentSubscriptions":287,"overduePerDelinquent":32.79}""",
            CollectionSummaryDto::class.java,
        )
        coEvery { api.collections() } returns Response.success(dto)

        val summary = CollectionsRepositoryImpl(api).summary().getOrThrow()

        assertEquals("6840.10".toBigDecimal(), summary.dueSoonAmount)
        assertEquals(214L, summary.dueSoonInvoices)
        assertNull(summary.deferralsKeptPercent)
        assertEquals("32.79".toBigDecimal(), summary.overduePerDelinquent)
        assertEquals(287L, summary.delinquentSubscriptions)
    }

    @Test fun `forbidden collections stay a failure`() = runTest {
        coEvery { api.collections() } returns Response.error(403, "".toResponseBody())

        assertTrue(CollectionsRepositoryImpl(api).summary().isFailure)
    }
}
