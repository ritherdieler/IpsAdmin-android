package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.dscorp.ispadmin.domain.model.CustomerData
import com.dscorp.ispadmin.domain.model.CustomerDetail
import com.dscorp.ispadmin.domain.model.CustomerServiceSummary
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.subscription
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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

private val VILLA = Place(id = "1", name = "la villa")
private val PRIMAVERA = Place(id = "2", name = "primavera")

private fun jorge() = subscription(firstName = "JORGE", lastName = "RAMOS VILLANUEVA").copy(
    dni = "45678912", phone = "987654321", customerId = 77, email = null,
).apply { address = "Jr. Los Pinos 214, frente a la losa deportiva"; place = VILLA }

private val TWO_SERVICES = CustomerDetail(
    id = 77,
    subscriptions = listOf(
        CustomerServiceSummary(1842, planName = "basico", installationType = "FIBER"),
        CustomerServiceSummary(1900, planName = "Cable basico", installationType = "ONLY_TV_FIBER"),
    ),
)

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1600dp")
class EditHolderTest {
    @get:Rule val compose = createComposeRule()
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<EditHolderGateway>()
    private lateinit var viewModel: EditHolderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.subscription(1842) } returns jorge()
        coEvery { gateway.customer(77) } returns TWO_SERVICES
        coEvery { gateway.places() } returns listOf(VILLA, PRIMAVERA)
        viewModel = EditHolderViewModel(gateway, dispatcher)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the form starts with the holder data and names every service it applies to`() = runTest(dispatcher) {
        viewModel.onIntent(EditHolderIntent.Load(1842))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HolderForm("JORGE", "RAMOS VILLANUEVA", "45678912", "987654321", "", "Jr. Los Pinos 214, frente a la losa deportiva", 1, "La Villa"), state.form)
        assertEquals("Se aplica a los 2 servicios de este titular: Fibra · Basico y Solo TV · Cable basico.", state.sharedNote)
        assertEquals(listOf("La Villa", "Primavera"), state.places.map { it.second })
        assertEquals(false, state.changed)
    }

    @Test
    fun `errors show as fields are edited and block saving`() = runTest(dispatcher) {
        viewModel.onIntent(EditHolderIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(EditHolderIntent.Changed(HolderField.DNI, "4567"))
        viewModel.onIntent(EditHolderIntent.Changed(HolderField.EMAIL, "jorge@"))
        viewModel.onIntent(EditHolderIntent.Changed(HolderField.NAME, "José"))

        val state = viewModel.uiState.value
        assertEquals("DNI requiere 8 dígitos", state.errors[HolderField.DNI])
        assertEquals("Email inválido", state.errors[HolderField.EMAIL])
        assertEquals("JOSE", state.form?.name)
        assertNull(state.errors[HolderField.NAME])
        assertEquals(false, state.canSave)
    }

    @Test
    fun `saving sends the holder data with the chosen place`() = runTest(dispatcher) {
        val sent = slot<CustomerData>()
        coEvery { gateway.save(capture(sent)) } returns Unit
        viewModel.onIntent(EditHolderIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(EditHolderIntent.Changed(HolderField.PHONE, "912345678"))
        viewModel.onIntent(EditHolderIntent.PlaceSelected(2))
        viewModel.onIntent(EditHolderIntent.Submit)
        advanceUntilIdle()

        coVerify(exactly = 1) { gateway.save(any()) }
        assertEquals(CustomerData(1842, "JORGE", "RAMOS VILLANUEVA", "45678912", "Primavera", "Jr. Los Pinos 214, frente a la losa deportiva", "912345678", "", 2), sent.captured)
        assertEquals("Datos del titular actualizados", viewModel.uiState.value.done)
    }

    @Test
    fun `the screen shows the shared note, counts the address and blocks a bad phone`() {
        val intents = mutableListOf<EditHolderIntent>()
        val form = HolderForm("JORGE", "RAMOS VILLANUEVA", "45678912", "98765", "", "Jr. Los Pinos 214, frente a la losa deportiva", 1, "La Villa")
        val state = EditHolderUiState(
            form = form, original = form.copy(phone = "987654321"), errors = mapOf(HolderField.PHONE to "Teléfono requiere 9 dígitos"),
            sharedNote = "Se aplica a los 2 servicios de este titular: Fibra · Basico y Solo TV · Cable basico.",
            places = listOf(1 to "La Villa"),
        )
        compose.setContent { MaterialTheme { EditHolderContent(state, onIntent = { intents += it }, onBack = {}) } }

        compose.onNodeWithText("Se aplica a los 2 servicios de este titular: Fibra · Basico y Solo TV · Cable basico.").assertExists()
        compose.onNodeWithText("Teléfono requiere 9 dígitos").assertExists()
        compose.onNodeWithText("45/300").assertExists()
        compose.onNodeWithTag(EditHolderTestTags.SUBMIT).assertIsNotEnabled()
        compose.onNodeWithTag(EditHolderTestTags.field(HolderField.PHONE)).performTextInput("1")

        assertEquals(EditHolderIntent.Changed(HolderField.PHONE, "198765"), intents.last())
    }

    @Test
    fun `stored data that breaks a rule shows its error from the start`() = runTest(dispatcher) {
        coEvery { gateway.subscription(1842) } returns jorge().copy(dni = "4338364")

        viewModel.onIntent(EditHolderIntent.Load(1842))
        advanceUntilIdle()

        assertEquals(mapOf(HolderField.DNI to "DNI requiere 8 dígitos"), viewModel.uiState.value.errors)
    }
}
