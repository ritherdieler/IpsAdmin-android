package com.dscorp.ispadmin.presentation.ui.features.networkmap

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.datasource.remote.NapDetailResponse
import com.dscorp.ispadmin.data.datasource.remote.NapMapBoxResponse
import com.dscorp.ispadmin.data.datasource.remote.NapMapMufaResponse
import com.dscorp.ispadmin.data.datasource.remote.NapMapPlaceResponse
import com.dscorp.ispadmin.data.datasource.remote.NapMapResponse
import com.dscorp.ispadmin.data.datasource.remote.NapSubscriberResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
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

internal val VILLA_MAP = NapMapResponse(
    placeId = 1, placeName = "la villa",
    places = listOf(NapMapPlaceResponse(1, "la villa", 60), NapMapPlaceResponse(2, "primavera", 12)),
    boxes = listOf(
        NapMapBoxResponse(13, "LV -15-3", "poste de interseccion de calles", -11.2311, -77.3852, 4, 16, 2),
        NapMapBoxResponse(11, "LV -15-1", "frente a la torre", -11.2313, -77.3847, 4, 16, 13),
        NapMapBoxResponse(32, "LV -3-2", "cruce principal", -11.2330, -77.3860, 9, 16, 16),
    ),
    mufas = listOf(NapMapMufaResponse(4, "Antena Claro", -11.2300, -77.3850, 96)),
)

internal val LV153 = NapDetailResponse(
    id = 13, code = "LV -15-3", address = "poste de interseccion de calles", latitude = -11.2311, longitude = -77.3852,
    placeName = "la villa", mufa = "Antena Claro", mufaThreads = 96, oltId = 1, oltSlot = "0/1/3", totalPorts = 16,
    subscribers = listOf(
        NapSubscriberResponse(1907, "MARIA QUISPE HUAMAN", 2, "basico", "ACTIVE", 50.0),
        NapSubscriberResponse(1842, "JORGE RAMOS VILLANUEVA", 7, "basico", "CUT_OFF", 100.0),
    ),
)

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1600dp")
class NetworkMapTest {
    @get:Rule val compose = createComposeRule()
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<NetworkMapGateway>()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `boxes read how full they are`() {
        val map = NetworkMap.from(VILLA_MAP)

        assertEquals("La Villa", map.placeName)
        assertEquals(listOf(1 to "La Villa", 2 to "Primavera"), map.places)
        assertEquals(NapPin(13, "LV -15-3", -11.2311, -77.3852, free = 14, level = NapLevel.ROOMY), map.pins[0])
        assertEquals(NapLevel.FEW, map.pins[1].level)
        assertEquals(NapLevel.FULL, map.pins[2].level)
        assertEquals("Antena Claro", map.mufas.single().label)
    }

    @Test
    fun `the detail lists ports with initials and who is connected`() {
        val detail = NapDetail.from(LV153)

        assertEquals("14 libres de 16", detail.freeLabel)
        assertEquals(
            listOf("Referencia" to "poste de interseccion de calles", "Lugar" to "La Villa", "Mufa" to "Antena Claro · 96 hilos",
                "OLT" to "OLT 1 · 0/1/3", "Coordenadas" to "-11.2311, -77.3852"),
            detail.rows,
        )
        assertEquals("MQ", detail.ports[1].initials)
        assertEquals("JR", detail.ports[6].initials)
        assertNull(detail.ports[0].initials)
        assertEquals(
            NapHolder(1842, "JR", "Jorge Ramos Villanueva", "Puerto 7 · Basico", StatusBadge("Cortado", BadgeTone.WARNING), "S/ 100.00"),
            detail.holders[1],
        )
        assertEquals("Mufa Antena Claro · OLT 1 · 0/1/3", detail.network)
        assertNull(NapDetail.from(LV153.copy(subscribers = LV153.subscribers.map { it.copy(debt = null) })).holders[0].debt)
    }

    @Test
    fun `selecting a box loads its card`() = runTest(dispatcher) {
        coEvery { gateway.map(null) } returns VILLA_MAP
        coEvery { gateway.detail(13) } returns LV153
        val viewModel = NetworkMapViewModel(gateway, dispatcher)

        viewModel.onIntent(NetworkMapIntent.Load)
        advanceUntilIdle()
        viewModel.onIntent(NetworkMapIntent.PinSelected(13))
        advanceUntilIdle()

        assertEquals("14 libres de 16", viewModel.uiState.value.selected?.freeLabel)
        assertEquals("Ver 2 abonados", viewModel.uiState.value.selected?.holdersLabel)
    }

    @Test
    fun `the map screen shows the legend and the selected box card`() {
        var opened: Int? = null
        compose.setContent {
            MaterialTheme {
                NetworkMapContent(
                    NetworkMapUiState(map = NetworkMap.from(VILLA_MAP), selected = NapDetail.from(LV153)),
                    onIntent = {}, onOpenBox = { opened = it }, onDirections = {}, map = {},
                )
            }
        }

        compose.onNodeWithText("≥5 libres").assertExists()
        compose.onNodeWithText("Llena").assertExists()
        compose.onNodeWithText("Mufa Antena Claro · OLT 1 · 0/1/3").assertExists()
        compose.onNodeWithText("Ver 2 abonados").performClick()
        assertEquals(13, opened)
    }

    @Test
    fun `the box screen shows ports and holders`() {
        var subscription: Int? = null
        compose.setContent {
            MaterialTheme { NapDetailContent(NapDetailUiState(detail = NapDetail.from(LV153)), onOpenSubscription = { subscription = it }, onDirections = {}, onOpenMap = {}, onRetry = {}) }
        }

        compose.onNodeWithText("Abonados en esta caja (2)").assertExists()
        compose.onNodeWithText("Ocupado (iniciales del titular)").assertExists()
        compose.onNodeWithText("Jorge Ramos Villanueva").performClick()
        assertEquals(1842, subscription)
    }
}
