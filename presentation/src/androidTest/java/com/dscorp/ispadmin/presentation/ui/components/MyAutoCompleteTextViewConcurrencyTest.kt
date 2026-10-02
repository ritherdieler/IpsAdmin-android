package com.dscorp.ispadmin.presentation.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.dscorp.ispadmin.R
import org.hamcrest.Matchers.hasToString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MyAutoCompleteTextViewConcurrencyTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun externalSelectionUpdate_doesNotRaceWithAndroidViewTextWatcher() {
        val selectedItem = mutableStateOf<String?>(null)
        val items = mutableStateOf(listOf("la villa", "otro lugar"))
        composeRule.activity.setTheme(R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                MyAutoCompleteTextViewCompose(
                    modifier = Modifier.testTag("place_field"),
                    items = items.value,
                    label = "Distrito o localidad *",
                    selectedItem = selectedItem.value,
                    onItemSelected = { selectedItem.value = it },
                    onSelectionCleared = { selectedItem.value = null },
                    onTextChanged = { query ->
                        items.value = listOf("la villa", "otro lugar")
                            .filter { it.contains(query, ignoreCase = true) }
                    },
                )
            }
        }

        onView(withId(android.R.id.text1)).perform(click(), replaceText("la villa"))
        onData(hasToString("la villa"))
            .inRoot(isPlatformPopup())
            .perform(click())

        composeRule.onNodeWithTag("place_field").assertIsDisplayed()
        assert(selectedItem.value == "la villa")
    }
}
