package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import android.Manifest
import android.content.Intent
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.dscorp.ispadmin.presentation.ui.features.main.MainActivity
import com.dscorp.ispadmin.presentation.ui.features.main.MainNavTestTags
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit
import com.google.android.material.R as MaterialR

/** TextInputLayout whose floating hint equals [hint]. */
private fun textInputLayoutWithHint(hint: String): Matcher<View> = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("TextInputLayout hint=\"$hint\"")
    }

    override fun matchesSafely(item: View): Boolean =
        item is TextInputLayout && item.hint?.toString() == hint
}

/** EditText nested under a Material TextInputLayout with the given floating hint. */
private fun editTextUnderHint(hint: String): Matcher<View> = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("EditText under TextInputLayout hint=\"$hint\"")
    }

    override fun matchesSafely(item: View): Boolean {
        if (item !is android.widget.EditText) return false
        var parent = item.parent
        while (parent is View) {
            if (parent is TextInputLayout && parent.hint?.toString() == hint) return true
            parent = parent.parent
        }
        return false
    }
}

/** Mirrors login tags for androidTest (avoids classpath quirk with LoginTestTags). */
private object E2eLoginTags {
    const val USERNAME = "login_username"
    const val PASSWORD = "login_password"
    const val SUBMIT = "login_submit"
}

/**
 * E2E FIBER: login → registro → primera ONU del dropdown → éxito TR-069.
 * Cleanup MySQL/SmartOLT/Firebase lo orquesta scripts/e2e_register_fiber_espresso.sh.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FiberRegisterFirstOnuE2ETest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(permissionRule).around(composeRule)

    private val args = InstrumentationRegistry.getArguments()
    private val username = args.getString("e2e.user") ?: "dscorp"
    private val password = args.getString("e2e.password") ?: "nohacker"
    private val dni = args.getString("e2e.dni") ?: ("9" + (System.currentTimeMillis() % 10_000_000).toString().padStart(7, '0')).take(8)
    private val placeHint = args.getString("e2e.place") ?: E2ePlaceLocationFixture.PLACE_NAME
    private val wifiSsid = args.getString("e2e.wifiSsid") ?: "mimiwifi"
    private val wifiPass = args.getString("e2e.wifiPass") ?: "MimiWifi24pass"
    private val onuSn = E2eOnuSnResolver.resolve(args.getString("e2e.onuSn"))
    private val napCode = E2eNapCodeResolver.resolve(args.getString("e2e.napCode"))
    private val geoLat = args.getString("e2e.lat") ?: E2ePlaceLocationFixture.LATITUDE
    private val geoLon = args.getString("e2e.lon") ?: E2ePlaceLocationFixture.LONGITUDE
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun registerFiber_withFirstOnu_reachesSuccess() {
        composeRule.waitUntil(timeoutMillis = 30_000) {
            runCatching {
                composeRule.onAllNodes(androidx.compose.ui.test.isRoot()).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        composeRule.waitForIdle()

        loginIfNeeded()
        openRegisterSubscription()
        ensureLocationReady()
        // Wait until catalog load finishes; fields stay disabled while isLoading.
        waitUntilTag(RegisterSubscriptionTestTags.FORM_READY, timeoutMs = 120_000)
        waitUntilEnabled(RegisterSubscriptionTestTags.FIRST_NAME, timeoutMs = 30_000, requireFormReady = true)

        // Step 1: Cliente + ubicación
        fillClientFields()
        selectPlaceAndAddress()
        selectCurrentLocation()
        clickWizardContinue()
        waitUntilTag(RegisterSubscriptionTestTags.INSTALLATION_TYPE, timeoutMs = 30_000)

        // Step 2: Instalación FIBER
        completeInstallationStepAndAdvance()

        // Step 3: Confirmación
        injectFacadePhoto()
        waitUntilEnabledSubmit(timeoutMs = 60_000)
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SUBMIT)
            .assertIsEnabled()
            .performClick()

        waitUntilAnyTag(
            tags = listOf(
                RegisterSubscriptionTestTags.PROGRESS_OVERLAY,
                RegisterSubscriptionTestTags.SUCCESS_FULLSCREEN,
            ),
            timeoutMs = 30_000,
        )
        waitUntilTag(RegisterSubscriptionTestTags.SUCCESS_FULLSCREEN, timeoutMs = 360_000)

        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SUCCESS_FULLSCREEN)
            .assertIsDisplayed()
        waitUntilTag(
            RegisterSubscriptionTestTags.oltProvisionStatus("COMPLETE"),
            timeoutMs = 180_000,
        )
        waitUntilTag(
            RegisterSubscriptionTestTags.tr069ProvisionStatus("COMPLETE"),
            timeoutMs = 300_000,
        )
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.oltProvisionStatus("COMPLETE"))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.OLT_STATUS_MESSAGE)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.tr069ProvisionStatus("COMPLETE"))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.TR069_STATUS_MESSAGE)
            .assertIsDisplayed()
        val tr069Text = composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.TR069_STATUS_MESSAGE)
            .fetchSemanticsNodes()
            .first()
            .config
            .toString()
        assertThat(tr069Text).contains("TR-069")
        val tr069Upper = tr069Text.uppercase()
        assertThat(tr069Upper).doesNotContain("PENDIENTE")
        assertThat(tr069Upper).doesNotContain("ESPERE")
        assertThat(tr069Upper.contains("MANUAL_REQUIRED") ||
            (tr069Upper.contains("MANUAL") && !tr069Upper.contains("NO REQUIERE"))).isFalse()
    }

    private fun loginIfNeeded() {
        composeRule.waitForIdle()
        // After pm clear, splash/nav can lag — wait for login OR already-authenticated drawer.
        waitUntilAnyTag(
            tags = listOf(E2eLoginTags.USERNAME, MainNavTestTags.OPEN_DRAWER),
            timeoutMs = 90_000,
        )
        val loginVisible = composeRule.onAllNodesWithTag(E2eLoginTags.USERNAME)
            .fetchSemanticsNodes()
            .isNotEmpty()
        if (!loginVisible) return

        waitUntilEnabled(E2eLoginTags.USERNAME, timeoutMs = 30_000)
        composeRule.onNodeWithTag(E2eLoginTags.USERNAME).performClick()
        composeRule.onNodeWithTag(E2eLoginTags.USERNAME).performTextClearance()
        composeRule.onNodeWithTag(E2eLoginTags.USERNAME).performTextInput(username)
        composeRule.onNodeWithTag(E2eLoginTags.PASSWORD).performTextClearance()
        composeRule.onNodeWithTag(E2eLoginTags.PASSWORD).performTextInput(password)
        closeSoftKeyboard()
        composeRule.onNodeWithTag(E2eLoginTags.SUBMIT).performClick()
        waitUntilTag(MainNavTestTags.OPEN_DRAWER, timeoutMs = 90_000)
    }

    private fun openRegisterSubscription() {
        waitUntilTag(MainNavTestTags.OPEN_DRAWER, timeoutMs = 60_000)
        composeRule.onNodeWithTag(MainNavTestTags.OPEN_DRAWER).performClick()
        waitUntilTag(RegisterSubscriptionNavTestTags.DRAWER_REGISTER, timeoutMs = 15_000)
        composeRule.onNodeWithTag(RegisterSubscriptionNavTestTags.DRAWER_REGISTER).performClick()
    }

    private fun ensureLocationReady() {
        composeRule.waitForIdle()
        val continueGate = composeRule.onAllNodesWithTag("location_setup_continue")
            .fetchSemanticsNodes()
        if (continueGate.isNotEmpty()) {
            composeRule.onNodeWithTag("location_setup_continue").performClick()
            Thread.sleep(1_500)
            device.waitForIdle(3_000)
            // Permission dialog may appear; GrantPermissionRule covers grant, but click continue again if needed.
            if (composeRule.onAllNodesWithTag("location_setup_continue").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("location_setup_continue").performClick()
            }
        }
        waitUntilTag(RegisterSubscriptionTestTags.FIRST_NAME, timeoutMs = 90_000)
    }

    private fun fillClientFields() {
        // Names must match ^[a-zA-Z\\s]+$ (no digits) — "E2e..." would keep Submit disabled.
        typeInto(RegisterSubscriptionTestTags.FIRST_NAME, "EeeFiber")
        typeInto(RegisterSubscriptionTestTags.LAST_NAME, "Prueba")
        typeInto(RegisterSubscriptionTestTags.DNI, dni)
        typeInto(RegisterSubscriptionTestTags.PHONE, "999888777")
        closeSoftKeyboard()
    }

    private fun typeInto(tag: String, value: String) {
        waitUntilEnabled(tag, timeoutMs = 60_000, requireFormReady = true)
        val editable = hasTestTag(tag).and(hasSetTextAction())
        val useUnmerged = runCatching {
            composeRule.onNode(editable.and(androidx.compose.ui.test.isEnabled()), useUnmergedTree = true)
                .assertExists()
            true
        }.getOrDefault(false)
        composeRule.onNode(editable, useUnmergedTree = useUnmerged)
            .performScrollTo()
            .performClick()
        composeRule.onNode(editable, useUnmergedTree = useUnmerged).performTextClearance()
        composeRule.onNode(editable, useUnmergedTree = useUnmerged).performTextInput(value)
    }

    private fun waitUntilEnabled(
        tag: String,
        timeoutMs: Long,
        requireFormReady: Boolean = false,
    ) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        val editable = hasTestTag(tag).and(hasSetTextAction()).and(androidx.compose.ui.test.isEnabled())
        while (System.nanoTime() < deadline) {
            if (requireFormReady) {
                val formReady = composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.FORM_READY)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
                if (!formReady) {
                    Thread.sleep(500)
                    composeRule.waitForIdle()
                    continue
                }
            }
            val unmergedOk = runCatching {
                composeRule.onNode(editable, useUnmergedTree = true).assertExists()
                true
            }.getOrDefault(false)
            if (unmergedOk) return
            val mergedOk = runCatching {
                composeRule.onNode(editable, useUnmergedTree = false).assertExists()
                true
            }.getOrDefault(false)
            if (mergedOk) return
            Thread.sleep(500)
            composeRule.waitForIdle()
        }
        throw AssertionError("Timeout waiting for enabled editable testTag=$tag after ${timeoutMs}ms")
    }

    private fun selectCurrentLocation() {
        selectManualLocation()
    }

    private fun selectManualLocation() {
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.LOCATION_METHOD_MANUAL)
            .performScrollTo()
            .performClick()
        waitUntilTag("map_coordinate_search", timeoutMs = 30_000)
        val pasted = "$geoLat, $geoLon"
        composeRule.onNodeWithTag("map_coordinate_search").performTextClearance()
        composeRule.onNodeWithTag("map_coordinate_search").performTextInput(pasted)
        composeRule.onNodeWithTag("map_coordinate_search_button").performClick()
        Thread.sleep(1_200)
        composeRule.onNodeWithTag("map_select_location_button").performClick()
        waitUntilTag(RegisterSubscriptionTestTags.LOCATION_COORDINATES, timeoutMs = 30_000)
        val shown = runCatching {
            composeRule.onNodeWithTag(RegisterSubscriptionTestTags.LOCATION_COORDINATES)
                .fetchSemanticsNode()
                .config
                .toString()
        }.getOrDefault("")
        if (!shown.contains(geoLat) || !shown.contains(geoLon)) {
            throw AssertionError(
                "Location not inside lab fixture after map select. shown=$shown expected=$pasted. " +
                    "Selecting before search/camera settle uses DEFAULT_MANUAL_MAP_CAMERA (San Jerónimo / La Villa)."
            )
        }
    }

    private fun clickWizardContinue() {
        waitUntilTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE, timeoutMs = 15_000)
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE)
            .assertIsEnabled()
            .performClick()
        composeRule.waitForIdle()
        Thread.sleep(500)
    }

    private fun selectPlaceAndAddress() {
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.PLACE)
            .performScrollTo()
        // Open Lugar dropdown and pick first match for the lab place hint.
        onView(
            allOf(
                withId(MaterialR.id.text_input_end_icon),
                isDescendantOfA(textInputLayoutWithHint("Lugar")),
                isDisplayed(),
            )
        ).perform(click())
        Thread.sleep(800)
        val matched = runCatching {
            onData(org.hamcrest.Matchers.hasToString(org.hamcrest.Matchers.containsString(placeHint)))
                .inRoot(RootMatchers.isPlatformPopup())
                .atPosition(0)
                .perform(click())
            true
        }.getOrDefault(false)
        if (!matched) {
            onView(editTextUnderHint("Lugar")).perform(click(), replaceText(placeHint))
            Thread.sleep(800)
            onData(anything())
                .inRoot(RootMatchers.isPlatformPopup())
                .atPosition(0)
                .perform(click())
        }
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ADDRESS)
            .performScrollTo()
            .performTextInput("Jr EEE Lab ciento veintitres")
        closeSoftKeyboard()
    }


    private fun selectOnu() {
        if (E2eOnuSnResolver.shouldSelectFirstOnu(onuSn)) {
            selectFirstOnu()
        } else {
            selectOnuBySn(requireNotNull(onuSn))
        }
    }

    private fun selectFirstOnu() {
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.REFRESH_ONU)
            .performScrollTo()
            .performClick()
        Thread.sleep(3_000)
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ONU)
            .performScrollTo()
            .performClick()
        waitUntilTag(RegisterSubscriptionTestTags.onuItem(0), timeoutMs = 60_000)
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.onuItem(0)).performClick()
    }

    private fun selectOnuBySn(sn: String) {
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.REFRESH_ONU)
            .performScrollTo()
            .performClick()
        val listDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(60_000)
        var listReady = false
        while (System.nanoTime() < listDeadline && !listReady) {
            composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ONU)
                .performScrollTo()
                .performClick()
            composeRule.waitForIdle()
            listReady = composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.onuItem(0))
                .fetchSemanticsNodes()
                .isNotEmpty()
            if (!listReady) {
                Thread.sleep(800)
            }
        }
        if (!listReady) {
            throw AssertionError("ONU lab $sn no en unconfigured_onus")
        }

        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(60_000)
        while (System.nanoTime() < deadline) {
            var index = 0
            while (index < 80) {
                val tag = RegisterSubscriptionTestTags.onuItem(index)
                val nodes = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes()
                if (nodes.isEmpty()) break
                val text = nodes.first().config.toString()
                if (E2eOnuSnResolver.matches(text, sn)) {
                    composeRule.onNodeWithTag(tag).performClick()
                    return
                }
                index++
            }
            Thread.sleep(800)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(RegisterSubscriptionTestTags.ONU).performClick()
        }
        throw AssertionError("ONU lab $sn no en unconfigured_onus")
    }

    private fun fillWifi() {
        waitUntilTag(RegisterSubscriptionTestTags.WIFI_SSID_24, timeoutMs = 30_000)
        typeInto(RegisterSubscriptionTestTags.WIFI_SSID_24, wifiSsid)
        typeInto(RegisterSubscriptionTestTags.WIFI_PASSWORD_24, wifiPass)
        closeSoftKeyboard()
    }

    private fun ensureFiberPlanSelected() {
        waitUntilTag(RegisterSubscriptionTestTags.PLAN, timeoutMs = 30_000)
        val planText = runCatching {
            var text = ""
            composeRule.onNodeWithTag(RegisterSubscriptionTestTags.PLAN).performScrollTo()
            onView(editTextUnderHint("Plan")).check { view, _ ->
                text = (view as android.widget.EditText).text?.toString().orEmpty()
            }
            text
        }.getOrDefault("")
        if (planText.isNotBlank()) return
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.PLAN)
            .performScrollTo()
            .performClick()
        waitUntilTag(RegisterSubscriptionTestTags.planItem(0), timeoutMs = 10_000)
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.planItem(0)).performClick()
    }
    private fun completeInstallationStepAndAdvance() {
        ensureFiberPlanSelected()
        selectNapIfNeeded()
        selectOnu()
        fillWifi()
        closeSoftKeyboard()
        composeRule.waitForIdle()
        clickWizardContinue()
        waitUntilTag(RegisterSubscriptionTestTags.FACADE_PHOTO, timeoutMs = 30_000)
    }

    private fun selectNapIfNeeded() {
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.NAP_BOX)
            .performScrollTo()
        waitUntilNearbyNapSettled()
        waitUntilNapFieldReady()
        if (napFieldMatchesWanted()) return
        selectNapFromDropdown()
        if (!napFieldMatchesWanted() && !napFieldHasAnySelection()) {
            throw AssertionError(
                "Could not select NAP from dropdown (wanted=${napCode ?: "any"}; " +
                    "geo must be inside place.area and near the lab NAP)"
            )
        }
        if (napCode != null && !napFieldMatchesWanted()) {
            throw AssertionError(
                "NAP field=${napFieldText()} does not match wanted=$napCode; " +
                    "use E2ePlaceLocationFixture lat/lon (NO-001)"
            )
        }
    }

    private fun waitUntilNapFieldReady() {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(20_000)
        while (System.nanoTime() < deadline) {
            if (napFieldMatchesWanted()) return
            if (napCode == null && napFieldHasAnySelection()) return
            Thread.sleep(400)
            composeRule.waitForIdle()
        }
    }

    private fun waitUntilNearbyNapSettled() {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(30_000)
        while (System.nanoTime() < deadline) {
            val loading = composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.NEARBY_NAP_LOADING)
                .fetchSemanticsNodes()
                .isNotEmpty()
            if (!loading) return
            Thread.sleep(400)
            composeRule.waitForIdle()
        }
    }

    private fun napFieldText(): String? = runCatching {
        var text = ""
        onView(editTextUnderHint("Caja Nap")).check { view, _ ->
            text = (view as android.widget.EditText).text?.toString().orEmpty()
        }
        text
    }.getOrNull()

    private fun napFieldHasAnySelection(): Boolean = napFieldText().orEmpty().isNotBlank()

    private fun napFieldMatchesWanted(): Boolean {
        val text = napFieldText().orEmpty()
        val wanted = napCode
        if (wanted != null) return E2eNapCodeResolver.matches(text, wanted)
        return text.isNotBlank()
    }

    private fun selectNapFromDropdown() {
        val wanted = napCode
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(45_000)
        var selected = false
        while (System.nanoTime() < deadline && !selected) {
            selected = runCatching {
                composeRule.onNodeWithTag(RegisterSubscriptionTestTags.NAP_BOX)
                    .performScrollTo()
                onView(
                    allOf(
                        withId(MaterialR.id.text_input_end_icon),
                        isDescendantOfA(textInputLayoutWithHint("Caja Nap")),
                        isDisplayed(),
                    )
                ).perform(click())
                Thread.sleep(800)
                if (wanted != null) {
                    onData(org.hamcrest.Matchers.hasToString(org.hamcrest.Matchers.containsString(wanted)))
                        .inRoot(RootMatchers.isPlatformPopup())
                        .atPosition(0)
                        .perform(click())
                } else {
                    onData(anything())
                        .inRoot(RootMatchers.isPlatformPopup())
                        .atPosition(0)
                        .perform(click())
                }
                true
            }.getOrDefault(false)
            if (!selected && wanted != null) {
                selected = runCatching {
                    device.findObject(
                        androidx.test.uiautomator.UiSelector().textContains(wanted)
                    ).click()
                    true
                }.getOrDefault(false)
            }
            if (!selected) {
                Thread.sleep(800)
                composeRule.waitForIdle()
            }
        }
    }

    private fun injectFacadePhoto() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        val out = File(target.cacheDir, "facade_e2e.jpg")
        instrumentation.context.assets.open("facade_e2e.jpg").use { input ->
            out.outputStream().use { input.copyTo(it) }
        }
        assertThat(out.exists()).isTrue()
        val intent = Intent(RegisterSubscriptionDebugActions.SET_FACADE_PHOTO).apply {
            setPackage(target.packageName)
            putExtra(RegisterSubscriptionDebugActions.EXTRA_PATH, out.absolutePath)
        }
        target.sendBroadcast(intent)
        Thread.sleep(1_500)
        composeRule.waitForIdle()
    }

    private fun waitUntilEnabledSubmit(timeoutMs: Long) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            val ok = runCatching {
                composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SUBMIT).assertIsEnabled()
                true
            }.getOrDefault(false)
            if (ok) return
            Thread.sleep(500)
            composeRule.waitForIdle()
        }
        throw AssertionError(
            "Timeout waiting for enabled Submit (wizard step 3: facade photo still missing?)"
        )
    }

    private fun waitUntilTag(tag: String, timeoutMs: Long) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            val found = runCatching {
                composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
            if (found) {
                return
            }
            Thread.sleep(400)
            runCatching { composeRule.waitForIdle() }
        }
        throw AssertionError("Timeout waiting for testTag=$tag after ${timeoutMs}ms")
    }

    private fun waitUntilAnyTag(tags: List<String>, timeoutMs: Long) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            val found = runCatching {
                tags.any { composeRule.onAllNodesWithTag(it).fetchSemanticsNodes().isNotEmpty() }
            }.getOrDefault(false)
            if (found) {
                return
            }
            Thread.sleep(400)
            runCatching { composeRule.waitForIdle() }
        }
        throw AssertionError("Timeout waiting for any of $tags after ${timeoutMs}ms")
    }
}
