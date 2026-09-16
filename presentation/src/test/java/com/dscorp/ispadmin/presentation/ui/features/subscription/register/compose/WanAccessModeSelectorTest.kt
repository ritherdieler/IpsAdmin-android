package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.AccessMode
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class WanAccessModeSelectorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `defaults to PPPoE and selecting static updates callback`() {
        var selected by mutableStateOf(AccessMode.PPPOE_DYNAMIC)

        composeRule.setContent {
            MaterialTheme {
                WanAccessModeSelector(
                    selected = selected,
                    onSelected = { selected = it },
                )
            }
        }

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ACCESS_MODE)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ACCESS_MODE_PPPOE)
            .assertIsDisplayed()
            .assertIsSelected()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ACCESS_MODE_STATIC_IP)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()
        assertEquals(AccessMode.STATIC_IP, selected)
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ACCESS_MODE_STATIC_IP)
            .assertIsSelected()
    }
}
