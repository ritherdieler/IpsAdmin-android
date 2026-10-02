package com.dscorp.ispadmin.presentation.ui.components

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import com.dscorp.ispadmin.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MyAutoCompleteTextViewComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectedItemUpdate_doesNotClearSelectionDuringAndroidViewUpdate() {
        val selectedItem = mutableStateOf<String?>(null)
        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                MyAutoCompleteTextViewCompose(
                    modifier = Modifier.testTag("place_field"),
                    items = listOf("la villa", "otro lugar"),
                    label = "Distrito o localidad *",
                    selectedItem = selectedItem.value,
                    onItemSelected = { selectedItem.value = it },
                    onSelectionCleared = { selectedItem.value = null },
                )
            }
        }

        composeRule.runOnIdle { selectedItem.value = "la villa" }

        composeRule.onNodeWithTag("place_field").assertIsDisplayed()
        assertEquals("la villa", selectedItem.value)
    }
}
