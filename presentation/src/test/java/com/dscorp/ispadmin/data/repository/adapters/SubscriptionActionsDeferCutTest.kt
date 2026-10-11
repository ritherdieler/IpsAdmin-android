package com.dscorp.ispadmin.data.repository.adapters

import com.dscorp.ispadmin.data.datasource.remote.CutDeferralBody
import com.dscorp.ispadmin.data.datasource.remote.RestApiServices
import com.dscorp.ispadmin.domain.usecase.service.DeferCutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SubscriptionActionsDeferCutTest {
    private val api = mockk<RestApiServices>()
    private val useCase = DeferCutUseCase(SubscriptionActionsRepositoryAdapter(mockk(relaxed = true), api))

    @Test
    fun `defer cut posts the date and the reason`() = runTest {
        coEvery { api.deferCut(9, any()) } returns Response.success(Unit)

        val result = useCase(9, "2026-10-25", "Paga el 25")

        assertTrue(result.isSuccess)
        coVerify { api.deferCut(9, CutDeferralBody(untilDate = "2026-10-25", reason = "Paga el 25")) }
    }

    @Test
    fun `defer cut failure carries the Core message`() = runTest {
        coEvery { api.deferCut(9, any()) } returns Response.error(
            400,
            """{"message":"La fecha debe ser posterior a hoy y dentro del mes en curso"}""".toResponseBody("application/json".toMediaType()),
        )

        val result = useCase(9, "2026-11-02", "Paga")

        assertEquals("La fecha debe ser posterior a hoy y dentro del mes en curso", result.exceptionOrNull()?.message)
    }
}
