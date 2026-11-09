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
import com.dscorp.ispadmin.domain.model.OutlayType
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OutlaysViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<OutlaysGateway>()
    private val summaries = mockk<OutlaySummaryGateway>()
    private val summary = OutlaySummary(2640.0, 3000.0, 360.0, 2500.0, 4150.0, 4100.0)
    private lateinit var viewModel: OutlaysViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.currentMonth() } returns OCTOBER + outlay(5, 1800.0, "Alquiler", "Alquiler", null, "Rosa Ccori", 5, type = "FIXED")
        coEvery { summaries.summary() } returns summary
        viewModel = OutlaysViewModel(gateway, summaries, canRegisterFixed = true, mainImmediate = dispatcher) { LocalDate.of(2026, 10, 14) }
    }

    @Test
    fun `loads the month summary next to the list`() = runTest(dispatcher) {
        viewModel.onIntent(OutlaysIntent.Load)
        advanceUntilIdle()

        assertEquals(summary, viewModel.uiState.value.summary)
    }

    @Test
    fun `the list still loads when the summary is not available`() = runTest(dispatcher) {
        coEvery { summaries.summary() } throws IllegalStateException("HTTP 404")

        viewModel.onIntent(OutlaysIntent.Load)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.summary)
        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(5, viewModel.uiState.value.month?.rows?.size)
    }

    @Test
    fun `knows whether the profile may register fixed outlays and gives the detail of a listed outlay`() = runTest(dispatcher) {
        viewModel.onIntent(OutlaysIntent.Load)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.canRegisterFixed)
        assertEquals("Alquiler", viewModel.detailOf(5)?.description)
        assertEquals(null, viewModel.detailOf(999))
    }

    @Test
    fun `the type filter keeps only fixed outlays`() = runTest(dispatcher) {
        viewModel.onIntent(OutlaysIntent.Load)
        advanceUntilIdle()

        viewModel.onIntent(OutlaysIntent.FilterType(OutlayType.FIXED))

        assertEquals(listOf(5), viewModel.uiState.value.month?.rows?.map { it.id })
        assertEquals(OutlayType.FIXED, viewModel.uiState.value.typeFilter)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads the month and filters by category`() = runTest(dispatcher) {
        viewModel.onIntent(OutlaysIntent.Load)
        advanceUntilIdle()
        assertEquals(5, viewModel.uiState.value.month?.rows?.size)

        viewModel.onIntent(OutlaysIntent.Filter("Otros"))
        assertEquals(listOf(4), viewModel.uiState.value.month?.rows?.map { it.id })
        assertEquals("S/ 2,269.00", viewModel.uiState.value.month?.total)
    }

    @Test
    fun `a failure keeps a readable error`() = runTest(dispatcher) {
        coEvery { gateway.currentMonth() } throws IllegalStateException("HTTP 500")

        viewModel.onIntent(OutlaysIntent.Load)
        advanceUntilIdle()

        assertEquals("No se pudieron cargar los egresos. Revisa tu conexión y vuelve a intentar.", viewModel.uiState.value.error)
    }
}
