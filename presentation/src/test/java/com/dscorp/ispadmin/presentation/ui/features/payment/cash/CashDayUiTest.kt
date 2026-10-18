package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1400dp")
class CashDayUiTest {
    @get:Rule val compose = createComposeRule()
    private val actions = mutableListOf<CashAction>()

    private fun show(state: CashDayUiState) = compose.setContent {
        MaterialTheme { CashDayContent(state, onAction = { actions += it }) }
    }

    @Test
    fun `the day shows totals by method and the charges`() {
        show(CashDayUiState(day = CashDay.from(cashDay(), LocalDate.of(2026, 10, 14))))

        compose.onNodeWithText("Cobrado hoy · mié 14 oct").assertExists()
        compose.onNodeWithText("Por método").assertExists()
        compose.onNodeWithText("S/ 1,438.00").assertExists()
        compose.onNodeWithText("Jorge Ramos Villanueva").performClick()
        compose.onNodeWithText("Buscar pago, titular o pagador").performClick()
        compose.onNodeWithTag(CashDayTestTags.NEW_CHARGE).performClick()

        assertEquals(listOf(CashAction.OpenPayment(5), CashAction.Search, CashAction.NewCharge), actions)
    }

    @Test
    fun `the period chip offers yesterday`() {
        show(CashDayUiState(day = CashDay.from(cashDay(), LocalDate.of(2026, 10, 14))))

        compose.onNodeWithTag(CashDayTestTags.PERIOD).performClick()
        compose.onNodeWithText("Ayer").performClick()

        assertEquals(listOf(CashAction.Period(CashPeriod.YESTERDAY)), actions)
    }

    @Test
    fun `a failed load offers to retry`() {
        show(CashDayUiState(error = "No se pudo cargar la caja. Revisa tu conexión y vuelve a intentar."))

        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(listOf(CashAction.Retry), actions)
    }
}
