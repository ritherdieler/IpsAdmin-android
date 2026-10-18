package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.RegistrationProgressCheckpoint
import com.dscorp.ispadmin.domain.model.Subscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h2000dp")
class RegisterResultScreensTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val lucia = Subscription(
        subscriptionId = 2318,
        firstName = "LUCIA",
        lastName = "MENDOZA ROJAS",
        phone = "987111222",
        installationType = InstallationType.FIBER,
        wifiSsid24 = "Mendoza_Fibra",
        wifiPassword24 = "k7Rm2pQx9v",
        wifiSsid5 = "Mendoza_Fibra - 5G",
    )

    @Test
    fun `progress counts finished stages`() {
        assertEquals(
            "2 de 4 listos",
            progressSummary(
                listOf(
                    RegistrationProgressCheckpoint(stage = "OLT", state = "SUCCEEDED", attempts = 1),
                    RegistrationProgressCheckpoint(stage = "ACS_CONTACT", state = "SUCCEEDED", attempts = 1),
                    RegistrationProgressCheckpoint(stage = "INTERNET", state = "RUNNING", attempts = 1),
                    RegistrationProgressCheckpoint(stage = "WIFI", state = "PENDING", attempts = 1),
                ),
            ),
        )
        assertEquals(null, progressSummary(emptyList()))
    }

    @Test
    fun `the whatsapp message carries the wifi data to the client's phone`() {
        val message = installationMessage(lucia)

        assertTrue(message.startsWith("Hola Lucia"))
        assertTrue("Mendoza_Fibra" in message)
        assertTrue("k7Rm2pQx9v" in message)
        assertTrue("Mendoza_Fibra - 5G" in message)
        assertEquals("https://wa.me/51987111222?text=Hola%20Lucia", whatsappLink("987 111 222", "Hola Lucia"))
        assertEquals(null, whatsappLink(null, "x"))
    }

    @Test
    fun `success names the subscription and offers whatsapp and the ficha`() {
        var shared = false
        var opened = false
        composeRule.setContent {
            MaterialTheme {
                RegisterSuccessFullScreen(
                    subscription = lucia,
                    onDismiss = {},
                    onContinue = {},
                    onShareWhatsApp = { shared = true },
                    onViewSubscription = { opened = true },
                )
            }
        }

        composeRule.onNodeWithText("Suscripción registrada").assertExists()
        composeRule.onNodeWithText("Lucia Mendoza Rojas · cód. 2318").assertExists()
        composeRule.onNodeWithText("Enviar datos por WhatsApp").performClick()
        composeRule.onNodeWithText("Ir a la ficha").performClick()

        assertTrue(shared)
        assertTrue(opened)
    }

    @Test
    fun `a failed wifi reads as registered with wifi missing`() {
        composeRule.setContent {
            MaterialTheme {
                RegisterSuccessFullScreen(
                    subscription = lucia.copy(tr069ProvisionStatus = "FAILED"),
                    onRetryTr069 = {},
                    onDismiss = {},
                    onContinue = {},
                )
            }
        }

        composeRule.onNodeWithText("Registrada, falta el Wi-Fi").assertExists()
        composeRule.onNodeWithText("El servicio ya tiene internet. La ONU no respondió al aplicar el Wi-Fi.").assertExists()
        composeRule.onNodeWithText("Continuar sin Wi-Fi").assertExists()
    }
}
