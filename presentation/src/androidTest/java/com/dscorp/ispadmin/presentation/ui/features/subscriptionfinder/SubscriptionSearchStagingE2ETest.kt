package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.dscorp.ispadmin.presentation.ui.features.main.MainActivity
import com.dscorp.ispadmin.presentation.ui.features.main.MainNavTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class SubscriptionSearchStagingE2ETest {

    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.POST_NOTIFICATIONS,
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(permissionRule).around(composeRule)

    private val args = InstrumentationRegistry.getArguments()
    private val username = args.getString("e2e.user") ?: "dscorp"
    private val password = args.getString("e2e.password") ?: "nohacker"
    private val dni = args.getString("e2e.dni") ?: ""

    @Test
    fun searchSubscriptionByDocument_showsResult() {
        require(dni.isNotBlank()) { "e2e.dni is required" }
        composeRule.waitUntil(timeoutMillis = 30_000) {
            runCatching {
                composeRule.onAllNodes(androidx.compose.ui.test.isRoot()).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        loginIfNeeded()
        openFinder()
        waitUntilTag(SubscriptionFinderTestTags.FILTER_DOCUMENT, timeoutMs = 30_000)
        composeRule.onNodeWithTag(SubscriptionFinderTestTags.FILTER_DOCUMENT).performClick()
        waitUntilTag(SubscriptionFinderTestTags.QUERY_DOCUMENT, timeoutMs = 15_000)
        val editable = hasTestTag(SubscriptionFinderTestTags.QUERY_DOCUMENT).and(hasSetTextAction())
        composeRule.onNode(editable).performClick()
        composeRule.onNode(editable).performTextClearance()
        composeRule.onNode(editable).performTextInput(dni)
        closeSoftKeyboard()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            runCatching {
                composeRule.onNodeWithText(dni, substring = true).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText(dni, substring = true).assertIsDisplayed()
    }

    private fun loginIfNeeded() {
        waitUntilAnyTag(listOf("login_username", MainNavTestTags.OPEN_DRAWER), 90_000)
        val loginVisible = composeRule.onAllNodesWithTag("login_username").fetchSemanticsNodes().isNotEmpty()
        if (!loginVisible) return
        composeRule.onNodeWithTag("login_username").performClick()
        composeRule.onNodeWithTag("login_username").performTextClearance()
        composeRule.onNodeWithTag("login_username").performTextInput(username)
        composeRule.onNodeWithTag("login_password").performTextClearance()
        composeRule.onNodeWithTag("login_password").performTextInput(password)
        closeSoftKeyboard()
        composeRule.onNodeWithTag("login_submit").performClick()
        waitUntilTag(MainNavTestTags.OPEN_DRAWER, 90_000)
    }

    private fun openFinder() {
        waitUntilTag(MainNavTestTags.OPEN_DRAWER, 60_000)
        composeRule.onNodeWithTag(MainNavTestTags.OPEN_DRAWER).performClick()
        waitUntilTag("drawer_nav_subscription_finder", 15_000)
        composeRule.onNodeWithTag("drawer_nav_subscription_finder").performClick()
    }

    private fun waitUntilTag(tag: String, timeoutMs: Long) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            val found = runCatching {
                composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
            if (found) return
            Thread.sleep(400)
            composeRule.waitForIdle()
        }
        throw AssertionError("Timeout waiting for tag $tag")
    }

    private fun waitUntilAnyTag(tags: List<String>, timeoutMs: Long) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            val found = tags.any { tag ->
                runCatching {
                    composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
                }.getOrDefault(false)
            }
            if (found) return
            Thread.sleep(400)
            composeRule.waitForIdle()
        }
        throw AssertionError("Timeout waiting for any of $tags")
    }
}
