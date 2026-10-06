package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class E2ePlaceLocationFixtureTest {

    @Test
    fun `fixture uses stored 9 de octubre coordinates inside prod polygon`() {
        assertThat(E2ePlaceLocationFixture.PLACE_ID).isEqualTo(1)
        assertThat(E2ePlaceLocationFixture.PLACE_NAME).isEqualTo("9 de octubre")
        assertThat(E2ePlaceLocationFixture.LATITUDE).isEqualTo("-11.2156")
        assertThat(E2ePlaceLocationFixture.LONGITUDE).isEqualTo("-77.4107")
        assertThat(E2ePlaceLocationFixture.NEARBY_NAP_CODE).isEqualTo("NO-001")
        assertThat(E2ePlaceLocationFixture.pastedCoordinates()).isEqualTo("-11.2156, -77.4107")
    }

    @Test
    fun `staging espresso script delivers wifi credentials before cleanup`() {
        val script = File("../scripts/e2e_register_fiber_staging_espresso.sh").readText()
        val wifiBlock = script.indexOf("== WiFi credentials (before cleanup) ==")
        val promptAt = script.indexOf("¿Ejecutar hard cleanup ahora?")
        val cleanupBlock = script.indexOf("== post cleanup ==")
        assertThat(wifiBlock).isGreaterThan(-1)
        assertThat(promptAt).isGreaterThan(wifiBlock)
        assertThat(cleanupBlock).isGreaterThan(promptAt)
        assertThat(script).contains("wifi_24 ssid=\$E2E_WIFI_SSID password=\$E2E_WIFI_PASS")
        assertThat(script).contains("wifi_5 ssid=\$E2E_WIFI_SSID_5 password=\$E2E_WIFI_PASS")
        assertThat(script).contains("¿Ejecutar hard cleanup ahora?")
        assertThat(script).contains("--cleanup-mode)")
        assertThat(script).contains("ask|auto|skip")
        assertThat(script).contains("CLEANUP_MODE")
        assertThat(script).contains("--ask-cleanup")
        assertThat(script).contains("--auto-cleanup")
        assertThat(script).contains("--no-cleanup")
        assertThat(script).contains("post cleanup skipped (user)")
        assertThat(script).contains("SKIP_POST_CLEANUP")
        assertThat(script.indexOf("¿Ejecutar hard cleanup ahora?")).isGreaterThan(wifiBlock)
        assertThat(script).doesNotContain("== post cleanup (required) ==")
        assertThat(script).contains("--wifi-ssid)")
        assertThat(script).contains("--wifi-pass)")
        assertThat(script).contains("E2E_WIFI_SSID_5=\"\${E2E_WIFI_SSID} - 5G\"")
        assertThat(script).doesNotContain("E2E_WIFI_SSID_5:-")
        assertThat(script).doesNotContain("tr069-e2e-mk-ping.sh")
        assertThat(script).doesNotContain("MikroTik2 ping to assigned IP")
        assertThat(script).doesNotContain("MikroTik ping validation failed")
        assertThat(script).contains("== service-health collection ==")
        assertThat(script).doesNotContain("pilot_enabled")
        assertThat(script).doesNotContain("staging e2e must keep collection on")
    }

    @Test
    fun `staging espresso script defaults wifi by lab onu brand`() {
        val script = File("../scripts/e2e_register_fiber_staging_espresso.sh").readText()
        assertThat(script).contains("E2E_ONU_SN=\"\${E2E_ONU_SN:-ZTEGDC47BFFD}\"")
        assertThat(script).contains("lab-vsol-e2e-24")
        assertThat(script).contains("LabVsolWifi24!")
        assertThat(script).contains("ztelab")
        assertThat(script).contains("11111111")
        assertThat(script).doesNotContain("lab-zte-e2e-24")
        assertThat(script).doesNotContain("LabZteWifi24!")
        assertThat(script).contains("VSOL*|56534F4C*|12345B*|B46415*")
        assertThat(script).contains("--access-mode)")
        assertThat(script).contains("pppoe|static")
        assertThat(script).contains("E2E_ACCESS_MODE=\"\${E2E_ACCESS_MODE:-PPPOE_DYNAMIC}\"")
        assertThat(script).doesNotContain("E2E_ACCESS_MODE=\"\${E2E_ACCESS_MODE:-STATIC_IP}\"")
        assertThat(script).contains("e2e.accessMode=")
        val onuAt = script.indexOf("E2E_ONU_SN=\"\${E2E_ONU_SN:-ZTEGDC47BFFD}\"")
        val wifiCaseAt = script.indexOf("lab-vsol-e2e-24")
        assertThat(wifiCaseAt).isGreaterThan(onuAt)
    }

    @Test
    fun `staging espresso script passes first and last name to instrumentation`() {
        val script = File("../scripts/e2e_register_fiber_staging_espresso.sh").readText()
        assertThat(script).contains("E2E_FIRST_NAME=\"\${E2E_FIRST_NAME:-EeeFiber}\"")
        assertThat(script).contains("E2E_LAST_NAME=\"\${E2E_LAST_NAME:-Prueba}\"")
        assertThat(script).contains("e2e.firstName=\"\$E2E_FIRST_NAME\"")
        assertThat(script).contains("e2e.lastName=\"\$E2E_LAST_NAME\"")
        val e2eSource = File(
            "src/androidTest/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/FiberRegisterFirstOnuE2ETest.kt"
        ).readText()
        assertThat(e2eSource).contains("e2e.firstName")
        assertThat(e2eSource).contains("e2e.lastName")
        assertThat(e2eSource).contains("typeInto(RegisterSubscriptionTestTags.FIRST_NAME, firstName)")
        assertThat(e2eSource).contains("typeInto(RegisterSubscriptionTestTags.LAST_NAME, lastName)")
    }

    @Test
    fun `staging espresso script and e2e test default to the polygon fixture`() {
        val script = File("../scripts/e2e_register_fiber_staging_espresso.sh").readText()
        assertThat(script).contains("GEO_LAT=\"\${GEO_LAT:-${E2ePlaceLocationFixture.LATITUDE}}\"")
        assertThat(script).contains("GEO_LON=\"\${GEO_LON:-${E2ePlaceLocationFixture.LONGITUDE}}\"")
        assertThat(script).contains("E2E_PLACE=\"\${E2E_PLACE:-${E2ePlaceLocationFixture.PLACE_NAME}}\"")
        assertThat(script).contains("e2e_catalog_body location")
        assertThat(script).contains("e2e_catalog_body near")
        val console = File("../scripts/e2e_console.sh").readText()
        assertThat(console).contains("place/findByLocation")
        assertThat(console).contains("napbox/near")
        assertThat(script).contains("GEO fuera de todo place.area")
        val localScript = File("../scripts/e2e_register_fiber_espresso.sh").readText()
        assertThat(localScript).contains("GEO_LAT=\"\${GEO_LAT:-${E2ePlaceLocationFixture.LATITUDE}}\"")
        assertThat(localScript).contains("GEO_LON=\"\${GEO_LON:-${E2ePlaceLocationFixture.LONGITUDE}}\"")
        assertThat(localScript).doesNotContain("GEO_LAT=\"\${GEO_LAT:--11.2177}\"")
        val e2eSource = File(
            "src/androidTest/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/FiberRegisterFirstOnuE2ETest.kt"
        ).readText()
        assertThat(e2eSource).contains("E2ePlaceLocationFixture.PLACE_NAME")
        assertThat(e2eSource).contains("E2ePlaceLocationFixture.LATITUDE")
        assertThat(e2eSource).contains("E2ePlaceLocationFixture.LONGITUDE")
    }

    @Test
    fun `prod espresso script matches staging preflight and instrumentation contract`() {
        val prod = File("../scripts/e2e_register_fiber_espresso.sh").readText()
        val staging = File("../scripts/e2e_register_fiber_staging_espresso.sh").readText()
        assertThat(prod).contains("API_BASE=\"\${API_BASE:-https://api.gigafiberperu.tech/ispadmin}\"")
        assertThat(prod).doesNotContain("ispadmin-staging")
        assertThat(prod).contains("connectedProdDebugAndroidTest")
        assertThat(prod).contains("installProdDebugAndroidTest")
        assertThat(prod).contains("E2E_ONU_SN=\"\${E2E_ONU_SN:-ZTEGDC47BFFD}\"")
        assertThat(prod).contains("--first-name)")
        assertThat(prod).contains("--last-name)")
        assertThat(prod).contains("E2E_FIRST_NAME=\"\${E2E_FIRST_NAME:-EeeFiber}\"")
        assertThat(prod).contains("E2E_LAST_NAME=\"\${E2E_LAST_NAME:-Prueba}\"")
        assertThat(prod).contains("E2E_NAP_CODE=\"\${E2E_NAP_CODE:-${E2ePlaceLocationFixture.NEARBY_NAP_CODE}}\"")
        assertThat(prod).contains("e2e.firstName=\"\$E2E_FIRST_NAME\"")
        assertThat(prod).contains("e2e.lastName=\"\$E2E_LAST_NAME\"")
        assertThat(prod).contains("e2e.napCode=\"\$E2E_NAP_CODE\"")
        assertThat(prod).contains("e2e.lat=\"\$GEO_LAT\"")
        assertThat(prod).contains("e2e.lon=\"\$GEO_LON\"")
        assertThat(prod).contains("e2e_catalog_body location")
        assertThat(prod).contains("e2e_catalog_body near")
        val console = File("../scripts/e2e_console.sh").readText()
        assertThat(console).contains("place/findByLocation")
        assertThat(console).contains("napbox/near")
        assertThat(prod).contains("unconfigured_onus")
        assertThat(prod).contains("== service-health collection ==")
        assertThat(prod).contains("e2e_poll_tr069")
        assertThat(prod).contains("e2e_summary")
        assertThat(prod).contains("--env prod")
        assertThat(prod).contains("12345B*|B46415*")
        assertThat(staging).contains("unconfigured_onus")
        assertThat(staging).contains("wanted[-6:]")
        assertThat(prod).contains("wanted[-6:]")
        assertThat(normalizeEspressoEnv(prod)).isEqualTo(normalizeEspressoEnv(staging))
    }

    @Test
    fun `prod and staging espresso scripts fail and hide wifi credentials when TR069 is pending`() {
        val scripts = listOf(
            File("../scripts/e2e_register_fiber_espresso.sh"),
            File("../scripts/e2e_register_fiber_staging_espresso.sh"),
        )

        scripts.forEach { file ->
            val script = file.readText()
            val pollAt = script.indexOf("e2e_poll_tr069 \"\$API_BASE\"")
            val failGateAt = script.indexOf("if [[ \"\$TR069_EXIT\" -ne 0 ]]; then", pollAt)
            val wifiCredentialsAt = script.indexOf("echo \"== WiFi credentials (before cleanup) ==\"")

            assertThat(pollAt).isGreaterThan(-1)
            assertThat(failGateAt).isGreaterThan(pollAt)
            assertThat(wifiCredentialsAt).isGreaterThan(failGateAt)
            assertThat(script.substring(failGateAt, wifiCredentialsAt)).contains("TEST_EXIT=1")
            assertThat(script).doesNotContain("TR-069 poll exit=\$TR069_EXIT (non-fatal)")
        }
    }

    @Test
    fun `prod and staging espresso scripts let instrumentation finish after UI assertions`() {
        listOf(
            File("../scripts/e2e_register_fiber_espresso.sh"),
            File("../scripts/e2e_register_fiber_staging_espresso.sh"),
        ).forEach { file ->
            assertThat(file.readText()).contains("-Pandroid.testInstrumentationRunnerArguments.e2e.keepOpen=false")
        }
    }

    private fun normalizeEspressoEnv(raw: String): String =
        raw
            .replace("e2e_register_fiber_staging_espresso.sh", "SCRIPT")
            .replace("e2e_register_fiber_espresso.sh", "SCRIPT")
            .replace("e2e_point_device_at_kvm4 \"\$ADB\" \"\$DEVICE\"", "POINT_KVM4")
            .replace("e2e_doing \"adb reverse tcp:8080 tcp:8080\"\n\$ADB -s \"\$DEVICE\" reverse tcp:8080 tcp:8080", "POINT_KVM4")
            .replace("https://api.gigafiberperu.tech/ispadmin-staging", "API_BASE")
            .replace("https://api.gigafiberperu.tech/ispadmin", "API_BASE")
            .replace("connectedStagingDebugAndroidTest", "connectedFlavorAndroidTest")
            .replace("connectedProdDebugAndroidTest", "connectedFlavorAndroidTest")
            .replace("installStagingDebug", "installFlavor")
            .replace("installProdDebug", "installFlavor")
            .replace("--env staging", "--env ENV")
            .replace("--env prod", "--env ENV")
            .replace("env=staging", "env=ENV")
            .replace("env=prod", "env=ENV")
            .replace("login staging", "login ENV")
            .replace("login prod", "login ENV")
            .replace("ensure stagingDebug", "ensure flavorDebug")
            .replace("ensure prodDebug", "ensure flavorDebug")
            .replace("against staging", "against ENV")
            .replace("against prod", "against ENV")
            .replace("Staging catalog", "ENV catalog")
            .replace("Prod catalog", "ENV catalog")
            .replace("Staging has no", "ENV has no")
            .replace("Prod has no", "ENV has no")
            .replace("staging-e2e-registration-catalog.sql", "ENV-e2e-registration-catalog.sql")
            .replace("prod-e2e-registration-catalog.sql", "ENV-e2e-registration-catalog.sql")
            .replace("E2E_FIBER_STAGING_ESPRESSO_OK", "E2E_FIBER_ENV_ESPRESSO_OK")
            .replace("E2E_FIBER_PROD_ESPRESSO_OK", "E2E_FIBER_ENV_ESPRESSO_OK")
            .replace("E2E_FIBER_ESPRESSO_OK", "E2E_FIBER_ENV_ESPRESSO_OK")
}
