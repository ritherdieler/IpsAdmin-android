package com.dscorp.ispadmin.presentation.ui.features.outlay

import io.mockk.coEvery
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
class OutlaysViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<OutlaysGateway>()
    private lateinit var viewModel: OutlaysViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.currentMonth() } returns OCTOBER
        viewModel = OutlaysViewModel(gateway, dispatcher) { LocalDate.of(2026, 10, 14) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads the month and filters by category`() = runTest(dispatcher) {
        viewModel.onIntent(OutlaysIntent.Load)
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.month?.rows?.size)

        viewModel.onIntent(OutlaysIntent.Filter("Otros"))
        assertEquals(listOf(4), viewModel.uiState.value.month?.rows?.map { it.id })
        assertEquals("S/ 469.00", viewModel.uiState.value.month?.total)
    }

    @Test
    fun `a failure keeps a readable error`() = runTest(dispatcher) {
        coEvery { gateway.currentMonth() } throws IllegalStateException("HTTP 500")

        viewModel.onIntent(OutlaysIntent.Load)
        advanceUntilIdle()

        assertEquals("No se pudieron cargar los egresos. Revisa tu conexión y vuelve a intentar.", viewModel.uiState.value.error)
    }
}
