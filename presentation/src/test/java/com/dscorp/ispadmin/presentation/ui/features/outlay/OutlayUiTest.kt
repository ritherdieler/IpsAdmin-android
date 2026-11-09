package com.dscorp.ispadmin.presentation.ui.features.outlay

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.domain.model.OutlayType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1600dp")
class OutlayUiTest {
    @get:Rule val compose = createComposeRule()
    private val intents = mutableListOf<OutlayIntent>()
    private val today = LocalDate.of(2026, 10, 14)

    private val variable = Outlay(amount = "85", description = "Combustible", category = "Transporte")
    private val summary = OutlaySummary(2640.0, 3000.0, 360.0, 2500.0, 4150.0, 4100.0)
    private val withFixed = OCTOBER + outlay(5, 1800.0, "Alquiler del local", "Alquiler", null, "Rosa Ccori", 5, type = "FIXED")

    private fun form(state: OutlayUiState) = compose.setContent {
        MaterialTheme { RegisterOutLayForm(state = state, onIntent = { intents += it }, onImageClick = {}) }
    }

    // ---- Nuevo egreso ----

    @Test
    fun `a variable outlay picks its category with chips and the receipt is optional`() {
        form(OutlayUiState(outlay = variable))

        compose.onNodeWithText("Foto del comprobante").assertExists()
        compose.onNodeWithText("Opcional. Adjúntala si tienes boleta o factura.").assertExists()
        compose.onNodeWithText("Tomar foto").assertExists()
        compose.onNodeWithText("Galería").assertExists()
        compose.onNodeWithText("Registrar egreso").assertIsEnabled()
        compose.onNodeWithText("Suministros").performClick()

        assertEquals(listOf(OutlayIntent.UpdateCategory("Suministros")), intents)
    }

    @Test
    fun `the form no longer asks for a cost centre, a document number or a date`() {
        form(OutlayUiState(outlay = variable))

        compose.onNodeWithText("Centro de costo *").assertDoesNotExist()
        compose.onNodeWithText("N.º de documento").assertDoesNotExist()
        compose.onNodeWithText("Fecha").assertDoesNotExist()
    }

    @Test
    fun `a profile without permission does not see the type selector`() {
        form(OutlayUiState(outlay = variable, canChooseType = false))

        compose.onNodeWithText("Tipo *").assertDoesNotExist()
    }

    @Test
    fun `admin chooses fixed and the form drops the receipt and explains the cap`() {
        form(OutlayUiState(outlay = Outlay(amount = "1800", description = "Alquiler", category = "Alquiler", type = OutlayType.FIXED), canChooseType = true))

        compose.onNodeWithText("Tipo *").assertExists()
        compose.onNodeWithText("Foto del comprobante").assertDoesNotExist()
        compose.onNodeWithText("Los egresos fijos no piden comprobante y no cuentan para el límite mensual de variables.").assertExists()
        compose.onNodeWithText("Registrar egreso fijo").assertIsEnabled()
        compose.onNode(hasText("Variable") and isSelectable()).performClick()

        assertEquals(listOf(OutlayIntent.UpdateType(OutlayType.VARIABLE)), intents)
    }

    @Test
    fun `the note says how much is left of the month`() {
        form(OutlayUiState(outlay = variable, summary = summary))

        compose.onNodeWithText("de S/ 3,000.00 para egresos variables en", substring = true).assertExists()
    }

    @Test
    fun `an amount above what is left shows the error and the reason on a disabled button`() {
        form(OutlayUiState(outlay = variable.copy(amount = "500"), summary = summary))

        compose.onNodeWithText("Supera lo disponible", substring = true).assertExists()
        compose.onNodeWithText("El monto supera lo disponible del mes").assertExists()
        compose.onNodeWithText("Registrar egreso").assertIsNotEnabled()
    }

    @Test
    fun `while saving the button says so and cannot be pressed again`() {
        form(OutlayUiState(outlay = variable, isLoading = true))

        compose.onNodeWithText("Registrando…").assertIsNotEnabled()
    }

    // ---- Egresos ----

    @Test
    fun `the month list opens a new outlay and an outlay`() {
        var created = false
        var opened: Int? = null
        compose.setContent {
            MaterialTheme {
                OutlaysContent(
                    OutlaysUiState(month = OutlayMonth.from(OCTOBER, null, today)),
                    onNew = { created = true }, onRetry = {}, onOpen = { opened = it },
                )
            }
        }

        compose.onNodeWithText("S/ 469.00").assertExists()
        compose.onNodeWithText("Combustible de la moto").performClick()
        compose.onNodeWithTag(OutlayTestTags.NEW).performClick()

        assertEquals(1, opened)
        assertTrue(created)
    }

    @Test
    fun `the month card shows the cap, what is left and the fixed total, and the segments filter by type`() {
        var type: OutlayType? = null
        compose.setContent {
            MaterialTheme {
                OutlaysContent(
                    OutlaysUiState(month = OutlayMonth.from(withFixed, null, today), summary = summary),
                    onNew = {}, onRetry = {}, onFilterType = { type = it },
                )
            }
        }

        compose.onNodeWithText("Cerca del límite").assertExists()
        compose.onNodeWithText("Disponible S/ 360.00").assertExists()
        compose.onNodeWithText("88% usado · 4 egresos").assertExists()
        compose.onNodeWithText("Sin límite").assertExists()
        compose.onNode(hasText("Fijos") and isSelectable()).performClick()

        assertEquals(OutlayType.FIXED, type)
    }

    @Test
    fun `the fixed filter compares the fixed outlays with the month before`() {
        compose.setContent {
            MaterialTheme {
                OutlaysContent(
                    OutlaysUiState(
                        month = OutlayMonth.from(withFixed, null, today, OutlayType.FIXED),
                        typeFilter = OutlayType.FIXED,
                        summary = summary.copy(fixedTotal = 4150.0, previousFixedTotal = 4100.0),
                    ),
                    onNew = {}, onRetry = {},
                )
            }
        }

        compose.onNodeWithText("set 2026: S/ 4,100.00").assertExists()
        compose.onNodeWithText("▲ S/ 50.00 (1.2%)").assertExists()
        compose.onNodeWithText("Alquiler del local").assertExists()
    }

    @Test
    fun `without fixed outlays only a profile that may register them gets the action`() {
        val empty = OutlayMonth.from(OCTOBER, null, today, OutlayType.FIXED)
        compose.setContent {
            MaterialTheme {
                OutlaysContent(
                    OutlaysUiState(month = empty, typeFilter = OutlayType.FIXED, canRegisterFixed = false),
                    onNew = {}, onRetry = {},
                )
            }
        }

        compose.onNodeWithText("Sin egresos fijos en oct 2026").assertExists()
        compose.onNodeWithTag(OutlayTestTags.EMPTY_ACTION).assertDoesNotExist()
    }

    @Test
    fun `an admin without fixed outlays can register one from the empty state`() {
        var created = false
        val empty = OutlayMonth.from(OCTOBER, null, today, OutlayType.FIXED)
        compose.setContent {
            MaterialTheme {
                OutlaysContent(
                    OutlaysUiState(month = empty, typeFilter = OutlayType.FIXED, canRegisterFixed = true),
                    onNew = { created = true }, onRetry = {},
                )
            }
        }

        compose.onNodeWithText("Registrar egreso fijo").performClick()

        assertTrue(created)
    }

    @Test
    fun `the result of a saved outlay shows as a snackbar and replaces the new outlay button`() {
        compose.setContent {
            MaterialTheme {
                OutlaysContent(
                    OutlaysUiState(month = OutlayMonth.from(OCTOBER, null, today)),
                    onNew = {}, onRetry = {}, snackbar = AnnotatedString("Egreso registrado."),
                )
            }
        }

        compose.onNodeWithText("Egreso registrado.").assertExists()
        compose.onNodeWithTag(OutlayTestTags.NEW).assertDoesNotExist()
    }
}
