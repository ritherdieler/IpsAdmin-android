package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.datasource.remote.CoverageResponse
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.subscription
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

private val NAP = NapBoxResponse(code = "LV-15-3", latitude = -11.2313, longitude = -77.3847, placeName = "La Villa", placeId = 1)

private fun located() = subscription().apply {
    location = GeoLocation(-11.2318, -77.3847)
    napBox = NAP
    place = Place(id = "1", name = "la villa")
    address = "Jr. Los Pinos 214, frente a la losa deportiva"
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1400dp")
class UpdateLocationTest {
    @get:Rule val compose = createComposeRule()
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<UpdateLocationGateway>()
    private lateinit var viewModel: UpdateLocationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.subscription(1842) } returns located()
        coEvery { gateway.coverage(any(), any(), any()) } returns CoverageResponse(hasCoverage = true, detectedPlaceName = "La Villa", matchesAssignedPlace = true)
        viewModel = UpdateLocationViewModel(gateway, dispatcher)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the point reads its coordinates, place and distance to the box`() = runTest(dispatcher) {
        viewModel.onIntent(UpdateLocationIntent.Load(1842))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("-11.2318, -77.3847", state.coordinates)
        assertEquals("La caja NAP actual (LV-15-3) queda a 60 m.", state.napNote)
        assertEquals(PlaceCheck("Dentro de La Villa", inside = true), state.placeCheck)
        assertNull(state.accuracy)
        assertEquals("Jr. Los Pinos 214, frente a la losa deportiva", state.reference)
    }

    @Test
    fun `moving the map checks the place once it stops and gps shows its precision`() = runTest(dispatcher) {
        coEvery { gateway.coverage(-11.25, -77.39, "la villa") } returns
            CoverageResponse(hasCoverage = true, detectedPlaceName = "Primavera", matchesAssignedPlace = false)
        viewModel.onIntent(UpdateLocationIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(UpdateLocationIntent.CenterMoved(-11.24, -77.38))
        advanceTimeBy(100)
        viewModel.onIntent(UpdateLocationIntent.CenterMoved(-11.25, -77.39))
        advanceUntilIdle()

        coVerify(exactly = 0) { gateway.coverage(-11.24, -77.38, any()) }
        assertEquals(PlaceCheck("Fuera de La Villa · cae en Primavera", inside = false), viewModel.uiState.value.placeCheck)

        viewModel.onIntent(UpdateLocationIntent.MyLocation(-11.2318, -77.3847, accuracy = 6.4f))
        assertEquals("Precisión ±6 m", viewModel.uiState.value.accuracy)
    }

    @Test
    fun `saving stores the new point and tells the ficha`() = runTest(dispatcher) {
        coEvery { gateway.save(1842, -11.2320, -77.3850) } returns Unit
        viewModel.onIntent(UpdateLocationIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(UpdateLocationIntent.CenterMoved(-11.2320, -77.3850))
        viewModel.onIntent(UpdateLocationIntent.Submit)
        advanceUntilIdle()

        coVerify { gateway.save(1842, -11.2320, -77.3850) }
        assertEquals("Ubicación actualizada", viewModel.uiState.value.done)
    }

    @Test
    fun `the screen shows the checks over the map and saves`() {
        var submitted = false
        val state = UpdateLocationUiState(
            loaded = true, latitude = -11.2318, longitude = -77.3847, accuracy = "Precisión ±6 m",
            placeCheck = PlaceCheck("Dentro de La Villa", inside = true), napNote = "La caja NAP actual (LV-15-3) queda a 60 m.",
            reference = "frente a la losa",
        )
        compose.setContent {
            MaterialTheme {
                UpdateLocationContent(state, onIntent = { if (it == UpdateLocationIntent.Submit) submitted = true }, onBack = {}, onMyLocation = {}, map = {})
            }
        }

        compose.onNodeWithText("Mueve el mapa para ajustar el punto").assertExists()
        compose.onNodeWithText("-11.2318, -77.3847").assertExists()
        compose.onNodeWithText("Dentro de La Villa").assertExists()
        compose.onNodeWithText("Guardar ubicación").performClick()
        assertEquals(true, submitted)
    }
}
