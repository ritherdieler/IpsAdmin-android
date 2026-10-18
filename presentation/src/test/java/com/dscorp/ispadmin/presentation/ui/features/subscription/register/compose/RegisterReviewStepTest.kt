package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h2400dp")
class RegisterReviewStepTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val intents = mutableListOf<RegisterSubscriptionIntent>()
    private val onu = Onu("1", "1", "1", "t", "ZTE F670L", "GPON", "5", "ZTEGC8F01A22")

    private fun show() {
        val form = RegisterSubscriptionFormState(
            firstName = "LUCÍA", lastName = "MENDOZA ROJAS", dni = "71234567", phone = "987111222",
            address = "Calle Las Flores 88", location = LatLng(-11.2296, -77.3861),
            selectedPlace = Place(id = "1", name = "la villa"),
            installationType = InstallationType.FIBER,
            selectedPlan = PlanResponse("1", "Basico", 50.0, "200", "200", InstallationType.FIBER),
            selectedNapBox = NapBoxResponse(code = "LV-15-5", address = "arco", placeName = "La Villa", placeId = 1),
            selectedOnu = onu,
            wifiSsid24 = "Mendoza_Fibra",
            wifiPassword24 = "k7Rm2pQx9v",
            facadePhotoUrl = "https://example.test/facade.jpg",
        )
        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(
                    formState = RegisterSubscriptionState(
                        registerSubscriptionForm = form,
                        wizardStep = RegisterSubscriptionWizardStep.CONFIRMATION,
                        preauthorizationEnabled = true,
                        preauthorizationOperation = OnuRegistrationOperation("op-1", "ZTEGC8F01A22", null, "READY_FOR_FORM", "READY_FOR_FORM", 2),
                    ),
                    onIntent = { intents += it },
                )
            }
        }
    }

    @Test
    fun `review groups the data by step in the app order`() {
        show()

        composeRule.onNodeWithText("Revisa antes de registrar").assertExists()
        composeRule.onNodeWithText("Autorizada en la OLT").assertExists()
        composeRule.onNodeWithText("Lucía Mendoza Rojas").assertExists()
        composeRule.onNodeWithText("71234567 · 987 111 222").assertExists()
        composeRule.onNodeWithText("Fibra · Basico").assertExists()
        composeRule.onNodeWithText("S/ 50.00 al mes · 200/200 Mbps").assertExists()
        composeRule.onNodeWithText("LV-15-5").assertExists()
        composeRule.onNodeWithText("Mendoza_Fibra").assertExists()
        composeRule.onNodeWithText("••••••••••").assertExists()
        composeRule.onAllNodesWithText("Editar").assertCountEquals(3)
    }

    @Test
    fun `edit goes back to the step that holds the data`() {
        show()

        composeRule.onNodeWithContentDescription("Editar cliente").performClick()
        composeRule.onNodeWithContentDescription("Editar servicio").performClick()

        assertEquals(
            listOf(
                RegisterSubscriptionIntent.EditStepClicked(RegisterSubscriptionWizardStep.CLIENT_LOCATION),
                RegisterSubscriptionIntent.EditStepClicked(RegisterSubscriptionWizardStep.INSTALLATION),
            ),
            intents,
        )
    }

    @Test
    fun `phones read in groups of three`() {
        assertEquals("987 111 222", phoneGroups("987111222"))
        assertTrue(phoneGroups("12345").contains("12345"))
    }
}
