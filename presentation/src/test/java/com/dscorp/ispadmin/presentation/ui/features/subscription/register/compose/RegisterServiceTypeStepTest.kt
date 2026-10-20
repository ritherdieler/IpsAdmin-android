package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.InstallationType
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
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h2400dp")
class RegisterServiceTypeStepTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val intents = mutableListOf<RegisterSubscriptionIntent>()

    private fun show(
        chosen: Boolean = false,
        type: InstallationType = InstallationType.FIBER,
        error: String? = null,
    ) {
        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(
                    formState = RegisterSubscriptionState(
                        registerSubscriptionForm = RegisterSubscriptionFormState(installationType = type),
                        wizardStep = RegisterSubscriptionWizardStep.SERVICE_TYPE,
                        preauthorizationEnabled = true,
                        serviceTypeChosen = chosen,
                        serviceTypeError = error,
                    ),
                    onIntent = { intents += it },
                )
            }
        }
    }

    @Test
    fun `shows the three options and the title`() {
        show()

        composeRule.onNodeWithText("¿Qué servicio vas a registrar?").assertExists()
        composeRule.onNodeWithText("Fibra con internet").assertExists()
        composeRule.onNodeWithText("Solo TV con receptor CATV").assertExists()
        composeRule.onNodeWithText("Inalámbrico").assertExists()
    }

    @Test
    fun `choosing solo TV sends ServiceTypeSelected`() {
        show()

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SERVICE_TYPE_TV).performClick()

        assertEquals(listOf(RegisterSubscriptionIntent.ServiceTypeSelected(InstallationType.ONLY_TV_FIBER)), intents)
    }

    @Test
    fun `continue is enabled only after choosing`() {
        show(chosen = false)
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE).assertIsNotEnabled()
        composeRule.onNodeWithText("Elige un tipo de servicio").assertExists()
    }

    @Test
    fun `continue is enabled and the footer names the choice`() {
        show(chosen = true, type = InstallationType.ONLY_TV_FIBER)

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE).assertIsEnabled()
        composeRule.onNodeWithText("Elegido: Solo TV con receptor CATV · sin ONU").assertExists()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SERVICE_TYPE_TV).assertIsSelected()
    }

    @Test
    fun `no option is selected before choosing`() {
        show(chosen = false, type = InstallationType.FIBER)

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SERVICE_TYPE_FIBER).assertIsNotSelected()
    }

    @Test
    fun `the error is shown under the options`() {
        show(chosen = true, type = InstallationType.FIBER, error = "No hay planes activos de Solo TV.")

        composeRule.onNodeWithText("No hay planes activos de Solo TV.").assertExists()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SERVICE_TYPE_FIBER).assertIsSelected()
    }

    @Test
    fun `closing the type step never starts an OLT cancellation`() {
        show(chosen = true, type = InstallationType.FIBER)

        composeRule.onNodeWithContentDescription("Cerrar registro").performClick()
        composeRule.onNodeWithText("Cancelar").performClick()

        assertTrue(intents.none { it is RegisterSubscriptionIntent.CancelOnuRegistration })
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ONU_REGISTRATION_CANCEL).assertDoesNotExist()
    }
}
