package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h2400dp")
class RegisterCatalogErrorTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val intents = mutableListOf<RegisterSubscriptionIntent>()
    private val backPresses = BackPressCounter()

    private fun show(error: String?) {
        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        backPresses.installOn(composeRule)
        composeRule.setContent {
            MaterialTheme {
                RegisterSubscriptionForm(
                    formState = RegisterSubscriptionState(
                        wizardStep = RegisterSubscriptionWizardStep.SERVICE_TYPE,
                        preauthorizationEnabled = true,
                        catalogLoadError = error,
                    ),
                    onIntent = { intents += it },
                )
            }
        }
    }

    @Test
    fun `shows the message and the retry button instead of the step and the navigation bar`() {
        show("No se pudo cargar el catálogo de registro. Revisa tu conexión y vuelve a intentar.")

        composeRule.onNodeWithText("No se pudo cargar el catálogo de registro. Revisa tu conexión y vuelve a intentar.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.CATALOG_RETRY).assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").assertIsDisplayed()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SERVICE_TYPE_FIBER).assertDoesNotExist()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE).assertDoesNotExist()
    }

    @Test
    fun `the message is announced as a polite live region`() {
        show("catalog unavailable")

        composeRule.onNodeWithText("catalog unavailable").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        )
    }

    @Test
    fun `retry sends RetryCatalogLoad`() {
        show("catalog unavailable")

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.CATALOG_RETRY).performClick()

        assertEquals(listOf<RegisterSubscriptionIntent>(RegisterSubscriptionIntent.RetryCatalogLoad), intents)
    }

    @Test
    fun `close still leaves the wizard while the error is shown`() {
        show("catalog unavailable")

        composeRule.onNodeWithContentDescription("Cerrar registro").performClick()

        assertEquals(1, backPresses.count)
    }

    @Test
    fun `without an error the retry view is not shown`() {
        show(null)

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.CATALOG_RETRY).assertDoesNotExist()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SERVICE_TYPE_FIBER).assertIsDisplayed()
    }
}
