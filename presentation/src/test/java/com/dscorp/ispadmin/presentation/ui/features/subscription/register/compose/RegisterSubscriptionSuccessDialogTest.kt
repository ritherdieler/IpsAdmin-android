package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionUiEvent
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.R
import com.google.gson.Gson
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
class RegisterSubscriptionSuccessDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `success summary shows plan speeds and access credentials without infrastructure statuses`() {
        val subscription = Gson().fromJson(
            """{
                "id":42,
                "firstName":"María Fernanda",
                "lastName":"López",
                "dni":"45872136",
                "phone":"987654321",
                "accessMode":"PPPOE_DYNAMIC",
                "pppoeUsername":"gf48291",
                "pppoePassword":"PppoeDemo!",
                "wifiSsid24":"Casa Maria",
                "wifiPassword24":"WifiDemo24!",
                "wifiSsid5":"Casa Maria_5G",
                "wifiPassword5":"WifiDemo5G!"
            }""",
            Subscription::class.java,
        )
        val events = MutableSharedFlow<RegisterSubscriptionUiEvent>(replay = 1).apply {
            tryEmit(RegisterSubscriptionUiEvent.Success(subscription))
        }
        val state = MutableStateFlow(
            RegisterSubscriptionState(
                registerSubscriptionForm = RegisterSubscriptionFormState(
                    selectedPlan = PlanResponse(
                        id = "1",
                        name = "Fibra 300 Mbps",
                        price = 79.9,
                        downloadSpeed = "300",
                        uploadSpeed = "100",
                        type = InstallationType.FIBER,
                    ),
                ),
            ),
        )
        val viewModel = mockk<RegisterSubscriptionComposeViewModel>(relaxed = true)
        every { viewModel.uiState } returns state
        every { viewModel.uiEvent } returns events

        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionFormScreen(
                    viewModel = viewModel,
                    installationOrderId = null,
                )
            }
        }

        composeRule.onNodeWithTag("register_success_fullscreen").assertIsDisplayed()
        composeRule.onNodeWithText("Fibra 300 Mbps").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("S/ 79.90 / mes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Bajada:").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("300 Mbps").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Subida:").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("100 Mbps").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("gf48291").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("PppoeDemo!").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Casa Maria").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Casa Maria_5G").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("WifiDemo24!").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("WifiDemo5G!").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("register_success_section_olt").assertDoesNotExist()
        composeRule.onNodeWithTag("register_success_section_tr069").assertDoesNotExist()
        composeRule.onNodeWithText("Borne").assertDoesNotExist()
    }

    @Test
    fun `success action says finalizar instalacion`() {
        composeRule.setContent {
            MaterialTheme {
                RegisterSuccessFullScreen(
                    subscription = Subscription(
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                    ),
                    onDismiss = {},
                    onContinue = {},
                )
            }
        }

        composeRule.onNodeWithText("Finalizar instalación").assertIsDisplayed()
    }

    @Test
    fun `success shows wifi credentials regardless of provisioning status`() {
        composeRule.setContent {
            MaterialTheme {
                SuccessDialog(
                    subscription = Subscription(
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "COMPLETE",
                        wifiSsid24 = "Casa24",
                        wifiPassword24 = "clave24xx",
                        wifiSsid5 = "Casa5",
                        wifiPassword5 = "clave5xxx",
                    ),
                    onDismiss = {},
                    onContinue = {},
                )
            }
        }

        composeRule.onNodeWithText("Casa24").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("clave24xx").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Casa5").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("clave5xxx").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ONU configurada automáticamente por TR-069.").assertDoesNotExist()
    }

    @Test
    fun `success summary hides infrastructure authorization statuses`() {
        composeRule.setContent {
            MaterialTheme {
                RegisterSuccessFullScreen(
                    subscription = Subscription(
                        subscriptionId = 42,
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        oltProvisionStatus = "COMPLETE",
                        tr069ProvisionStatus = "COMPLETE",
                        wifiSsid24 = "Casa24",
                        wifiSsid5 = "Casa5",
                    ),
                    onDismiss = {},
                    onContinue = {},
                )
            }
        }

        composeRule.onNodeWithTag("register_success_fullscreen").assertIsDisplayed()
        composeRule.onNodeWithTag("register_success_section_olt").assertDoesNotExist()
        composeRule.onNodeWithTag("register_success_section_tr069").assertDoesNotExist()
        composeRule.onNodeWithText("Autorización OLT").assertDoesNotExist()
        composeRule.onNodeWithText("ONU / TR-069").assertDoesNotExist()
        composeRule.onNodeWithText("Borne").assertDoesNotExist()
    }

    @Test
    fun `wifi credentials remain visible when manual provisioning is required`() {
        composeRule.setContent {
            MaterialTheme {
                RegisterSuccessFullScreen(
                    subscription = Subscription(
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "MANUAL_REQUIRED",
                        tr069RequiresManualConfig = true,
                        tr069Message = "ONU no contactó al ACS",
                        wifiSsid24 = "Casa24",
                        wifiPassword24 = "clave24xx",
                        wifiSsid5 = "Casa5",
                        wifiPassword5 = "clave5xxx",
                    ),
                    onDismiss = {},
                    onContinue = {},
                )
            }
        }

        composeRule.onNodeWithText("Casa24").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("clave24xx").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ONU no contactó al ACS").assertDoesNotExist()
        composeRule.onNodeWithText("No se pudo configurar la ONU por TR-069.").assertDoesNotExist()
    }

    @Test
    fun `MANUAL_REQUIRED shows retry button in fullscreen`() {
        composeRule.setContent {
            MaterialTheme {
                RegisterSuccessFullScreen(
                    subscription = Subscription(
                        subscriptionId = 42,
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "MANUAL_REQUIRED",
                    ),
                    onRetryTr069 = {},
                    onDismiss = {},
                    onContinue = {}
                )
            }
        }

        composeRule.onNodeWithTag("register_success_fullscreen").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_retry_wifi").assertIsDisplayed()
        composeRule.onNodeWithTag("register_success_section_subscriber").assertIsDisplayed()
        composeRule.onNodeWithTag("register_success_section_plan")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("register_success_section_wifi")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `MANUAL_REQUIRED hides retry button when subscription id is missing`() {
        composeRule.setContent {
            MaterialTheme {
                RegisterSuccessFullScreen(
                    subscription = Subscription(
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "MANUAL_REQUIRED",
                        tr069RequiresManualConfig = true,
                    ),
                    onRetryTr069 = null,
                    onDismiss = {},
                    onContinue = {}
                )
            }
        }

        composeRule.onNodeWithTag("btn_retry_wifi").assertDoesNotExist()
    }

    @Test
    fun `MANUAL_REQUIRED shows retry button`() {
        composeRule.setContent {
            MaterialTheme {
                SuccessDialog(
                    subscription = Subscription(
                        subscriptionId = 42,
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "MANUAL_REQUIRED",
                    ),
                    onRetryTr069 = {},
                    onDismiss = {},
                    onContinue = {}
                )
            }
        }

        composeRule.onNodeWithTag("btn_retry_wifi").assertIsDisplayed()
    }

    @Test
    fun `COMPLETE hides retry button`() {
        composeRule.setContent {
            MaterialTheme {
                SuccessDialog(
                    subscription = Subscription(
                        subscriptionId = 42,
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "COMPLETE",
                    ),
                    onRetryTr069 = {},
                    onDismiss = {},
                    onContinue = {}
                )
            }
        }

        composeRule.onNodeWithTag("btn_retry_wifi").assertDoesNotExist()
    }

    @Test
    fun `PENDING shows wifi details and retry button without exposing status message`() {
        composeRule.setContent {
            MaterialTheme {
                RegisterSuccessFullScreen(
                    subscription = Subscription(
                        subscriptionId = 42,
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "PENDING",
                        tr069Message = "Los SSIDs no se confirmaron en el ACS dentro del tiempo de espera.",
                        wifiSsid24 = "Casa24",
                        wifiSsid5 = "Casa5"
                    ),
                    onRetryTr069 = {},
                    onDismiss = {},
                    onContinue = {}
                )
            }
        }

        composeRule.onNodeWithTag("register_success_fullscreen").assertIsDisplayed()
        composeRule.onNodeWithTag("register_success_section_wifi")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("btn_retry_wifi").assertIsDisplayed()
        composeRule.onNodeWithText("Los SSIDs no se confirmaron en el ACS dentro del tiempo de espera.")
            .assertDoesNotExist()
        composeRule.onNodeWithText(
            "No se pudo configurar la ONU por TR-069. Configure la ONU manualmente."
        ).assertDoesNotExist()
    }

    @Test
    fun `retry loading shows feedback message and reintentando label`() {
        composeRule.setContent {
            MaterialTheme {
                RegisterSuccessFullScreen(
                    subscription = Subscription(
                        subscriptionId = 42,
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "PENDING",
                        tr069Message = "Esperando ACS",
                    ),
                    tr069RetryLoading = true,
                    onRetryTr069 = {},
                    onDismiss = {},
                    onContinue = {}
                )
            }
        }

        composeRule.onNodeWithTag("wifi_retry_feedback").assertIsDisplayed()
        composeRule.onNodeWithText("Actualizando redes Wi-Fi…").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_retry_wifi").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentando Wi-Fi…").assertIsDisplayed()
    }

    @Test
    fun `FAILED shows retry button`() {
        composeRule.setContent {
            MaterialTheme {
                RegisterSuccessFullScreen(
                    subscription = Subscription(
                        subscriptionId = 42,
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "FAILED",
                        tr069Message = "ACS no respondió",
                    ),
                    onRetryTr069 = {},
                    onDismiss = {},
                    onContinue = {}
                )
            }
        }

        composeRule.onNodeWithTag("btn_retry_wifi").assertIsDisplayed()
        composeRule.onNodeWithTag("register_success_section_wifi")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `NA hides tr069 card in SuccessDialog`() {
        composeRule.setContent {
            MaterialTheme {
                SuccessDialog(
                    subscription = Subscription(
                        firstName = "Ana",
                        lastName = "García",
                        dni = "12345678",
                        phone = "987654321",
                        address = "Av. Principal 100",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "NA"
                    ),
                    onDismiss = {},
                    onContinue = {}
                )
            }
        }

        composeRule.onNodeWithTag("register_success_section_tr069").assertDoesNotExist()
        composeRule.onNodeWithTag("register_success_message").assertIsDisplayed()
    }
}
