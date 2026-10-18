package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CashDayViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<CashDayGateway>()
    private val today = LocalDate.of(2026, 10, 14)
    private lateinit var viewModel: CashDayViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.day(any()) } returns cashDay()
        viewModel = CashDayViewModel(gateway, dispatcher) { today }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `opening loads today and yesterday asks for the previous day`() = runTest(dispatcher) {
        viewModel.onIntent(CashIntent.Load)
        advanceUntilIdle()
        assertEquals("S/ 1,692.00", viewModel.uiState.value.day?.amount)

        coEvery { gateway.day(LocalDate.of(2026, 10, 13)) } returns cashDay("2026-10-13")
        viewModel.onIntent(CashIntent.SelectDate(today.minusDays(1)))
        advanceUntilIdle()

        assertEquals("Cobrado el mar 13 oct", viewModel.uiState.value.day?.title)
        coVerify { gateway.day(LocalDate.of(2026, 10, 13)) }
    }

    @Test
    fun `a failure keeps a readable error`() = runTest(dispatcher) {
        coEvery { gateway.day(any()) } throws IllegalStateException("HTTP 500")

        viewModel.onIntent(CashIntent.Load)
        advanceUntilIdle()

        assertEquals("No se pudo cargar la caja. Revisa tu conexión y vuelve a intentar.", viewModel.uiState.value.error)
    }
}
