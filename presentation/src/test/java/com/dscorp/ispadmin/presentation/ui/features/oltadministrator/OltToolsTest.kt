package com.dscorp.ispadmin.presentation.ui.features.oltadministrator

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.datasource.remote.OnuLinkedSubscriptionResponse
import com.dscorp.ispadmin.data.datasource.remote.OnuLiveStatusResponse
import com.dscorp.ispadmin.data.datasource.remote.OnuLookupResponse
import com.dscorp.ispadmin.data.datasource.remote.OltToolsApi
import com.dscorp.ispadmin.data.utils.EndpointUnavailableException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
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

private val LOOSE = OnuLookupResponse("HWTC7B21C0D4", "olt-01_1_2_14", "HG8145V5", "OLT-01", "0/1/2", "14", "2025-03-12 10:20:00", null)
private val USED = LOOSE.copy(subscription = OnuLinkedSubscriptionResponse(1842, "JORGE RAMOS VILLANUEVA", "CUT_OFF"))

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1400dp")
class OltToolsTest {
    @get:Rule val compose = createComposeRule()
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<OltToolsGateway>()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the card says where the ONU is, who uses it and how it reads`() {
        val card = OnuCard.from(LOOSE, OnuLiveStatusResponse(runState = "offline", onuRxDbm = -27.14))

        assertEquals(
            listOf("Ubicación" to "OLT-01 · 0/1/2 · ONT 14", "Estado" to "Sin suscripción vinculada", "Señal" to "Sin señal", "Potencia" to "−27.1 dBm"),
            card.rows,
        )
        assertEquals("Se borra su configuración en OLT-01 0/1/2. Si alguien la usa, pierde el servicio. No se puede deshacer.", card.deleteWarning)
        val used = OnuCard.from(USED, null)
        assertEquals("Suscripción 1842 · Jorge Ramos Villanueva · Cortado", used.rows.toMap()["Estado"])
        assertEquals(false, used.rows.toMap().containsKey("Potencia"))
        assertEquals("La usa la suscripción 1842 (Jorge Ramos Villanueva), que perderá el servicio.", used.linkedWarning)
        assertNull(card.linkedWarning)
    }

    @Test
    fun `searching finds the ONU even when the live status fails`() = runTest(dispatcher) {
        coEvery { gateway.lookup("HWTC7B21C0D4") } returns LOOSE
        coEvery { gateway.liveStatus("olt-01_1_2_14") } throws IllegalStateException("gateway off")
        val viewModel = OltToolsViewModel(gateway, dispatcher)

        viewModel.onIntent(OltToolsIntent.QueryChanged(" hwtc7b21c0d4 "))
        viewModel.onIntent(OltToolsIntent.Search)
        advanceUntilIdle()

        assertEquals("HWTC7B21C0D4", viewModel.uiState.value.card?.sn)
        assertEquals(false, viewModel.uiState.value.card?.rows?.toMap()?.containsKey("Señal"))
    }

    @Test
    fun `deleting removes the ONU by serial and clears the card`() = runTest(dispatcher) {
        coEvery { gateway.lookup(any()) } returns LOOSE
        coEvery { gateway.liveStatus(any()) } returns OnuLiveStatusResponse()
        coEvery { gateway.deleteBySn("HWTC7B21C0D4") } returns Unit
        val viewModel = OltToolsViewModel(gateway, dispatcher)
        viewModel.onIntent(OltToolsIntent.QueryChanged("HWTC7B21C0D4"))
        viewModel.onIntent(OltToolsIntent.Search)
        advanceUntilIdle()

        viewModel.onIntent(OltToolsIntent.Delete)
        advanceUntilIdle()

        // Staging only lets lab serials be written, so the delete must carry the SN, not the external id.
        coVerify { gateway.deleteBySn("HWTC7B21C0D4") }
        assertNull(viewModel.uiState.value.card)
        assertEquals("ONU HWTC7B21C0D4 eliminada de la OLT", viewModel.uiState.value.message)
    }

    @Test
    fun `an unknown serial reads as not found`() = runTest(dispatcher) {
        coEvery { gateway.lookup(any()) } returns null
        val viewModel = OltToolsViewModel(gateway, dispatcher)
        viewModel.onIntent(OltToolsIntent.QueryChanged("XXXX1234"))
        viewModel.onIntent(OltToolsIntent.Search)
        advanceUntilIdle()

        assertEquals("No hay una ONU con ese serial en la OLT.", viewModel.uiState.value.error)
    }

    @Test
    fun `a Core without the lookup says the tool is not available instead of not found`() = runTest(dispatcher) {
        coEvery { gateway.lookup(any()) } throws EndpointUnavailableException()
        val viewModel = OltToolsViewModel(gateway, dispatcher)
        viewModel.onIntent(OltToolsIntent.QueryChanged("HWTC7B21C0D4"))
        viewModel.onIntent(OltToolsIntent.Search)
        advanceUntilIdle()

        assertEquals("Función no disponible en este servidor.", viewModel.uiState.value.error)
    }

    @Test
    fun `the api gateway tells a missing lookup endpoint from an unknown serial`() = runTest(dispatcher) {
        val api = mockk<OltToolsApi>()
        coEvery { api.lookup("HWTC7B21C0D4") } returns Response.error(
            404,
            """{"timestamp":"2026-10-20T10:00:00.000+00:00","status":404,"error":"Not Found","message":"No message available","path":"/ispadmin/onu/lookup"}"""
                .toResponseBody("application/json".toMediaType()),
        )
        coEvery { api.lookup("XXXX1234") } returns Response.error(
            404,
            """{"status":404,"error":"Not Found","message":"No se encontró la ONU en la OLT"}""".toResponseBody("application/json".toMediaType()),
        )

        val missing = runCatching { ApiOltToolsGateway(api).lookup("HWTC7B21C0D4") }.exceptionOrNull()

        assertEquals(true, missing is EndpointUnavailableException)
        assertNull(ApiOltToolsGateway(api).lookup("XXXX1234"))
    }

    @Test
    fun `deleting asks first and shows what it affects`() {
        var deleted = false
        compose.setContent {
            MaterialTheme {
                OltToolsContent(OltToolsUiState(query = "HWTC7B21C0D4", card = OnuCard.from(USED, null)), onIntent = { if (it == OltToolsIntent.Delete) deleted = true })
            }
        }

        compose.onNodeWithText("Solo administradores. Los cambios afectan el servicio del abonado.").assertExists()
        compose.onNodeWithText("Eliminar de la OLT").performClick()
        compose.onNodeWithText("¿Eliminar la ONU HWTC7B21C0D4?").assertExists()
        compose.onNodeWithText("La usa la suscripción 1842 (Jorge Ramos Villanueva), que perderá el servicio.").assertExists()
        assertEquals(false, deleted)
        compose.onNodeWithText("Eliminar ONU").performClick()
        assertEquals(true, deleted)
    }

    @Test
    fun `a refused delete shows the reason and keeps the card`() = runTest(dispatcher) {
        coEvery { gateway.lookup(any()) } returns LOOSE
        coEvery { gateway.liveStatus(any()) } returns OnuLiveStatusResponse()
        coEvery { gateway.deleteBySn("HWTC7B21C0D4") } throws OltToolsRefusal("En staging solo se pueden borrar ONUs de laboratorio.")
        val viewModel = OltToolsViewModel(gateway, dispatcher)
        viewModel.onIntent(OltToolsIntent.QueryChanged("HWTC7B21C0D4"))
        viewModel.onIntent(OltToolsIntent.Search)
        advanceUntilIdle()

        viewModel.onIntent(OltToolsIntent.Delete)
        advanceUntilIdle()

        assertEquals("En staging solo se pueden borrar ONUs de laboratorio.", viewModel.uiState.value.error)
        assertEquals("HWTC7B21C0D4", viewModel.uiState.value.card?.sn)
    }

    @Test
    fun `the api gateway reads the Core reason of a refused delete`() = runTest(dispatcher) {
        val api = mockk<OltToolsApi>()
        coEvery { api.deleteBySn("HWTC7B21C0D4") } returns Response.error(
            403,
            """{"status":403,"error":"Forbidden","message":"En staging solo se pueden borrar ONUs de laboratorio."}"""
                .toResponseBody("application/json".toMediaType()),
        )
        coEvery { api.deleteBySn("HWTC00000000") } returns Response.error(502, "".toResponseBody(null))

        val refusal = runCatching { ApiOltToolsGateway(api).deleteBySn("HWTC7B21C0D4") }.exceptionOrNull()
        val silent = runCatching { ApiOltToolsGateway(api).deleteBySn("HWTC00000000") }.exceptionOrNull()

        assertEquals("En staging solo se pueden borrar ONUs de laboratorio.", (refusal as OltToolsRefusal).message)
        assertEquals(false, silent is OltToolsRefusal)
    }
}
