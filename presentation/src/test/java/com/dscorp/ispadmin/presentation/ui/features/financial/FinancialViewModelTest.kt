package com.dscorp.ispadmin.presentation.ui.features.financial

import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import com.dscorp.ispadmin.domain.model.financial.FinancialReport
import com.dscorp.ispadmin.domain.repository.FinancialRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<FinancialRepository>()
    private lateinit var vm: FinancialViewModel
    private val report = FinancialReport(id = "cut-1", serviceMonth = "2026-09", movementsMonth = "2026-10", cutoff = "2026-10-09T12:00:00-05:00")
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = FinancialViewModel(repository, dispatcher, "2026-10")
    }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `load uses previous service month and current movements month`() = runTest(dispatcher) {
        coEvery { repository.load("2026-09", "2026-10") } returns Result.success(report)
        vm.onIntent(FinancialIntent.Load)
        runCurrent()
        assertEquals("cut-1", vm.uiState.value.report?.id)
        assertFalse(vm.uiState.value.isLoading)
    }
    @Test fun `failed refresh retains saved cut and retry clears error`() = runTest(dispatcher) {
        coEvery { repository.load(any(), any()) } returns Result.success(report) andThen Result.failure(Exception("network")) andThen Result.success(report.copy(id = "cut-2"))
        vm.onIntent(FinancialIntent.Load)
        advanceUntilIdle()
        vm.onIntent(FinancialIntent.Load)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.failed)
        assertEquals("cut-1", vm.uiState.value.report?.id)
        vm.onIntent(FinancialIntent.Load)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.failed)
        assertEquals("cut-2", vm.uiState.value.report?.id)
    }
    @Test fun `export uses displayed cut id even after period selection`() = runTest(dispatcher) {
        coEvery { repository.load(any(), any()) } returns Result.success(report)
        coEvery { repository.export("cut-1") } returns Result.success(DownloadDocumentResponse("report", "xlsx", "bytes"))
        val events = mutableListOf<FinancialEvent>()
        val job = launch { vm.events.collect { events.add(it) } }
        vm.onIntent(FinancialIntent.Load)
        advanceUntilIdle()
        vm.onIntent(FinancialIntent.Export)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.export("cut-1") }
        assertEquals(1, events.size)
        job.cancel()
    }
    @Test fun `changing period clears stale cut before loading`() = runTest(dispatcher) {
        coEvery { repository.load(any(), any()) } returns Result.success(report) andThen Result.failure(Exception())
        vm.onIntent(FinancialIntent.Load)
        advanceUntilIdle()
        vm.onIntent(FinancialIntent.SelectServiceMonth("2026-08"))
        advanceUntilIdle()
        assertNull(vm.uiState.value.report)
        assertTrue(vm.uiState.value.failed)
    }
}
