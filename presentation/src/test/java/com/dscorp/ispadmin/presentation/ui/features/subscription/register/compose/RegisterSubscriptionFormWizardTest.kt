package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.RegistrationProgressCheckpoint
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionUiEvent
import com.google.android.gms.maps.model.LatLng
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RegisterSubscriptionFormWizardTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `continue is enabled when current step is valid and later steps are incomplete`() {
        val state = RegisterSubscriptionState(
            registerSubscriptionForm = RegisterSubscriptionFormState(
                firstName = "Juan",
                lastName = "Perez",
                dni = "12345678",
                phone = "987654321",
                address = "Calle larga 12345",
                placeList = listOf(Place(id = "place-1", name = "Lima")),
                selectedPlace = Place(id = "place-1", name = "Lima"),
                location = LatLng(-12.046374, -77.042793),
            ),
            wizardStep = RegisterSubscriptionWizardStep.CLIENT_LOCATION,
            preauthorizationEnabled = true,
        )

        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(formState = state)
            }
        }

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE)
            .assertIsEnabled()
    }

    @Test
    fun `preauthorization gates ONU selection and exposes only ACS actions while waiting`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        val state = mutableStateOf(
            RegisterSubscriptionState(
                wizardStep = RegisterSubscriptionWizardStep.ONU_SELECTION,
                preauthorizationEnabled = true,
            )
        )

        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(formState = state.value)
            }
        }

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE)
            .assertIsNotEnabled()
        composeRule.runOnIdle {
            state.value = state.value.copy(
                registerSubscriptionForm = state.value.registerSubscriptionForm.copy(
                    onuList = listOf(onu),
                    selectedOnu = onu,
                )
            )
        }
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE)
            .assertIsEnabled()

        composeRule.runOnIdle {
            state.value = state.value.copy(wizardStep = RegisterSubscriptionWizardStep.ONU_CONFIRMATION)
        }
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE)
            .assertIsEnabled()
        composeRule.runOnIdle {
            state.value = state.value.copy(
                registerSubscriptionForm = state.value.registerSubscriptionForm.copy(selectedOnu = null)
            )
        }
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE)
            .assertIsNotEnabled()

        composeRule.runOnIdle {
            state.value = state.value.copy(wizardStep = RegisterSubscriptionWizardStep.WAITING_FOR_ACS)
        }
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE)
            .assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar").assertExists()
        composeRule.onNodeWithText("Reintentar").assertExists()
    }

    @Test
    fun `selected ONU can be authorized without a duplicate confirmation screen`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        val state = mutableStateOf(
            RegisterSubscriptionState(
                wizardStep = RegisterSubscriptionWizardStep.ONU_SELECTION,
                preauthorizationEnabled = true,
            )
        )

        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(
                    formState = state.value,
                    onIntent = { intent ->
                        when (intent) {
                            RegisterSubscriptionIntent.WizardContinueClicked -> {
                                state.value = state.value.copy(
                                    wizardStep = RegisterSubscriptionWizardStep.WAITING_FOR_ACS,
                                )
                            }
                            else -> Unit
                        }
                    },
                )
            }
        }

        composeRule.runOnIdle {
            state.value = state.value.copy(
                registerSubscriptionForm = state.value.registerSubscriptionForm.copy(
                    onuList = listOf(onu),
                    selectedOnu = onu,
                )
            )
        }
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE)
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithText("Cancelar").assertExists()
        composeRule.onNodeWithText("Autorizar ONU").assertDoesNotExist()
    }

    @Test
    fun `preauthorized installation does not ask for ONU a second time`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        val form = validFiberInstallationForm(onu)
        val state = RegisterSubscriptionState(
            registerSubscriptionForm = form,
            wizardStep = RegisterSubscriptionWizardStep.INSTALLATION,
            preauthorizationEnabled = true,
        )

        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(formState = state)
            }
        }

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ONU)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE)
            .assertIsEnabled()
    }

    @Test
    fun `cancel registration action stays visible throughout the preauthorized form`() {
        val state = mutableStateOf(
            RegisterSubscriptionState(
                wizardStep = RegisterSubscriptionWizardStep.CLIENT_LOCATION,
                preauthorizationEnabled = true,
            )
        )

        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(formState = state.value)
            }
        }

        listOf(
            RegisterSubscriptionWizardStep.ONU_SELECTION,
            RegisterSubscriptionWizardStep.ONU_CONFIRMATION,
            RegisterSubscriptionWizardStep.WAITING_FOR_ACS,
            RegisterSubscriptionWizardStep.CLIENT_LOCATION,
            RegisterSubscriptionWizardStep.INSTALLATION,
            RegisterSubscriptionWizardStep.CONFIRMATION,
        ).forEach { step ->
            composeRule.runOnIdle { state.value = state.value.copy(wizardStep = step) }
            composeRule.onNodeWithTag("onu_registration_cancel").assertExists()
        }
    }

    @Test
    fun `registration progress overlay exposes cancellation while blocking the form`() {
        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegistrationProgressOverlay(onCancel = {})
            }
        }

        composeRule.onNodeWithTag("registration_cancel_action").assertExists()
    }

    @Test
    fun `opening preauthorization form shows general loader without cancellation action`() {
        val state = MutableStateFlow(
            RegisterSubscriptionState(
                isLoading = true,
                wizardStep = RegisterSubscriptionWizardStep.ONU_SELECTION,
                preauthorizationEnabled = true,
            )
        )
        val viewModel = mockk<RegisterSubscriptionComposeViewModel>(relaxed = true)
        every { viewModel.uiState } returns state
        every { viewModel.uiEvent } returns MutableSharedFlow<RegisterSubscriptionUiEvent>()

        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionFormScreen(
                    viewModel = viewModel,
                    installationOrderId = null,
                )
            }
        }

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.CATALOG_LOADING_MESSAGE)
            .assertTextEquals("Preparando el registro…")
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.REGISTRATION_CANCEL_ACTION)
            .assertDoesNotExist()

        composeRule.runOnIdle {
            state.value = state.value.copy(isPreauthorizationRequestInProgress = true)
        }

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.CATALOG_LOADING_MESSAGE)
            .assertTextEquals("Autorizando el equipo en la OLT…")
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.REGISTRATION_CANCEL_ACTION)
            .assertExists()
    }

    @Test
    fun `registration progress headline follows the active backend checkpoint`() {
        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegistrationProgressOverlay(
                    progressMessage = "Esperando aprovisionamiento TR-069.",
                    progressCheckpoints = listOf(
                        RegistrationProgressCheckpoint(stage = "WIFI", state = "RUNNING", attempts = 1),
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.PROGRESS_STEP)
            .assertTextEquals("Aplicando WiFi…")
    }

    @Test
    fun `preauthorized fiber installation shows required WiFi inputs`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        val state = mutableStateOf(
            RegisterSubscriptionState(
                registerSubscriptionForm = validFiberInstallationForm(onu).copy(
                    wifiSsid24 = "",
                    wifiPassword24 = "",
                ),
                wizardStep = RegisterSubscriptionWizardStep.INSTALLATION,
                preauthorizationEnabled = true,
            )
        )

        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(formState = state.value)
            }
        }

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ONU).assertDoesNotExist()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIFI_SSID_24).assertExists()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIFI_PASSWORD_24).assertExists()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE).assertIsNotEnabled()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                registerSubscriptionForm = state.value.registerSubscriptionForm.copy(
                    wifiSsid24 = "CasaFibra",
                    wifiPassword24 = "clave123",
                )
            )
        }
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE).assertIsEnabled()
    }

    @Test
    fun `final registration button waits for the required facade photo`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        val state = mutableStateOf(
            RegisterSubscriptionState(
                registerSubscriptionForm = validFiberInstallationForm(onu).copy(
                    firstName = "Juan",
                    lastName = "Perez",
                    dni = "12345678",
                    phone = "987654321",
                    address = "Calle larga 12345",
                    placeList = listOf(Place(id = "place-1", name = "Lima")),
                    selectedPlace = Place(id = "place-1", name = "Lima"),
                    location = LatLng(-12.046374, -77.042793),
                ),
                wizardStep = RegisterSubscriptionWizardStep.CONFIRMATION,
                preauthorizationEnabled = true,
            )
        )

        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(formState = state.value)
            }
        }

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SUBMIT)
            .assertIsNotEnabled()
        composeRule.runOnIdle {
            state.value = state.value.copy(
                registerSubscriptionForm = state.value.registerSubscriptionForm.copy(
                    facadePhotoUrl = "https://storage.example/photo.jpg"
                )
            )
        }
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SUBMIT)
            .assertIsEnabled()
    }

    @Test
    fun `legacy installation still asks for ONU`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        val state = RegisterSubscriptionState(
            registerSubscriptionForm = RegisterSubscriptionFormState(
                onuList = listOf(onu),
                selectedOnu = onu,
            ),
            wizardStep = RegisterSubscriptionWizardStep.INSTALLATION,
        )

        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(formState = state)
            }
        }

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ONU)
            .assertExists()
    }

    private fun validFiberInstallationForm(onu: Onu) = RegisterSubscriptionFormState(
        planList = listOf(
            PlanResponse(
                id = "plan-1",
                name = "Fibra",
                price = 10.0,
                downloadSpeed = "100",
                uploadSpeed = "100",
                type = InstallationType.FIBER,
            )
        ),
        selectedPlan = PlanResponse(
            id = "plan-1",
            name = "Fibra",
            price = 10.0,
            downloadSpeed = "100",
            uploadSpeed = "100",
            type = InstallationType.FIBER,
        ),
        coreDeviceList = listOf(NetworkDevice(id = 10, name = "Core", disabled = false)),
        selectedHostDevice = NetworkDevice(id = 10, name = "Core", disabled = false),
        napBoxList = listOf(NapBoxResponse(id = "nap-1", placeName = "Lima", placeId = 1)),
        selectedNapBox = NapBoxResponse(id = "nap-1", placeName = "Lima", placeId = 1),
        onuList = listOf(onu),
        selectedOnu = onu,
        wifiSsid24 = "CasaFibra",
        wifiPassword24 = "clave123",
    )
}
