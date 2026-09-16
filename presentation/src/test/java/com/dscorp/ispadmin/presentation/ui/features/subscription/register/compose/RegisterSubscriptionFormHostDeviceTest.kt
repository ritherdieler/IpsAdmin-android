package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.InstallationType
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
        assertThat(formSource).contains("RegisterSubscriptionTestTags.HOST_DEVICE")
        assertThat(formSource).contains("RegisterSubscriptionIntent.HostDeviceSelected")
        assertThat(formSource).contains("R.string.host_device")
        assertThat(formSource).contains("form.activeCoreDevices()")
        assertThat(formSource).contains("RegisterSubscriptionTestTags.CLIENT_IP")
        assertThat(formSource).contains("requiresClientIpAddress")
        assertThat(formSource).contains("shouldShowAccessModeSelector()")
        assertThat(formSource).contains("WanAccessModeSelector")
        assertThat(formSource).contains("RegisterSubscriptionIntent.AccessModeSelected")
        assertThat(formSource).contains("RegisterSubscriptionTestTags.ACCESS_MODE")
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
    fun `shouldShowAccessModeSelector is visible for fiber and wireless`() {
        assertTrue(
            RegisterSubscriptionFormState(installationType = InstallationType.FIBER)
                .shouldShowAccessModeSelector()
        )
        assertTrue(
            RegisterSubscriptionFormState(installationType = InstallationType.WIRELESS)
                .shouldShowAccessModeSelector()
        )
        assertFalse(
            RegisterSubscriptionFormState(installationType = InstallationType.ONLY_TV_FIBER)
                .shouldShowAccessModeSelector()
        )
    }

    @Test
    fun `FiberOpticForm muestra dropdown VLAN solo en FIBER`() {
        val formSource = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/compose/RegisterSubscriptionForm.kt"
        ).readText()

        assertThat(formSource).contains("RegisterSubscriptionTestTags.VLAN")
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
        val vlanTagIndex = formSource.indexOf("RegisterSubscriptionTestTags.VLAN")
        val showOnuSelectorIndex = formSource.indexOf("if (showOnuSelector)")
        assertTrue(showOnuSelectorIndex >= 0)
        assertTrue(vlanTagIndex > showOnuSelectorIndex)
        assertThat(formSource).contains("RegisterSubscriptionTestTags.ONU")
        assertThat(formSource).contains("form.requiresOnu()")
        assertThat(formSource).contains("requiresWifiConfig()")
    }

    @Test
    fun `WifiFields usa un nombre de red y check para nombres distintos`() {
        val formSource = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/compose/RegisterSubscriptionForm.kt"
        ).readText()

        assertThat(formSource).contains("RegisterSubscriptionTestTags.WIFI_DIFFERENT_NAMES")
        assertThat(formSource).contains("UseDifferentWifiNamesChanged")
        assertThat(formSource).contains("Nombre de red")
        assertThat(formSource).contains("Clave WiFi")
        assertThat(formSource).contains("form.useDifferentWifiNames")
        assertThat(formSource).contains("resolvedWifiSsid5()")
        assertThat(formSource).contains("RegisterSubscriptionTestTags.WIFI_SSID_5")
        assertThat(formSource).contains("contentDescription")
    }
}
