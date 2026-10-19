package com.dscorp.ispadmin.presentation.ui.features.report

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private val TODAY = LocalDate.of(2026, 10, 18)

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1600dp")
class ReportsTest {
    @get:Rule val compose = createComposeRule()
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<ReportsGateway>()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `reports say what they contain with this and last month named`() {
        val items = reportItems(TODAY)

        assertEquals(7, items.size)
        assertEquals(ReportItem(DownloadDocumentType.DEBTORS_CUT_OFF_CANDIDATES, "Candidatos a corte", "Recibos impagos antes del día 15"), items[0])
        assertEquals("Recibos de setiembre aún sin pagar", items.first { it.type == DownloadDocumentType.DEBTORS_FROM_PAST_MONTH }.subtitle)
        assertEquals("Bajas de octubre 2026", items.first { it.type == DownloadDocumentType.CANCELLED_SUBSCRIPTIONS_FROM_CURRENT_MONTH }.subtitle)
        assertEquals("Bajas de diciembre 2026", reportItems(LocalDate.of(2027, 1, 5)).first { it.type == DownloadDocumentType.CANCELLED_SUBSCRIPTIONS_FROM_PAST_MONTH }.subtitle)
    }

    @Test
    fun `downloading marks the report as generating and hands over the file`() = runTest(dispatcher) {
        val document = DownloadDocumentResponse("Candidatos a corte", "xlsx", "UEsDBA==")
        coEvery { gateway.download(DownloadDocumentType.DEBTORS_CUT_OFF_CANDIDATES) } returns document
        val viewModel = ReportsViewModel(gateway, dispatcher) { TODAY }

        viewModel.onIntent(ReportsIntent.Download(DownloadDocumentType.DEBTORS_CUT_OFF_CANDIDATES))
        assertEquals(setOf(DownloadDocumentType.DEBTORS_CUT_OFF_CANDIDATES), viewModel.uiState.value.generating)
        advanceUntilIdle()

        assertEquals(emptySet<DownloadDocumentType>(), viewModel.uiState.value.generating)
        assertEquals(document, viewModel.uiState.value.ready)
        viewModel.onIntent(ReportsIntent.Opened)
        assertNull(viewModel.uiState.value.ready)
    }

    @Test
    fun `a failed report keeps a readable error`() = runTest(dispatcher) {
        coEvery { gateway.download(any()) } throws IllegalStateException("HTTP 500")
        val viewModel = ReportsViewModel(gateway, dispatcher) { TODAY }

        viewModel.onIntent(ReportsIntent.Download(DownloadDocumentType.CUT_OFF))
        advanceUntilIdle()

        assertEquals("No se pudo generar «Cortados». Revisa tu conexión y vuelve a intentar.", viewModel.uiState.value.error)
    }

    @Test
    fun `the list shows generating and starts a download`() {
        val tapped = mutableListOf<ReportsIntent>()
        compose.setContent {
            MaterialTheme {
                ReportsContent(ReportsUiState(items = reportItems(TODAY), generating = setOf(DownloadDocumentType.CUT_OFF)), onIntent = { tapped += it })
            }
        }

        compose.onNodeWithText("Se descargan en Excel (XLSX) y se abren con la app que elijas.").assertExists()
        compose.onNodeWithText("Generando…").assertExists()
        compose.onNodeWithText("Candidatos a corte").performClick()
        assertEquals(listOf(ReportsIntent.Download(DownloadDocumentType.DEBTORS_CUT_OFF_CANDIDATES)), tapped)
    }
}
