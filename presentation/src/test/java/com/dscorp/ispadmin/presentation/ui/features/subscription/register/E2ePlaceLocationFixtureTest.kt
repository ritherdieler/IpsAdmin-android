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
    }

    @Test
    fun `staging espresso script defaults wifi by lab onu brand`() {
        val script = File("../scripts/e2e_register_fiber_staging_espresso.sh").readText()
        assertThat(script).contains("E2E_ONU_SN=\"\${E2E_ONU_SN:-ZTEGDC47BFFD}\"")
        assertThat(script).contains("lab-vsol-e2e-24")
        assertThat(script).contains("LabVsolWifi24!")
        assertThat(script).contains("lab-zte-e2e-24")
        assertThat(script).contains("LabZteWifi24!")
        assertThat(script).contains("VSOL*")
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
        assertThat(script).contains("place/findByLocation")
        assertThat(script).contains("napbox/near")
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
}
