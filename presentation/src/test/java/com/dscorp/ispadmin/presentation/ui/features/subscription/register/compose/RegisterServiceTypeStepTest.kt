package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
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
    private var exits = 0

    private fun show(
        chosen: Boolean = false,
        type: InstallationType = InstallationType.FIBER,
        error: String? = null,
    ) {
        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.addCallback(
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        exits++
                    }
                },
            )
        }
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
    fun `the footer names a wireless choice without ONU`() {
        show(chosen = true, type = InstallationType.WIRELESS)

        composeRule.onNodeWithText("Elegido: Inalámbrico · sin ONU").assertExists()
    }

    @Test
    fun `the option cards are announced as one selection group`() {
        show()

        val inGroup = hasAnyAncestor(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        listOf(
            RegisterSubscriptionTestTags.SERVICE_TYPE_FIBER,
            RegisterSubscriptionTestTags.SERVICE_TYPE_TV,
            RegisterSubscriptionTestTags.SERVICE_TYPE_WIRELESS,
        ).forEach { composeRule.onNode(hasTestTag(it) and inGroup).assertExists() }
    }

    @Test
    fun `the cancel action leaves the screen`() {
        show()

        composeRule.onNodeWithText("Cancelar").performClick()

        assertEquals(1, exits)
        assertTrue(intents.none { it is RegisterSubscriptionIntent.CancelOnuRegistration })
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

        assertTrue(exits > 0)
        assertTrue(intents.none { it is RegisterSubscriptionIntent.CancelOnuRegistration })
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ONU_REGISTRATION_CANCEL).assertDoesNotExist()
    }
}
