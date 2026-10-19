package com.dscorp.ispadmin.presentation.ui.features.plan

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.datasource.remote.PlanCountResponse
import com.dscorp.ispadmin.data.datasource.remote.PlanSaveRequest
import com.dscorp.ispadmin.domain.model.InstallationType
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

private val BASICO = PlanCountResponse(1, "basico", 50.0, 200, 200, InstallationType.FIBER, true, 813)
private val DUO = PlanCountResponse(2, "Duo basico 80", 80.0, 200, 200, InstallationType.FIBER, true, 119)
private val RADIO = PlanCountResponse(5, "Basico wireless 50", 50.0, 20, 20, InstallationType.WIRELESS, true, 229)
private val TV = PlanCountResponse(6, "Cable basico", 30.0, 0, 0, InstallationType.ONLY_TV_FIBER, true, 80)
private val OLD = PlanCountResponse(9, "Viejo 30", 30.0, 10, 10, InstallationType.FIBER, false, 0)
private val ALL = listOf(RADIO, OLD, TV, DUO, BASICO)

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1600dp")
class PlanCatalogTest {
    @get:Rule val compose = createComposeRule()
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<PlanCatalogGateway>()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `plans are grouped by type, most used first, with active and retired tabs`() {
        val catalog = PlanCatalog.from(ALL)

        assertEquals(4 to 1, catalog.activeCount to catalog.retiredCount)
        val active = catalog.groups(retired = false)
        assertEquals(listOf("Fibra · 2", "Inalámbrico · 1", "Solo TV · 1"), active.map { it.title })
        assertEquals(PlanRow(1, "Basico", "S/ 50.00", "200/200 Mbps · 813 abonados"), active[0].rows[0])
        assertEquals("TV cable, sin internet · 80 abonados", active[2].rows[0].detail)
        assertEquals(listOf("Viejo 30"), catalog.groups(retired = true).flatMap { it.rows }.map { it.name })
    }

    @Test
    fun `the editor locks the type, warns about subscribers and knows what changed`() {
        val editor = PlanEditor.edit(BASICO)

        assertEquals("No se puede cambiar: tiene abonados", editor.typeNote)
        assertEquals("813 abonados usan este plan. Cambiar el precio cambia su mensualidad y la velocidad actualiza su cola en el MikroTik.", editor.warning)
        assertEquals("Se muestra como “Basico”", editor.preview)
        assertNull(editor.blockingReason)
        assertEquals("Escribe el precio", editor.copy(price = "").blockingReason)
        assertEquals("La bajada y la subida deben ser mayores a 0", editor.copy(download = "0").blockingReason)
        assertEquals(false, editor.changed)
        assertEquals(true, editor.copy(price = "55").changed)
        assertEquals(PlanEditor.new(InstallationType.WIRELESS).typeNote, null)
    }

    @Test
    fun `saving updates the plan and its availability`() = runTest(dispatcher) {
        coEvery { gateway.plans() } returns ALL
        coEvery { gateway.update(any()) } returns Unit
        coEvery { gateway.setActive(1, false) } returns Unit
        val viewModel = PlanCatalogViewModel(gateway, dispatcher)
        viewModel.onIntent(PlanCatalogIntent.Load)
        advanceUntilIdle()

        viewModel.onIntent(PlanCatalogIntent.Edit(1))
        viewModel.onIntent(PlanCatalogIntent.EditorChanged { it.copy(price = "55", available = false) })
        viewModel.onIntent(PlanCatalogIntent.Save)
        advanceUntilIdle()

        coVerify { gateway.update(PlanSaveRequest(1, "basico", 55.0, 200, 200, InstallationType.FIBER)) }
        coVerify { gateway.setActive(1, false) }
        assertNull(viewModel.uiState.value.editor)
        assertEquals("Plan Basico guardado", viewModel.uiState.value.message)
    }

    @Test
    fun `a new plan is created with its type`() = runTest(dispatcher) {
        coEvery { gateway.plans() } returns ALL
        coEvery { gateway.create(any()) } returns Unit
        val viewModel = PlanCatalogViewModel(gateway, dispatcher)
        viewModel.onIntent(PlanCatalogIntent.Load)
        advanceUntilIdle()

        viewModel.onIntent(PlanCatalogIntent.New)
        viewModel.onIntent(PlanCatalogIntent.EditorChanged { it.copy(name = "Internet 600", price = "180", download = "600", upload = "600") })
        viewModel.onIntent(PlanCatalogIntent.Save)
        advanceUntilIdle()

        coVerify { gateway.create(PlanSaveRequest(null, "Internet 600", 180.0, 600, 600, InstallationType.FIBER)) }
    }

    @Test
    fun `the list opens a plan and the editor cannot save without changes`() {
        var opened: Int? = null
        compose.setContent {
            MaterialTheme { PlanCatalogContent(PlanCatalogUiState(catalog = PlanCatalog.from(ALL)), onIntent = { if (it is PlanCatalogIntent.Edit) opened = it.planId }) }
        }
        compose.onNodeWithText("Fibra · 2").assertExists()
        compose.onNodeWithText("Retirados · 1").assertExists()
        compose.onNodeWithText("Duo basico 80").performClick()
        assertEquals(2, opened)
    }

    @Test
    fun `the editor shows the type lock and the save state`() {
        compose.setContent { MaterialTheme { PlanEditorContent(PlanEditor.edit(BASICO), saving = false, error = null, onChange = {}, onSave = {}, onRetire = {}) } }

        compose.onNodeWithText("No se puede cambiar: tiene abonados").assertExists()
        compose.onNodeWithText("Se muestra como “Basico”").assertExists()
        compose.onNodeWithTag(PlanCatalogTestTags.SAVE).assertIsNotEnabled()
        compose.onNodeWithText("Retirar plan").assertExists()
    }
}
