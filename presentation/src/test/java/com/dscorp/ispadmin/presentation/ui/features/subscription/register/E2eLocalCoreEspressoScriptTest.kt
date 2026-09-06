package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class E2eLocalCoreEspressoScriptTest {

    private val script = File("../scripts/e2e_register_fiber_local_espresso.sh")

    @Test
    fun `local core espresso script talks to Core via emulator reverse not staging or prod`() {
        assertThat(script.exists()).isTrue()
        val text = script.readText()
        assertThat(text).contains("connectedDevDebugAndroidTest")
        assertThat(text).contains("com.dscorp.ispadmin.dev")
        assertThat(text).contains("tcp:8080 tcp:8082")
        assertThat(text).contains("local-e2e-ensure-catalog.sh")
        val ensureAt = text.indexOf("local-e2e-ensure-catalog.sh")
        val loginAt = text.indexOf("== login Core ==")
        assertThat(ensureAt).isGreaterThan(-1)
        assertThat(loginAt).isGreaterThan(ensureAt)
        assertThat(text).contains("E2E_USER=\"\${E2E_USER:-dscorp}\"")
        assertThat(text).contains("E2E_PASSWORD=\"\${E2E_PASSWORD:-nohacker}\"")
        assertThat(text).contains("E2E_ONU_SN=\"\${E2E_ONU_SN:-ZTEGDC47BFFD}\"")
        assertThat(text).contains("E2E_PLACE=\"\${E2E_PLACE:-${E2ePlaceLocationFixture.PLACE_NAME}}\"")
        assertThat(text).contains("E2E_NAP_CODE=\"\${E2E_NAP_CODE:-${E2ePlaceLocationFixture.NEARBY_NAP_CODE}}\"")
        assertThat(text).contains("GEO_LAT=\"\${GEO_LAT:-${E2ePlaceLocationFixture.LATITUDE}}\"")
        assertThat(text).contains("GEO_LON=\"\${GEO_LON:-${E2ePlaceLocationFixture.LONGITUDE}}\"")
        val prod = File("../scripts/e2e_register_fiber_espresso.sh").readText()
        assertThat(text).contains("E2E_WIFI_SSID=\"\${E2E_WIFI_SSID:-mimiwifi}\"")
        assertThat(prod).contains("E2E_USER=\"\${E2E_USER:-dscorp}\"")
        assertThat(prod).contains("E2E_PASSWORD=\"\${E2E_PASSWORD:-nohacker}\"")
        assertThat(prod).contains("E2E_WIFI_SSID=\"\${E2E_WIFI_SSID:-mimiwifi}\"")
        assertThat(prod).contains("E2E_WIFI_PASS=\"\${E2E_WIFI_PASS:-MimiWifi24pass}\"")
        assertThat(text).doesNotContain("connectedProdDebugAndroidTest")
        assertThat(text).doesNotContain("connectedStagingDebugAndroidTest")
        assertThat(text).doesNotContain("--env staging")
        assertThat(text).doesNotContain("api.gigafiberperu.cloud")
    }

    @Test
    fun `local core espresso script prints wifi before cleanup and uses long passphrase`() {
        val text = script.readText()
        val wifiBlock = text.indexOf("== WiFi credentials (before ping/cleanup) ==")
        val cleanupBlock = text.indexOf("== post cleanup")
        assertThat(wifiBlock).isGreaterThan(-1)
        assertThat(cleanupBlock).isGreaterThan(wifiBlock)
        assertThat(text).contains("wifi_24 ssid=\$E2E_WIFI_SSID password=\$E2E_WIFI_PASS")
        assertThat(text).contains("E2E_WIFI_PASS=\"\${E2E_WIFI_PASS:-MimiWifi24pass}\"")
        assertThat("MimiWifi24pass".length).isAtLeast(8)
        assertThat(text).contains("SKIP_POST_CLEANUP")
        assertThat(text).contains("post cleanup skipped (SKIP_POST_CLEANUP=1)")
    }

    @Test
    fun `dev flavor BASE_URL is loopback so adb reverse reaches local Core`() {
        val gradle = File("build.gradle").readText()
        val devBlock = gradle.substringAfter("dev {").substringBefore("prod {")
        assertThat(devBlock).contains("http://127.0.0.1:8080/ispadmin/")
        assertThat(devBlock).doesNotContain("192.168.1.4")
    }
}
