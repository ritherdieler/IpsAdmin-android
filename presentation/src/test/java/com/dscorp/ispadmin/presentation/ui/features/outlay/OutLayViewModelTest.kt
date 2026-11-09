package com.dscorp.ispadmin.presentation.ui.features.outlay

import android.net.Uri
import com.dscorp.ispadmin.domain.model.OutlaySaveResult
import com.dscorp.ispadmin.domain.model.OutlayType
import com.dscorp.ispadmin.domain.repository.OutlayReceiptPreparer
import com.dscorp.ispadmin.domain.repository.OutlayRepository
import com.dscorp.ispadmin.domain.usecase.outlay.RegisterOutlayUseCase
import com.dscorp.ispadmin.observability.ObservabilityClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class OutLayViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<OutlayRepository>()
    private val preparer = mockk<OutlayReceiptPreparer>()
    private val summaries = mockk<OutlaySummaryGateway>()
    private val summary = OutlaySummary(2640.0, 3000.0, 360.0, 2500.0, 4150.0, 4100.0)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.currentResponsibleUserId() } returns 7
        every { repository.canRegisterFixedOutlay() } returns false
        coEvery { summaries.summary() } returns summary
        coEvery { repository.saveOutlay(any(), any()) } returns OutlaySaveResult(id = 5, teamNotified = true)
        coEvery { preparer.prepareCompressedReceipts(any()) } returns emptyList<File>()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        OutLayViewModel(RegisterOutlayUseCase(repository, preparer), repository, summaries, mockk<ObservabilityClient>(relaxed = true))

    private fun OutLayViewModel.fill(amount: String = "85") {
        handleIntent(OutlayIntent.UpdateAmount(amount))
        handleIntent(OutlayIntent.UpdateDescription("Combustible"))
        handleIntent(OutlayIntent.UpdateCategory("Transporte"))
        handleIntent(OutlayIntent.UpdateCostCenter("Técnico"))
    }

    @Test
    fun `starts as a variable outlay and only admin or accountant can choose the type`() {
        assertEquals(OutlayType.VARIABLE, viewModel().uiState.value.type)
        assertFalse(viewModel().uiState.value.canChooseType)

        every { repository.canRegisterFixedOutlay() } returns true
        assertTrue(viewModel().uiState.value.canChooseType)
    }

    @Test
    fun `a profile without permission cannot switch to fixed`() {
        val vm = viewModel()
        vm.handleIntent(OutlayIntent.UpdateType(OutlayType.FIXED))
        assertEquals(OutlayType.VARIABLE, vm.uiState.value.type)
    }

    @Test
    fun `switching to fixed drops the receipt photo`() {
        every { repository.canRegisterFixedOutlay() } returns true
        val vm = viewModel()
        vm.handleIntent(OutlayIntent.TakeImage(Uri.parse("content://a")))

        vm.handleIntent(OutlayIntent.UpdateType(OutlayType.FIXED))

        assertEquals(OutlayType.FIXED, vm.uiState.value.type)
        assertTrue(vm.uiState.value.photoList.isEmpty())
    }

    @Test
    fun `an amount above what is available blocks a variable outlay with a reason`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.handleIntent(OutlayIntent.LoadLimit)
        advanceUntilIdle()
        vm.fill("500")

        val state = vm.uiState.value
        assertEquals("Supera lo disponible por S/ 140.00. Ingresa S/ 360.00 o menos, o pide a un administrador que lo registre.", state.amountError)
        assertEquals("El monto supera lo disponible del mes", state.disabledReason)
        assertFalse(state.canSubmit)
    }

    @Test
    fun `the exact available amount is allowed and the photo is optional`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.handleIntent(OutlayIntent.LoadLimit)
        advanceUntilIdle()
        vm.fill("360")

        assertNull(vm.uiState.value.amountError)
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `the limit never blocks a fixed outlay and it needs no cost centre`() = runTest(dispatcher) {
        every { repository.canRegisterFixedOutlay() } returns true
        val vm = viewModel()
        vm.handleIntent(OutlayIntent.LoadLimit)
        advanceUntilIdle()
        vm.handleIntent(OutlayIntent.UpdateType(OutlayType.FIXED))
        vm.handleIntent(OutlayIntent.UpdateAmount("1800"))
        vm.handleIntent(OutlayIntent.UpdateDescription("Alquiler"))
        vm.handleIntent(OutlayIntent.UpdateCategory("Alquiler"))

        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `without the summary the outlay can still be sent and the server decides`() = runTest(dispatcher) {
        coEvery { summaries.summary() } throws IllegalStateException("HTTP 404")
        val vm = viewModel()
        vm.handleIntent(OutlayIntent.LoadLimit)
        advanceUntilIdle()
        vm.fill("5000")

        assertNull(vm.uiState.value.summary)
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `registering sends the chosen type without a photo`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.fill()
        vm.handleIntent(OutlayIntent.RegisterOutLay)
        advanceUntilIdle()

        coVerify { repository.saveOutlay(match { it.type == OutlayType.VARIABLE && it.responsibleId == 7 }, emptyList()) }
        assertTrue(vm.uiState.value.isSaved)
    }
}
