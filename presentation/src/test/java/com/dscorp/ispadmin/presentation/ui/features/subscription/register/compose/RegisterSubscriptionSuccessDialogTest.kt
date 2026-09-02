package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Subscription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RegisterSubscriptionSuccessDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `COMPLETE shows automatic tr069 message and ssids`() {
        composeRule.setContent {
            MaterialTheme {
                Tr069StatusCard(
                    subscription = Subscription(
                        firstName = "Ana",
                        lastName = "García",
                        installationType = InstallationType.FIBER,
                        tr069ProvisionStatus = "COMPLETE",
                        wifiSsid24 = "Casa24",
                        wifiSsid5 = "Casa5"
                    )
                )
            }
        }

        composeRule.onNodeWithTag("tr069_status_card").assertIsDisplayed()
        composeRule.onNodeWithTag("tr069_provision_status_COMPLETE").assertIsDisplayed()
        composeRule.onNodeWithText(
            "ONU configurada automáticamente por TR-069. No requiere configuración manual."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Casa24").assertIsDisplayed()
        composeRule.onNodeWithText("Casa5").assertIsDisplayed()
    }

    @Test
    fun `FIBER success requires OLT COMPLETE and ACS COMPLETE tags`() {
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
        composeRule.onNodeWithTag("olt_provision_status_COMPLETE")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("olt_status_message")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("ONU autorizada en la OLT.").assertIsDisplayed()
        composeRule.onNodeWithTag("tr069_provision_status_COMPLETE")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("tr069_status_message")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `MANUAL_REQUIRED shows manual message reason and credentials`() {
        composeRule.setContent {
            MaterialTheme {
                Tr069StatusCard(
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
                        wifiPassword5 = "clave5xxx"
                    )
                )
            }
        }

        composeRule.onNodeWithTag("tr069_status_card").assertIsDisplayed()
        composeRule.onNodeWithText(
            "No se pudo configurar la ONU por TR-069. Configure la ONU manualmente."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("ONU no contactó al ACS").assertIsDisplayed()
        composeRule.onNodeWithText("Casa24").assertIsDisplayed()
        composeRule.onNodeWithText("clave24xx").assertIsDisplayed()
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
        composeRule.onNodeWithTag("btn_retry_tr069").assertIsDisplayed()
        composeRule.onNodeWithTag("register_success_section_subscriber").assertIsDisplayed()
        composeRule.onNodeWithTag("register_success_section_network")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("register_success_section_tr069")
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

        composeRule.onNodeWithTag("btn_retry_tr069").assertDoesNotExist()
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

        composeRule.onNodeWithTag("btn_retry_tr069").assertIsDisplayed()
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

        composeRule.onNodeWithTag("btn_retry_tr069").assertDoesNotExist()
    }

    @Test
    fun `PENDING shows applying message and retry button`() {
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
        composeRule.onNodeWithTag("register_success_section_tr069")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("tr069_status_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "Aplicando configuración WiFi en la ONU. Espere o reintente."
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("btn_retry_tr069").assertIsDisplayed()
        composeRule.onNodeWithText(
            "No se pudo configurar la ONU por TR-069. Configure la ONU manualmente."
        ).assertDoesNotExist()
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

        composeRule.onNodeWithTag("tr069_status_card").assertDoesNotExist()
        composeRule.onNodeWithTag("register_success_message").assertIsDisplayed()
    }
}
