package com.dscorp.ispadmin.presentation.ui.features.subscription.provisioning

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dscorp.ispadmin.domain.model.ProvisioningOperation
import com.dscorp.ispadmin.domain.model.ProvisioningProgress
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ProvisioningScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun `cancel asks for confirmation and shows operation reference`() {
        val intents = mutableListOf<ProvisioningIntent>()
        val state = ProvisioningUiState(progress = ProvisioningProgress(
            operation = ProvisioningOperation(id = "op-42", subscriptionId = 42, revision = 3, state = "FAILED"),
            canRetry = true, canCancel = true))
        compose.setContent { MaterialTheme { ProvisioningContent(state = state, onIntent = { intents += it }, onNavigate = {}) } }
        compose.onNodeWithText("Referencia: op-42").assertIsDisplayed()
        compose.onNodeWithTag("provisioning_cancel").performClick()
        assertEquals(listOf(ProvisioningIntent.AskCancel), intents)
        compose.onNodeWithTag("provisioning_new").assertDoesNotExist()
    }
}
