package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RegisterSubscriptionFormHostDeviceTest {

    @Test
    fun `InstallationBlock muestra dropdown host cuando hay mas de un core activo`() {
        val formSource = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/compose/RegisterSubscriptionForm.kt"
        ).readText()

        assertThat(formSource).contains("form.shouldShowHostDeviceSelector()")
        assertThat(formSource).contains("testTag(\"register_host_device_dropdown\")")
        assertThat(formSource).contains("RegisterSubscriptionIntent.HostDeviceSelected")
        assertThat(formSource).contains("R.string.host_device")
        assertThat(formSource).contains("form.activeCoreDevices()")
        assertThat(formSource).contains("testTag(\"tf_client_ip_address\")")
        assertThat(formSource).contains("requiresClientIpAddress")
    }

    @Test
    fun `shouldShowHostDeviceSelector refleja visibilidad del dropdown`() {
        val oneCore = RegisterSubscriptionFormState(
            coreDeviceList = listOf(NetworkDevice(id = 1, name = "A", disabled = false))
        )
        val twoCores = RegisterSubscriptionFormState(
            coreDeviceList = listOf(
                NetworkDevice(id = 1, name = "A", disabled = false),
                NetworkDevice(id = 2, name = "B", disabled = false)
            )
        )

        assertFalse(oneCore.shouldShowHostDeviceSelector())
        assertTrue(twoCores.shouldShowHostDeviceSelector())
    }

    @Test
    fun `FiberOpticForm muestra dropdown VLAN solo en FIBER`() {
        val formSource = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/compose/RegisterSubscriptionForm.kt"
        ).readText()

        assertThat(formSource).contains("testTag(\"register_vlan_dropdown\")")
        assertThat(formSource).contains("RegisterSubscriptionIntent.OnVlanChanged")
        assertThat(formSource).contains("VLAN_OPTIONS")
        assertThat(formSource).contains("isItemEnabled = { it.selectable }")
        val vlanOptionsSource = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/models/VlanOption.kt"
        ).readText()
        assertThat(vlanOptionsSource).contains("VLAN 1")
        assertThat(vlanOptionsSource).contains("VLAN 100")
        assertThat(vlanOptionsSource).contains("selectable = false")
        assertThat(vlanOptionsSource).contains("DEFAULT_REGISTRATION_VLAN = \"100\"")
        val vlanTagIndex = formSource.indexOf("testTag(\"register_vlan_dropdown\")")
        val showOnuSelectorIndex = formSource.indexOf("if (showOnuSelector)")
        assertTrue(showOnuSelectorIndex >= 0)
        assertTrue(vlanTagIndex > showOnuSelectorIndex)
        assertThat(formSource).contains("testTag(\"register_onu_dropdown\")")
        assertThat(formSource).contains("form.requiresOnu()")
        assertThat(formSource).contains("requiresWifiConfig()")
        assertThat(formSource).contains("testTag(\"register_tv_cpe_onu\")")
        assertThat(formSource).contains("testTag(\"register_tv_cpe_optical_receiver\")")
        assertThat(formSource).contains("RegisterSubscriptionIntent.TvCpeKindSelected")
    }

    @Test
    fun `WifiFields usa un nombre de red y check para nombres distintos`() {
        val formSource = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/compose/RegisterSubscriptionForm.kt"
        ).readText()

        assertThat(formSource).contains("testTag(\"cb_wifi_different_names\")")
        assertThat(formSource).contains("UseDifferentWifiNamesChanged")
        assertThat(formSource).contains("Nombre de red")
        assertThat(formSource).contains("Clave WiFi")
        assertThat(formSource).contains("form.useDifferentWifiNames")
        assertThat(formSource).contains("resolvedWifiSsid5()")
        assertThat(formSource).contains("tf_wifi_ssid_5")
        assertThat(formSource).contains("contentDescription")
    }
}
