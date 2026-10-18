package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.AccessMode
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionTestTags
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
class RegisterServiceStepTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val intents = mutableListOf<RegisterSubscriptionIntent>()
    private val basico = PlanResponse("1", "Basico", 50.0, "200", "200", InstallationType.FIBER)
    private val duo = PlanResponse("2", "Duo basico 80", 80.0, "200", "200", InstallationType.FIBER)
    private val home = LatLng(-11.2296, -77.3861)
    private val near = NapBoxResponse(code = "LV-15-5", address = "arco de ingreso a primavera", latitude = -11.2280, longitude = -77.3861,
        placeName = "La Villa", placeId = 1)
    private val far = NapBoxResponse(code = "LV-1-7", address = "caja poste madera", latitude = -11.2256, longitude = -77.3861,
        placeName = "La Villa", placeId = 1)

    private fun show(form: RegisterSubscriptionFormState) {
        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(
                    formState = RegisterSubscriptionState(
                        registerSubscriptionForm = form,
                        wizardStep = RegisterSubscriptionWizardStep.INSTALLATION,
                        preauthorizationEnabled = true,
                    ),
                    onIntent = { intents += it },
                )
            }
        }
    }

    private val fiber = RegisterSubscriptionFormState(
        installationType = InstallationType.FIBER,
        planList = listOf(basico, duo),
        selectedPlan = basico,
        location = home,
        selectedPlace = Place(id = "1", name = "La Villa"),
        napBoxList = listOf(far, near),
        selectedNapBox = near,
        wifiSsid24 = "Mendoza_Fibra",
    )

    @Test
    fun `distances read in metres and kilometres`() {
        assertEquals("180 m", distanceLabel(178.4))
        assertEquals("1.2 km", distanceLabel(1_249.0))
        assertEquals(178.0, napDistanceMeters(home, near)!!, 3.0)
        assertEquals(null, napDistanceMeters(null, near))
    }

    @Test
    fun `generated wifi keys are long enough and avoid look-alike characters`() {
        repeat(20) {
            val key = generateWifiPassword()
            assertEquals(10, key.length)
            assertTrue(key.none { it in "0O1lI" })
        }
    }

    @Test
    fun `type, plan and access are chosen on the page`() {
        show(fiber)

        composeRule.onNodeWithText("Plan · Fibra (2 activos)").assertExists()
        composeRule.onNodeWithText("S/ 80.00").assertExists()
        composeRule.onNodeWithText("Inalámbrico").performClick()
        composeRule.onNodeWithText("Duo basico 80").performClick()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ACCESS_MODE_STATIC_IP).performClick()

        assertEquals(
            listOf(
                RegisterSubscriptionIntent.InstallationTypeSelected(InstallationType.WIRELESS),
                RegisterSubscriptionIntent.PlanSelected(duo),
                RegisterSubscriptionIntent.AccessModeSelected(AccessMode.STATIC_IP),
            ),
            intents,
        )
    }

    @Test
    fun `the nap card shows the chosen box and the sheet lists the nearest first`() {
        show(fiber)

        composeRule.onNodeWithText("LV-15-5").assertExists()
        composeRule.onNodeWithText("180 m", substring = true).assertExists()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.NAP_BOX).performScrollTo().performClick()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.NAP_SHEET).assertExists()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.napItem(1)).performClick()

        assertEquals(RegisterSubscriptionIntent.NapBoxSelected(far), intents.last())
    }

    @Test
    fun `wifi shows the derived 5 GHz name and can generate a key`() {
        show(fiber)

        composeRule.onNodeWithText("5 GHz usará Mendoza_Fibra - 5G").performScrollTo().assertExists()
        composeRule.onNodeWithText("Generar").performScrollTo().performClick()

        val generated = intents.filterIsInstance<RegisterSubscriptionIntent.WifiPassword24Changed>().single()
        assertEquals(10, generated.value.length)
    }
}
