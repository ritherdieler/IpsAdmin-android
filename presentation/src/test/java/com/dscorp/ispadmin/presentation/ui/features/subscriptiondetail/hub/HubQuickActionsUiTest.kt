package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.SubscriptionDetailTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HubQuickActionsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `fiber subscription with phone shows every quick action`() {
        val tapped = mutableListOf<HubAction>()
        compose.setContent {
            MaterialTheme { HubQuickActions(actions = HubAction.available(hasPhone = true, isFiber = true), onAction = { tapped += it }) }
        }

        compose.onNodeWithTag(SubscriptionDetailTestTags.CALL).assertExists()
        compose.onNodeWithTag(SubscriptionDetailTestTags.WHATSAPP).assertExists()
        compose.onNodeWithText("Registro ONU").assertExists()
        compose.onNodeWithText("Pagos").performClick()
        compose.onNodeWithText("Ticket").performClick()

        assertEquals(listOf(HubAction.PAYMENTS, HubAction.TICKET), tapped)
    }

    @Test
    fun `without phone or fiber the related actions are hidden`() {
        assertEquals(listOf(HubAction.PAYMENTS, HubAction.TICKET), HubAction.available(hasPhone = false, isFiber = false))
    }
}
