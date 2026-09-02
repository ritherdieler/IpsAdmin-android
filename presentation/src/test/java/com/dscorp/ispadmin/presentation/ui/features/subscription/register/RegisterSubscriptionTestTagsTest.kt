package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class RegisterSubscriptionTestTagsTest {

    @Test
    fun `tags interactivos del formulario estan definidos y no vacios`() {
        RegisterSubscriptionTestTags.interactive.forEach { tag ->
            assertThat(tag).isNotEmpty()
        }
        assertThat(RegisterSubscriptionTestTags.onuItem(0)).isEqualTo("register_onu_item_0")
        assertThat(RegisterSubscriptionTestTags.planItem(0)).isEqualTo("register_plan_item_0")
        assertThat(RegisterSubscriptionNavTestTags.DRAWER_REGISTER).isNotEmpty()
        assertThat(RegisterSubscriptionDebugActions.SET_FACADE_PHOTO).contains("DEBUG_SET_FACADE_PHOTO")
    }

    @Test
    fun `formulario aplica testTags criticos del flujo fiber`() {
        val formSource = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/compose/RegisterSubscriptionForm.kt"
        ).readText()
        listOf(
            "RegisterSubscriptionTestTags.FIRST_NAME",
            "RegisterSubscriptionTestTags.LAST_NAME",
            "RegisterSubscriptionTestTags.DNI",
            "RegisterSubscriptionTestTags.PHONE",
            "RegisterSubscriptionTestTags.ADDRESS",
            "RegisterSubscriptionTestTags.SUBMIT",
            "RegisterSubscriptionTestTags.FORM_READY",
            "RegisterSubscriptionTestTags.FACADE_PHOTO",
            "RegisterSubscriptionTestTags.ONU",
            "RegisterSubscriptionTestTags.onuItem",
            "RegisterSubscriptionTestTags.PLAN",
            "RegisterSubscriptionTestTags.INSTALLATION_TYPE",
            "RegisterSubscriptionTestTags.PLACE",
            "RegisterSubscriptionTestTags.NAP_BOX",
            "RegisterSubscriptionTestTags.NEARBY_NAP_LOADING",
            "RegisterSubscriptionTestTags.WIZARD_CONTINUE",
            "RegisterSubscriptionTestTags.LOCATION_METHOD_CURRENT",
            "RegisterSubscriptionTestTags.LOCATION_METHOD_MANUAL",
        ).forEach { ref ->
            assertThat(formSource).contains(ref)
        }
        assertThat(RegisterSubscriptionTestTags.FORM_READY).isEqualTo("register_form_ready")
        assertThat(RegisterSubscriptionTestTags.WIZARD_CONTINUE).isEqualTo("wizard_continue")
        assertThat(RegisterSubscriptionTestTags.NEARBY_NAP_LOADING)
            .isEqualTo("register_nearby_nap_loading")
        assertThat(RegisterSubscriptionTestTags.oltProvisionStatus("COMPLETE"))
            .isEqualTo("olt_provision_status_COMPLETE")
        assertThat(RegisterSubscriptionTestTags.tr069ProvisionStatus("COMPLETE"))
            .isEqualTo("tr069_provision_status_COMPLETE")
        val e2eSource = File(
            "src/androidTest/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/FiberRegisterFirstOnuE2ETest.kt"
        ).readText()
        assertThat(e2eSource).contains("oltProvisionStatus(\"COMPLETE\")")
        assertThat(e2eSource).contains("tr069ProvisionStatus(\"COMPLETE\")")
        val screenSource = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/compose/RegisterSubscriptionScreen.kt"
        ).readText()
        assertThat(screenSource).contains("RegisterSubscriptionDebugActions.SET_FACADE_PHOTO")
        assertThat(screenSource).contains("OltStatusCard")
        assertThat(screenSource).contains("oltProvisionStatus(")
        assertThat(screenSource).contains("tr069ProvisionStatus(")
    }

    @Test
    fun `mapa manual expone tags de coordenadas para e2e staging`() {
        val mapSource = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/locationMapView/LocationSelectorComposeDialog.kt"
        ).readText()
        listOf(
            "map_coordinate_search",
            "map_coordinate_search_button",
            "map_select_location_button",
        ).forEach { tag ->
            assertThat(mapSource).contains(tag)
        }
    }
}
