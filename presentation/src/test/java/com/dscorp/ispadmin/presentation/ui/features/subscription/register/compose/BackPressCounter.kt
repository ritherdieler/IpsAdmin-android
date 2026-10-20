package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule

/** Counts the back presses (screen exits) the register form triggers; install it before `setContent`. */
class BackPressCounter {
    var count = 0
        private set

    fun installOn(composeRule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>) {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.addCallback(
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        count++
                    }
                },
            )
        }
    }
}
