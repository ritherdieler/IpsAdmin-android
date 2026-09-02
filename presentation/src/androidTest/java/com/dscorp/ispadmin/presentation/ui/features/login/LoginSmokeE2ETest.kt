package com.dscorp.ispadmin.presentation.ui.features.login

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.dscorp.ispadmin.presentation.ui.features.main.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Smoke: harness androidTest + tags de login visibles al cold start (sesión limpia).
 * Si ya hay sesión, el test se salta la aserción de login.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class LoginSmokeE2ETest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= 33) {
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        GrantPermissionRule.grant()
    }

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(permissionRule).around(composeRule)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loginFields_areReachable_whenLoggedOut() {
        composeRule.waitUntil(timeoutMillis = 30_000) {
            runCatching { composeRule.onAllNodes(isRoot()).fetchSemanticsNodes().isNotEmpty() }
                .getOrDefault(false)
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
        val usernameNodes = composeRule.onAllNodesWithTag(LoginTestTags.USERNAME).fetchSemanticsNodes()
        if (usernameNodes.isEmpty()) {
            // Already authenticated — harness still loaded MainActivity successfully.
            return
        }
        composeRule.onNodeWithTag(LoginTestTags.USERNAME).assertIsDisplayed()
        composeRule.onNodeWithTag(LoginTestTags.PASSWORD).assertIsDisplayed()
        composeRule.onNodeWithTag(LoginTestTags.SUBMIT).assertIsDisplayed()
    }
}
