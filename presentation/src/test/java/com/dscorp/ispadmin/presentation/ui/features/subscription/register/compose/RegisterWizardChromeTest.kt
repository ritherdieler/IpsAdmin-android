package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1400dp")
class RegisterWizardChromeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val zte = Onu("1", "1", "1", "t", "ZTE F670L", "GPON", "5", "ZTEGC8F01A22")
    private val huawei = Onu("1", "1", "1", "t", "HG8145V5", "GPON", "2", "HWTC7B21C0D4")
    private val intents = mutableListOf<RegisterSubscriptionIntent>()

    private fun show(state: RegisterSubscriptionState) {
        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent { MaterialTheme { RegisterSubscriptionForm(formState = state, onIntent = { intents += it }) } }
    }

    private fun onuStep(selected: Onu? = null) = RegisterSubscriptionState(
        registerSubscriptionForm = RegisterSubscriptionFormState(onuList = listOf(zte, huawei), selectedOnu = selected),
        wizardStep = RegisterSubscriptionWizardStep.ONU_SELECTION,
        preauthorizationEnabled = true,
    )

    @Test
    fun `the header follows the app order with the step count`() {
        show(onuStep())

        composeRule.onNodeWithText("Nueva suscripción").assertExists()
        composeRule.onNodeWithText("1 de 4").assertExists()
        composeRule.onNodeWithContentDescription("Paso 1 de 4: Equipo").assertExists()
    }

    @Test
    fun `onus are listed with model and port and can be filtered by serial`() {
        show(onuStep())

        composeRule.onNodeWithText("HWTC7B21C0D4").assertExists()
        composeRule.onNodeWithText("ZTE F670L · puerto 1/5").assertExists()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ONU_SEARCH).performTextInput("zteg")
        composeRule.onNodeWithText("HWTC7B21C0D4").assertDoesNotExist()
        composeRule.onNodeWithText("ZTEGC8F01A22").performClick()

        assertEquals(RegisterSubscriptionIntent.OnuSelected(zte), intents.last())
    }

    @Test
    fun `the footer names the chosen onu and authorizes it`() {
        show(onuStep(selected = zte))

        composeRule.onNodeWithText("ONU elegida: ZTEGC8F01A22 · OLT 1 · puerto 1/5").assertExists()
        composeRule.onNodeWithText("Autorizar y continuar").assertIsEnabled().performClick()

        assertTrue(RegisterSubscriptionIntent.WizardContinueClicked in intents)
    }

    @Test
    fun `closing asks to cancel the registration`() {
        show(onuStep())

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ONU_REGISTRATION_CANCEL).performClick()

        assertEquals(listOf(RegisterSubscriptionIntent.CancelOnuRegistration), intents)
    }

    @Test
    fun `the client step is second and has no way back to the onu`() {
        show(onuStep(selected = zte).copy(wizardStep = RegisterSubscriptionWizardStep.CLIENT_LOCATION))

        composeRule.onNodeWithText("2 de 4").assertExists()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_BACK).assertDoesNotExist()
    }
}
