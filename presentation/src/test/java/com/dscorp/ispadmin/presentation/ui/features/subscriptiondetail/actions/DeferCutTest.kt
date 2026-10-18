package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.datasource.remote.CutDeferralResponse
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.receipt
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.subscription
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

private val TODAY = LocalDate.of(2026, 10, 14)
private fun billed(month: Int) = LocalDate.of(2026, month, 1).atStartOfDay(ZoneId.of("America/Lima")).toInstant().toEpochMilli()

private fun maria(status: ServiceStatus = ServiceStatus.ACTIVE) =
    subscription(status = status, pendingInvoices = 1, totalDebt = 50.0, firstName = "MARIA", lastName = "QUISPE HUAMAN")
private val SEPTEMBER = listOf(receipt(5, billed(9)), receipt(4, billed(8), paid = true))

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1600dp")
class DeferCutTest {
    @get:Rule val compose = createComposeRule()
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<DeferCutGateway>()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `dates stay inside the month and the summary names the debt`() {
        val model = DeferCut.from(maria(), SEPTEMBER, current = null, today = TODAY)

        assertEquals("Maria debe S/ 50.00 (set). Sin postergación entra en el próximo corte automático.", model.summary)
        assertEquals(
            listOf(DeferOption(LocalDate.of(2026, 10, 16), "Vie 16 oct", "en 2 días"), DeferOption(LocalDate.of(2026, 10, 19), "Lun 19 oct", "en 5 días"),
                DeferOption(LocalDate.of(2026, 10, 31), "Sáb 31 oct", "fin de mes")),
            model.options,
        )
        assertEquals(listOf("Vie 30 oct", "Sáb 31 oct"), DeferCut.from(maria(), SEPTEMBER, null, LocalDate.of(2026, 10, 28)).options.map { it.title })
        assertEquals(LocalDate.of(2026, 10, 15) to LocalDate.of(2026, 10, 31), model.pickRange)
        assertEquals("Queda fuera del corte automático hasta el lun 19 oct inclusive.", model.note(LocalDate.of(2026, 10, 19)))
        assertEquals("Postergar hasta el lun 19 oct", model.action(LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun `a cut subscription is restored and an existing deferral is replaced`() {
        val model = DeferCut.from(maria(ServiceStatus.CUT_OFF), SEPTEMBER, CutDeferralResponse(untilDate = "2026-10-16", reason = "Viaje"), TODAY)

        assertEquals("Maria debe S/ 50.00 (set). Está cortada: al postergar se le restablece el servicio.", model.summary)
        assertEquals("Ya tiene postergación hasta el vie 16 oct (Viaje). Al guardar se reemplaza.", model.currentNote)
        assertEquals("Queda fuera del corte automático hasta el lun 19 oct inclusive. Se restablece el servicio ahora.", model.note(LocalDate.of(2026, 10, 19)))
        assertNull(DeferCut.from(maria(), SEPTEMBER, null, LocalDate.of(2026, 10, 31)).pickRange)
    }

    @Test
    fun `saving sends the date and the reason with its detail`() = runTest(dispatcher) {
        coEvery { gateway.subscription(1842) } returns maria()
        coEvery { gateway.receipts(1842) } returns SEPTEMBER
        coEvery { gateway.current(1842) } returns null
        coEvery { gateway.defer(1842, "2026-10-19", "Promesa de pago · paga el sábado") } returns Unit
        val viewModel = DeferCutViewModel(gateway, dispatcher) { TODAY }

        viewModel.onIntent(DeferCutIntent.Load(1842))
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 10, 19), viewModel.uiState.value.date)
        viewModel.onIntent(DeferCutIntent.ReasonSelected("Promesa de pago"))
        viewModel.onIntent(DeferCutIntent.DetailChanged(" paga el sábado "))
        viewModel.onIntent(DeferCutIntent.Submit)
        advanceUntilIdle()

        coVerify { gateway.defer(1842, "2026-10-19", "Promesa de pago · paga el sábado") }
        assertEquals("Corte postergado hasta el lun 19 oct", viewModel.uiState.value.done)
    }

    @Test
    fun `without a reason there is nothing to save`() {
        val model = DeferCut.from(maria(), SEPTEMBER, null, TODAY)
        val intents = mutableListOf<DeferCutIntent>()
        compose.setContent {
            MaterialTheme { DeferCutContent(DeferCutUiState(model = model, date = LocalDate.of(2026, 10, 19)), onIntent = { intents += it }, onBack = {}, onPickDate = {}) }
        }

        compose.onNodeWithText("Hasta cuándo").assertExists()
        compose.onNodeWithText("Elige el motivo").assertExists()
        compose.onNodeWithTag(DeferCutTestTags.SUBMIT).assertIsNotEnabled()
        compose.onNodeWithText("Viaje").performClick()
        compose.onNodeWithText("Vie 16 oct").performClick()

        assertEquals(listOf(DeferCutIntent.ReasonSelected("Viaje"), DeferCutIntent.DateSelected(LocalDate.of(2026, 10, 16))), intents)
    }
}
