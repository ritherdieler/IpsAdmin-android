package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import android.Manifest
import android.content.Intent
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
import com.dscorp.ispadmin.domain.model.AccessMode
import com.dscorp.ispadmin.presentation.ui.features.main.MainActivity
import com.dscorp.ispadmin.presentation.ui.features.main.MainNavTestTags
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.google.android.material.R as MaterialR

private const val PLACE_FIELD_HINT = "Distrito o localidad *"

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
 * Cleanup MySQL/Gateway/Firebase lo orquesta scripts/e2e_register_fiber_espresso.sh.
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
    private val firstName = args.getString("e2e.firstName") ?: "EeeFiber"
    private val lastName = args.getString("e2e.lastName") ?: "Prueba"
    private val accessMode = E2eAccessModeResolver.resolve(args.getString("e2e.accessMode"))
    private val onuSn = E2eOnuSnResolver.resolve(args.getString("e2e.onuSn"))
    private val napCode = E2eNapCodeResolver.resolve(args.getString("e2e.napCode"))
    private val geoLat = args.getString("e2e.lat") ?: E2ePlaceLocationFixture.LATITUDE
    private val geoLon = args.getString("e2e.lon") ?: E2ePlaceLocationFixture.LONGITUDE
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun applyE2eAccessMode() {
        E2eAccessModeResolver.override = args.getString("e2e.accessMode")
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "e2e-keep-open").delete()
    }

    @After
    fun clearE2eAccessMode() {
        E2eAccessModeResolver.override = null
    }

    @Test
    fun registerFiber_withFirstOnu_reachesSuccess() {
        try {
        composeRule.waitUntil(timeoutMillis = 30_000) {
            runCatching {
                composeRule.onAllNodes(androidx.compose.ui.test.isRoot()).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }

        loginIfNeeded()
        openRegisterSubscription()
        ensureLocationReady()
        // Wait until catalog load finishes; fields stay disabled while isLoading.
        waitUntilTag(RegisterSubscriptionTestTags.FORM_READY, timeoutMs = 120_000)

        // A resumed installation or confirmation draft may have an obsolete place.
        if (isTagVisible(RegisterSubscriptionTestTags.SUBMIT)) {
            composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_BACK).performClick()
            waitUntilTag(RegisterSubscriptionTestTags.INSTALLATION_TYPE, timeoutMs = 15_000)
        }
        if (!isTagVisible(RegisterSubscriptionTestTags.FIRST_NAME) &&
            isTagVisible(RegisterSubscriptionTestTags.INSTALLATION_TYPE)
        ) {
            composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_BACK).performClick()
            waitUntilTag(RegisterSubscriptionTestTags.FIRST_NAME, timeoutMs = 15_000)
        }

        if (onuSn != null && isTagVisible(RegisterSubscriptionTestTags.FIRST_NAME)) {
            waitUntilOnuAuthorized(onuSn, timeoutMs = 15_000)
        }

        // Step 1: Cliente + ubicación
        if (isTagVisible(RegisterSubscriptionTestTags.FIRST_NAME)) {
            waitUntilEnabled(RegisterSubscriptionTestTags.FIRST_NAME, timeoutMs = 30_000, requireFormReady = true)
            fillClientFields()
            selectPlaceAndAddress()
            selectCurrentLocation()
            if (args.getString("e2e.stopAfterClientForm") == "true") {
                composeRule.onNodeWithTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE)
                    .assertIsEnabled()
                assertThat(
                    composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.PROGRESS_OVERLAY)
                        .fetchSemanticsNodes(),
                ).isEmpty()
                assertThat(
                    composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.SUCCESS_FULLSCREEN)
                        .fetchSemanticsNodes(),
                ).isEmpty()
                return
            }
            clickWizardContinue()
        }

        // Step 2: Instalación. La foto de fachada vive en este paso; al continuar pasa a revisión.
        waitUntilTag(RegisterSubscriptionTestTags.INSTALLATION_TYPE, timeoutMs = 30_000)
        completeInstallationStep()
        injectFacadePhoto()
        clickWizardContinue()

        // Step 3: Confirmación
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
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.SUCCESS_SECTION_WIFI)
            .performScrollTo()
            .assertIsDisplayed()
        listOf(wifiSsid, wifiPass).forEach { expected ->
            val shown = composeRule.onAllNodes(
                hasText(expected) and hasAnyAncestor(hasTestTag(RegisterSubscriptionTestTags.SUCCESS_SECTION_WIFI)),
                useUnmergedTree = true,
            ).fetchSemanticsNodes()
            assertThat(shown).isNotEmpty()
        }
        holdAppOpen()
        } catch (failure: Throwable) {
            android.util.Log.e("FiberRegisterFirstOnuE2E", "E2E failed before activity teardown", failure)
            throw failure
        }
    }

    private fun holdAppOpen() {
        if (args.getString("e2e.keepOpen") == "false") return
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "e2e-keep-open").writeText("1")
        CountDownLatch(1).await()
    }

    private fun loginIfNeeded() {
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
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90)
        while (System.nanoTime() < deadline) {
            val loginStillVisible = composeRule.onAllNodesWithTag(E2eLoginTags.USERNAME)
                .fetchSemanticsNodes().isNotEmpty()
            if (!loginStillVisible) return
            Thread.sleep(500)
        }
        throw AssertionError("Login form remained visible after submitting credentials")
    }

    private fun openRegisterSubscription() {
        waitUntilTag(MainNavTestTags.OPEN_DRAWER, timeoutMs = 60_000)
        composeRule.onNodeWithTag(MainNavTestTags.OPEN_DRAWER).performClick()
        waitUntilTag(RegisterSubscriptionNavTestTags.DRAWER_REGISTER, timeoutMs = 15_000)
        composeRule.onNodeWithTag(RegisterSubscriptionNavTestTags.DRAWER_REGISTER).performClick()
    }

    private fun ensureLocationReady() {
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
        waitUntilTag(RegisterSubscriptionTestTags.FORM_READY, timeoutMs = 120_000)
        val entryDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(120_000)
        var onuSelectorVisibleSince: Long? = null
        while (System.nanoTime() < entryDeadline) {
            if (isTagVisible(RegisterSubscriptionTestTags.FIRST_NAME) ||
                isTagVisible(RegisterSubscriptionTestTags.INSTALLATION_TYPE) ||
                isTagVisible(RegisterSubscriptionTestTags.SUBMIT)) return
            val onuSelectorVisible = composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.ONU)
                .fetchSemanticsNodes().isNotEmpty()
            if (onuSelectorVisible) {
                val visibleSince = onuSelectorVisibleSince ?: System.nanoTime().also { onuSelectorVisibleSince = it }
                if (System.nanoTime() - visibleSince >= TimeUnit.SECONDS.toNanos(5)) break
            } else {
                onuSelectorVisibleSince = null
            }
            Thread.sleep(500)
        }
        if (isTagVisible(RegisterSubscriptionTestTags.INSTALLATION_TYPE) ||
            isTagVisible(RegisterSubscriptionTestTags.SUBMIT)) return
        if (composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.ONU)
                .fetchSemanticsNodes().isEmpty()) {
            throw AssertionError("Registration did not expose a client form or an ONU selector")
        }

        // Preauthorization is part of the wizard. The client form is not exposed
        // until the selected ONU reaches READY_FOR_FORM through ACS.
        waitUntilTag(RegisterSubscriptionTestTags.ONU, timeoutMs = 30_000)
        selectOnu()
        clickWizardContinue()
        waitForClientFormAfterAcsContact()
    }

    private fun isTagVisible(tag: String): Boolean = composeRule.onAllNodesWithTag(tag)
        .fetchSemanticsNodes().isNotEmpty()

    private fun waitForClientFormAfterAcsContact() {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(360_000)
        while (System.nanoTime() < deadline) {
            if (composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.FIRST_NAME)
                    .fetchSemanticsNodes().isNotEmpty()) return
            val retry = composeRule.onAllNodes(
                hasText("Reintentar") and androidx.compose.ui.test.isEnabled(),
            ).fetchSemanticsNodes()
            if (retry.isNotEmpty()) {
                composeRule.onNode(hasText("Reintentar") and androidx.compose.ui.test.isEnabled())
                    .performClick()
            }
            Thread.sleep(15_000)
        }
        throw AssertionError("ONU did not reach READY_FOR_FORM after ACS retries")
    }

    private fun fillClientFields() {
        typeInto(RegisterSubscriptionTestTags.FIRST_NAME, firstName)
        typeInto(RegisterSubscriptionTestTags.LAST_NAME, lastName)
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
        val tag = RegisterSubscriptionTestTags.WIZARD_CONTINUE
        waitUntilTag(tag, timeoutMs = 15_000)
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(30_000)
        while (System.nanoTime() < deadline) {
            val enabled = runCatching {
                composeRule.onNodeWithTag(tag).assertIsEnabled()
                true
            }.getOrDefault(false)
            if (enabled) {
                composeRule.onNodeWithTag(tag).performClick()
                Thread.sleep(500)
                return
            }
            Thread.sleep(400)
        }
        val formReady = composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.FORM_READY)
            .fetchSemanticsNodes()
            .isNotEmpty()
        val catalogBusy = composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.CATALOG_LOADING_OVERLAY)
            .fetchSemanticsNodes()
            .isNotEmpty()
        val registrationBusy = composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.PROGRESS_OVERLAY)
            .fetchSemanticsNodes()
            .isNotEmpty()
        val serialNode = composeRule.onAllNodesWithText("Serial:", substring = true)
            .fetchSemanticsNodes()
            .firstOrNull()
            ?.config
            ?.toString()
        val cancelAction = composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.ONU_REGISTRATION_CANCEL)
            .fetchSemanticsNodes()
            .isNotEmpty()
        throw AssertionError(
            "Timeout waiting for enabled wizard continue after 30000ms " +
                "formReady=$formReady catalogBusy=$catalogBusy registrationBusy=$registrationBusy " +
                "cancelAction=$cancelAction serialNode=$serialNode"
        )
    }

    private fun selectPlaceAndAddress() {
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.PLACE)
            .performScrollTo()
        onView(editTextUnderHint(PLACE_FIELD_HINT)).perform(click(), replaceText(placeHint))
        val exactPlace = object : TypeSafeMatcher<Any>() {
            override fun describeTo(description: Description) {
                description.appendText("place containing $placeHint, ignoring case")
            }

            override fun matchesSafely(item: Any): Boolean =
                item.toString().contains(placeHint, ignoreCase = true)
        }
        onData(exactPlace)
            .inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        var selectedPlace = ""
        onView(editTextUnderHint(PLACE_FIELD_HINT)).check { view, _ ->
            selectedPlace = (view as android.widget.EditText).text?.toString().orEmpty()
        }
        assertThat(selectedPlace.lowercase()).contains(placeHint.lowercase())
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

    private fun selectAccessMode() {
        val tag = when (accessMode) {
            AccessMode.STATIC_IP -> RegisterSubscriptionTestTags.ACCESS_MODE_STATIC_IP
            AccessMode.PPPOE_DYNAMIC, AccessMode.PPPOE_FIXED ->
                RegisterSubscriptionTestTags.ACCESS_MODE_PPPOE
        }
        waitUntilTag(tag, timeoutMs = 15_000)
        composeRule.onNodeWithTag(tag)
            .performScrollTo()
            .performClick()
    }

    private fun completeInstallationStep() {
        ensureFiberPlanSelected()
        selectAccessMode()
        selectNapIfNeeded()
        if (!isTagVisible(RegisterSubscriptionTestTags.ONU_REGISTRATION_CANCEL) &&
            composeRule.onAllNodesWithTag(RegisterSubscriptionTestTags.ONU).fetchSemanticsNodes().isNotEmpty()
        ) {
            selectOnu()
        }
        fillWifi()
        closeSoftKeyboard()
        waitUntilTag(RegisterSubscriptionTestTags.FACADE_PHOTO, timeoutMs = 30_000)
        composeRule.onNodeWithTag(RegisterSubscriptionTestTags.FACADE_PHOTO)
            .performScrollTo()
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
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            val photoSelected = composeRule.onAllNodes(
                hasContentDescription("Cambiar foto de fachada"),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
            if (photoSelected) return
            Thread.sleep(250)
        }
        throw AssertionError("Debug photo injection did not select a facade photo")
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
        }
        throw AssertionError("Timeout waiting for testTag=$tag after ${timeoutMs}ms")
    }

    private fun waitUntilText(text: String, timeoutMs: Long, substring: Boolean = false) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            val found = runCatching {
                composeRule.onAllNodesWithText(text, substring = substring)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }.getOrDefault(false)
            if (found) return
            Thread.sleep(400)
        }
        throw AssertionError("Timeout waiting for text=$text after ${timeoutMs}ms")
    }

    private fun waitUntilOnuAuthorized(wantedSn: String, timeoutMs: Long) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            val confirmed = composeRule.onAllNodesWithText("autorizada", substring = true)
                .fetchSemanticsNodes()
                .any { node ->
                    E2eOnuSnResolver.matchesAuthorizedOnuText(node.config.toString(), wantedSn)
                }
            if (confirmed) return
            Thread.sleep(400)
        }
        throw AssertionError("Timeout waiting for authorization confirmation for the requested ONU")
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
        }
        throw AssertionError("Timeout waiting for any of $tags after ${timeoutMs}ms")
    }
}
