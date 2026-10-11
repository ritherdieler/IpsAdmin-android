package com.dscorp.ispadmin.presentation.ui.features.dashboard

import com.dscorp.ispadmin.domain.model.dashboard.CollectionSummary
import com.dscorp.ispadmin.domain.repository.CollectionsRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<CollectionsRepository>()
    private lateinit var vm: CollectionsViewModel
    private val summary = CollectionSummary(7, BigDecimal("6840.00"), 214, 9, 3, 90, 72.0, 46, 39, BigDecimal("9412.00"), 287, BigDecimal("32.79"))

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = CollectionsViewModel(repository, dispatcher)
    }

    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `load publishes the collection summary`() = runTest(dispatcher) {
        coEvery { repository.summary() } returns Result.success(summary)

        vm.onIntent(CollectionsIntent.Load)
        advanceUntilIdle()

        assertEquals(summary, vm.uiState.value.summary)
        assertFalse(vm.uiState.value.failed)
    }

    @Test fun `failed refresh keeps the last summary visible`() = runTest(dispatcher) {
        coEvery { repository.summary() } returns Result.success(summary) andThen Result.failure(Exception("network"))

        vm.onIntent(CollectionsIntent.Load)
        advanceUntilIdle()
        vm.onIntent(CollectionsIntent.Load)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.failed)
        assertEquals(summary, vm.uiState.value.summary)
    }
}
