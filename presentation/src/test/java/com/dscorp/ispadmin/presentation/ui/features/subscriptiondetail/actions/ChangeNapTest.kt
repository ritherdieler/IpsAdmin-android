package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.datasource.remote.NapOptionResponse
import com.dscorp.ispadmin.data.datasource.remote.NapMoveApi
import com.dscorp.ispadmin.data.datasource.remote.NapOptionsResponse
import com.dscorp.ispadmin.data.utils.EndpointUnavailableException
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

internal val NAP_OPTIONS = NapOptionsResponse(
    placeName = "La Villa", currentNapId = 13, currentPort = 7,
    options = listOf(
        NapOptionResponse(11, "LV-15-1", "frente a la torre de claro", 4, 60, 16, listOf(1, 2, 3, 4)),
        NapOptionResponse(13, "LV-15-3", "poste de interseccion de calles", 4, 0, 16, listOf(1, 2, 3, 5, 6, 8)),
        NapOptionResponse(12, "LV-15-2", "frente a una tienda", 4, 90, 16, listOf(1, 2, 3)),
        NapOptionResponse(32, "LV-3-2", "cruce principal", 9, 260, 16, (1..16).toList()),
        NapOptionResponse(16, "LV-1-6", "caja en la curva", 9, 1340, 16, listOf(1, 2)),
    ),
)

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1600dp")
class ChangeNapTest {
    @get:Rule val compose = createComposeRule()
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<ChangeNapGateway>()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `boxes read with distance and free ports, sorted three ways`() {
        val model = ChangeNap.from(NAP_OPTIONS)

        assertEquals("LV-15-3 · puerto 7", model.current)
        assertEquals("poste de interseccion de calles", model.currentAddress)
        assertEquals("La Villa · 5 cajas · distancia desde la ubicación del titular", model.header)
        assertEquals("La Villa · 5 cajas · distancia desde la ubicación del titular", ChangeNap.from(NAP_OPTIONS.copy(placeName = "la villa")).header)
        val nearest = model.rows(NapSort.NEAREST)
        assertEquals(listOf("LV-15-1", "LV-15-2", "LV-3-2", "LV-1-6"), nearest.map { it.code })
        assertEquals(NapRow(11, "LV-15-1", "frente a la torre de claro", "60 m", "12 libres", enabled = true), nearest[0])
        assertEquals(NapRow(32, "LV-3-2", "cruce principal", "260 m", "Sin puertos", enabled = false), nearest[2])
        assertEquals("1.3 km", nearest[3].distance)
        assertEquals(listOf("LV-1-6", "LV-15-2", "LV-15-1", "LV-3-2"), model.rows(NapSort.FREEST).map { it.code })
        assertEquals(listOf("LV-15-1", "LV-15-2"), model.rows(NapSort.SAME_MUFA).map { it.code })
    }

    @Test
    fun `the port grid marks taken, free and chosen ports`() {
        val model = ChangeNap.from(NAP_OPTIONS)

        val ports = model.ports(11, chosen = 5)
        assertEquals(16, ports.size)
        assertEquals(PortState.TAKEN, ports[0].state)
        assertEquals(PortState.CHOSEN, ports[4].state)
        assertEquals(PortState.FREE, ports[5].state)
        assertEquals(5, model.firstFree(11))
        assertNull(model.firstFree(32))
        assertEquals("12 libres de 16", model.freeOf(11))
        assertEquals("Se libera el puerto 7 de LV-15-3.", model.releaseNote)
    }

    @Test
    fun `choosing a box picks its first free port and moving reports it`() = runTest(dispatcher) {
        coEvery { gateway.options(1842) } returns NAP_OPTIONS
        coEvery { gateway.move(1842, 12, 4) } returns "LV-15-2 · puerto 4"
        val viewModel = ChangeNapViewModel(gateway, dispatcher)

        viewModel.onIntent(ChangeNapIntent.Load(1842))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.onIntent(ChangeNapIntent.NapSelected(12))
        assertEquals(4, viewModel.uiState.value.port)
        assertEquals("Mover a LV-15-2 · puerto 4", viewModel.uiState.value.action)
        viewModel.onIntent(ChangeNapIntent.Submit)
        advanceUntilIdle()

        coVerify { gateway.move(1842, 12, 4) }
        assertEquals("Caja cambiada a LV-15-2 · puerto 4", viewModel.uiState.value.done)
    }

    @Test
    fun `a taken port or a failed move keeps the screen with the reason`() = runTest(dispatcher) {
        coEvery { gateway.options(1842) } returns NAP_OPTIONS
        coEvery { gateway.move(any(), any(), any()) } throws IllegalStateException("HTTP 409")
        val viewModel = ChangeNapViewModel(gateway, dispatcher)
        viewModel.onIntent(ChangeNapIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(ChangeNapIntent.NapSelected(11))
        viewModel.onIntent(ChangeNapIntent.PortSelected(2))
        assertEquals(5, viewModel.uiState.value.port)
        viewModel.onIntent(ChangeNapIntent.Submit)
        advanceUntilIdle()

        assertEquals("No se pudo mover la ONU. Revisa tu conexión y vuelve a intentar.", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.done)
    }

    @Test
    fun `a Core without NAP options says the change is not available yet`() = runTest(dispatcher) {
        coEvery { gateway.options(1842) } throws EndpointUnavailableException()
        val viewModel = ChangeNapViewModel(gateway, dispatcher)

        viewModel.onIntent(ChangeNapIntent.Load(1842))
        advanceUntilIdle()

        assertEquals("Cambiar caja NAP no está disponible todavía en este servidor.", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.model)
    }

    @Test
    fun `the api gateway reads Spring's default 404 as a missing endpoint`() = runTest(dispatcher) {
        val api = mockk<NapMoveApi>()
        coEvery { api.options(1842) } throws HttpException(
            Response.error<NapOptionsResponse>(
                404,
                """{"status":404,"error":"Not Found","message":"No message available","path":"/ispadmin/subscription/1842/nap-options"}"""
                    .toResponseBody("application/json".toMediaType()),
            ),
        )
        coEvery { api.options(7) } throws HttpException(
            Response.error<NapOptionsResponse>(404, """{"code":"NOT_FOUND","message":"Suscripción no encontrada"}""".toResponseBody("application/json".toMediaType())),
        )

        val missing = runCatching { ApiChangeNapGateway(api).options(1842) }.exceptionOrNull()
        val notFound = runCatching { ApiChangeNapGateway(api).options(7) }.exceptionOrNull()

        assertEquals(true, missing is EndpointUnavailableException)
        assertEquals(true, notFound is HttpException)
    }

    @Test
    fun `the screen lists boxes, the grid and the move action`() {
        val intents = mutableListOf<ChangeNapIntent>()
        val state = ChangeNapUiState(model = ChangeNap.from(NAP_OPTIONS), napId = 11, port = 5)
        compose.setContent { MaterialTheme { ChangeNapContent(state, onIntent = { intents += it }, onBack = {}) } }

        compose.onNodeWithText("LV-15-3 · puerto 7").assertExists()
        compose.onNodeWithText("Puerto en LV-15-1").assertExists()
        compose.onNodeWithText("Se libera el puerto 7 de LV-15-3.").assertExists()
        compose.onNodeWithText("Más libres").performClick()
        compose.onNodeWithTag(ChangeNapTestTags.port(9)).performClick()
        compose.onNodeWithText("Mover a LV-15-1 · puerto 5").performClick()

        assertEquals(listOf(ChangeNapIntent.SortSelected(NapSort.FREEST), ChangeNapIntent.PortSelected(9), ChangeNapIntent.Submit), intents)
    }

    @Test
    fun `a long list shows the nearest boxes first and the rest on demand`() {
        val many = NAP_OPTIONS.copy(options = (1..12).map { NapOptionResponse(100 + it, "LV-9-$it", null, 4, it * 10, 16, emptyList()) })
        compose.setContent { MaterialTheme { ChangeNapContent(ChangeNapUiState(model = ChangeNap.from(many)), onIntent = {}, onBack = {}) } }

        compose.onNodeWithText("LV-9-8").assertExists()
        compose.onNodeWithText("LV-9-9").assertDoesNotExist()
        compose.onNodeWithText("Ver las 12 cajas").performClick()
        compose.onNodeWithText("LV-9-12").assertExists()
    }
}
