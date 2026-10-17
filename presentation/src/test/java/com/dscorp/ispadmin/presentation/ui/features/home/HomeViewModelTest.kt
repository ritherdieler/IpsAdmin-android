package com.dscorp.ispadmin.presentation.ui.features.home

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<HomeGateway>()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.summary() } returns homeSummary()
        viewModel = HomeViewModel(gateway, mainImmediate = dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `load shows the secretary home once`() = runTest(dispatcher) {
        viewModel.onIntent(HomeIntent.Load)
        advanceUntilIdle()
        viewModel.onIntent(HomeIntent.Load)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("S/ 1,692.00", state.home?.todayAmount)
        assertFalse(state.loading)
        coVerify(exactly = 1) { gateway.summary() }
    }

    @Test
    fun `refresh reloads and a failure keeps the last data with a readable error`() = runTest(dispatcher) {
        viewModel.onIntent(HomeIntent.Load)
        advanceUntilIdle()
        coEvery { gateway.summary() } throws IllegalStateException("HTTP 500")

        viewModel.onIntent(HomeIntent.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("S/ 1,692.00", state.home?.todayAmount)
        assertEquals("No se pudo actualizar el inicio. Revisa tu conexión y vuelve a intentar.", state.error)

        coEvery { gateway.summary() } returns homeSummary()
        viewModel.onIntent(HomeIntent.Refresh)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
    }
}
