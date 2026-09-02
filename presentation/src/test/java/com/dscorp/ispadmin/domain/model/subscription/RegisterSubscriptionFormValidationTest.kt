package com.dscorp.ispadmin.domain.model.subscription

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.FormFieldKey
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.TvCpeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterSubscriptionFormValidationTest {

    private val activeCore = NetworkDevice(id = 1, name = "Core-1", disabled = false)
    private val disabledCore = NetworkDevice(id = 2, name = "Core-2", disabled = true)
    private val secondActiveCore = NetworkDevice(id = 3, name = "Core-3", disabled = false)

    @Test
    fun `validate equipment condition always null`() {
        assertNull(RegisterSubscriptionFormState().validate(FormFieldKey.EQUIPMENT_CONDITION))
    }

    @Test
    fun `subscriptionHostDeviceError null when selected among active cores`() {
        assertNull(
            subscriptionHostDeviceError(
                selectedHostDevice = activeCore,
                coreDeviceList = listOf(activeCore, disabledCore)
            )
        )
    }

    @Test
    fun `subscriptionHostDeviceError when null and active cores exist`() {
        assertEquals(
            "Seleccione un dispositivo host",
            subscriptionHostDeviceError(
                selectedHostDevice = null,
                coreDeviceList = listOf(activeCore, secondActiveCore)
            )
        )
    }

    @Test
    fun `subscriptionHostDeviceError null when no active cores`() {
        assertNull(
            subscriptionHostDeviceError(
                selectedHostDevice = null,
                coreDeviceList = listOf(disabledCore)
            )
        )
    }

    @Test
    fun `activeCoreDevices filters disabled`() {
        val form = RegisterSubscriptionFormState(
            coreDeviceList = listOf(activeCore, disabledCore, secondActiveCore)
        )
        assertEquals(listOf(activeCore, secondActiveCore), form.activeCoreDevices())
    }

    @Test
    fun `shouldShowHostDeviceSelector true when more than one active core`() {
        val form = RegisterSubscriptionFormState(
            coreDeviceList = listOf(activeCore, disabledCore, secondActiveCore)
        )
        assertTrue(form.shouldShowHostDeviceSelector())
    }

    @Test
    fun `shouldShowHostDeviceSelector false when one active core`() {
        val form = RegisterSubscriptionFormState(
            coreDeviceList = listOf(activeCore, disabledCore)
        )
        assertFalse(form.shouldShowHostDeviceSelector())
    }

    @Test
    fun `validate HOST_DEVICE sets hostDeviceError via validated`() {
        val form = RegisterSubscriptionFormState(
            coreDeviceList = listOf(activeCore, secondActiveCore),
            selectedHostDevice = null
        ).validated(FormFieldKey.HOST_DEVICE)
        assertNotNull(form.hostDeviceError)
    }

    @Test
    fun `subscriptionFirstNameError blank`() {
        assertEquals("Ingrese el nombre", subscriptionFirstNameError(""))
    }

    @Test
    fun `subscriptionFirstNameError invalid format`() {
        assertNotNull(subscriptionFirstNameError("Juan1"))
    }

    @Test
    fun `subscriptionFirstNameError too long`() {
        val long = "a".repeat(RegisterSubscriptionFormConstraints.MAX_PERSON_NAME_LENGTH + 1)
        assertNotNull(subscriptionFirstNameError(long))
    }

    @Test
    fun `subscriptionFirstNameError valid`() {
        assertNull(subscriptionFirstNameError("Juan"))
    }

    @Test
    fun `subscriptionDniError requires eight digits`() {
        assertNotNull(subscriptionDniError("1234567"))
        assertNull(subscriptionDniError("12345678"))
    }

    @Test
    fun `subscriptionAddressError min length`() {
        assertNotNull(subscriptionAddressError("1234"))
        assertNull(subscriptionAddressError("12345"))
    }

    @Test
    fun `subscriptionPhoneError nine digits`() {
        assertNotNull(subscriptionPhoneError("12345678"))
        assertNull(subscriptionPhoneError("123456789"))
    }

    @Test
    fun `subscriptionNoteError over max`() {
        val long = "x".repeat(RegisterSubscriptionFormConstraints.MAX_NOTE_LENGTH + 1)
        assertNotNull(subscriptionNoteError(long))
        assertNull(subscriptionNoteError("ok"))
    }

    @Test
    fun `subscriptionPlanError invalid selection`() {
        val plan = PlanResponse(
            id = "p1",
            name = "P",
            price = 1.0,
            downloadSpeed = "1",
            uploadSpeed = "1",
            type = InstallationType.FIBER
        )
        assertNotNull(subscriptionPlanError(plan, emptyList()))
        assertNull(subscriptionPlanError(plan, listOf(plan)))
    }

    @Test
    fun `subscriptionOnuError when fiber requires onu`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        assertNotNull(subscriptionOnuError(true, null, listOf(onu)))
        assertNull(subscriptionOnuError(true, onu, listOf(onu)))
        assertNull(subscriptionOnuError(false, null, emptyList()))
    }

    @Test
    fun `subscriptionNapBoxError when fiber requires nap`() {
        val nap = NapBoxResponse(id = "n1", placeName = "P", placeId = 1)
        assertNotNull(subscriptionNapBoxError(true, null, listOf(nap)))
        assertNull(subscriptionNapBoxError(true, nap, listOf(nap)))
    }

    @Test
    fun `subscriptionNapBoxError accepts nap matched by code when id differs`() {
        val listed = NapBoxResponse(id = "104", code = "NO-001", placeName = "P", placeId = 1)
        val selected = NapBoxResponse(id = null, code = "NO-001", placeName = "P", placeId = 1)
        assertNull(subscriptionNapBoxError(true, selected, listOf(listed)))
    }

    @Test
    fun `subscriptionOnuErrorAfterListRefresh stale selection`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        assertEquals(
            "La ONU seleccionada ya no está disponible",
            subscriptionOnuErrorAfterListRefresh(
                requiresOnu = true,
                previousSelected = onu,
                newSelected = null,
                newList = emptyList(),
                previousFieldError = null
            )
        )
    }

    @Test
    fun `subscriptionOnuErrorAfterListRefresh revalidates when previous error`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        assertNotNull(
            subscriptionOnuErrorAfterListRefresh(
                requiresOnu = true,
                previousSelected = null,
                newSelected = null,
                newList = listOf(onu),
                previousFieldError = "x"
            )
        )
    }

    @Test
    fun `subscriptionOnuErrorAfterListRefresh clears when no stale and no previous error`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        assertNull(
            subscriptionOnuErrorAfterListRefresh(
                requiresOnu = true,
                previousSelected = null,
                newSelected = null,
                newList = listOf(onu),
                previousFieldError = null
            )
        )
    }

    @Test
    fun `napBoxToPreselectAfterNearbyRefresh takes first ordered nap of the place`() {
        val nearer = NapBoxResponse(id = "near", placeName = "P", placeId = 1)
        val farther = NapBoxResponse(id = "far", placeName = "P", placeId = 1)
        val otherPlace = NapBoxResponse(id = "other", placeName = "Q", placeId = 2)

        val selected = napBoxToPreselectAfterNearbyRefresh(
            nearbyOrdered = listOf(nearer, farther, otherPlace),
            placeId = 1,
        )

        assertEquals(nearer, selected)
    }

    @Test
    fun `napBoxToPreselectAfterNearbyRefresh uses first nearby when place is unknown`() {
        val first = NapBoxResponse(id = "a", placeName = "A", placeId = 1)
        val second = NapBoxResponse(id = "b", placeName = "B", placeId = 2)

        val selected = napBoxToPreselectAfterNearbyRefresh(
            nearbyOrdered = listOf(first, second),
            placeId = null,
        )

        assertEquals(first, selected)
    }

    @Test
    fun `napBoxToPreselectAfterNearbyRefresh returns null when list is empty`() {
        assertNull(
            napBoxToPreselectAfterNearbyRefresh(
                nearbyOrdered = emptyList(),
                placeId = 1,
            )
        )
    }

    @Test
    fun `subscriptionNapBoxErrorAfterNearbyRefresh stale selection`() {
        val nap = NapBoxResponse(id = "n1", placeName = "P", placeId = 1)
        assertEquals(
            "La caja NAP seleccionada ya no está disponible",
            subscriptionNapBoxErrorAfterNearbyRefresh(
                requiresNapBox = true,
                previousSelected = nap,
                newSelected = null,
                newList = emptyList(),
                previousFieldError = null
            )
        )
    }

    @Test
    fun `subscriptionNapBoxErrorAfterNearbyRefresh revalidates when previous error`() {
        val nap = NapBoxResponse(id = "n1", placeName = "P", placeId = 1)
        assertNotNull(
            subscriptionNapBoxErrorAfterNearbyRefresh(
                requiresNapBox = true,
                previousSelected = null,
                newSelected = null,
                newList = listOf(nap),
                previousFieldError = "x"
            )
        )
    }

    @Test
    fun `subscriptionClientIpAddressError required when empty`() {
        assertEquals(
            "Ingrese la IP del cliente",
            subscriptionClientIpAddressError("", required = true)
        )
    }

    @Test
    fun `subscriptionClientIpAddressError null when not required and empty`() {
        assertNull(subscriptionClientIpAddressError("  ", required = false))
    }

    @Test
    fun `subscriptionClientIpAddressError invalid ipv4`() {
        assertEquals(
            "La IP del cliente no es válida",
            subscriptionClientIpAddressError("999.1.1.1", required = true)
        )
    }

    @Test
    fun `subscriptionClientIpAddressError accepts valid ipv4`() {
        assertNull(subscriptionClientIpAddressError("192.168.1.50", required = true))
    }

    @Test
    fun `blockingForSubmit excludes equipment and client ip`() {
        assertTrue(FormFieldKey.blockingForSubmit.contains(FormFieldKey.NOTE))
        assertTrue(FormFieldKey.blockingForSubmit.contains(FormFieldKey.HOST_DEVICE))
        assertFalse(FormFieldKey.blockingForSubmit.contains(FormFieldKey.EQUIPMENT_CONDITION))
        assertFalse(FormFieldKey.blockingForSubmit.contains(FormFieldKey.CLIENT_IP_ADDRESS))
    }

    @Test
    fun `RegisterSubscriptionFormState wireless blocking fields pass without onu nap or facade photo uri`() {
        val plan = PlanResponse(
            id = "w1",
            name = "Wireless",
            price = 1.0,
            downloadSpeed = "1",
            uploadSpeed = "1",
            type = InstallationType.WIRELESS
        )
        val form = RegisterSubscriptionFormState(
            firstName = "Juan",
            lastName = "Perez",
            dni = "12345678",
            address = "Calle larga 12345",
            phone = "987654321",
            planList = listOf(plan),
            selectedPlan = plan,
            selectedPlace = Place(id = "1", name = "L"),
            installationType = InstallationType.WIRELESS,
        )
        listOf(
            FormFieldKey.FIRST_NAME,
            FormFieldKey.LAST_NAME,
            FormFieldKey.DNI,
            FormFieldKey.ADDRESS,
            FormFieldKey.PHONE,
            FormFieldKey.PLAN,
            FormFieldKey.PLACE,
            FormFieldKey.ONU,
            FormFieldKey.NAP_BOX,
            FormFieldKey.NOTE,
        ).forEach { field ->
            assertNull(form.validate(field))
        }
        assertNotNull(form.validate(FormFieldKey.FACADE_PHOTO))
    }

    @Test
    fun `RegisterSubscriptionFormState isValid false without facade photo`() {
        val plan = PlanResponse(
            id = "w1",
            name = "Wireless",
            price = 1.0,
            downloadSpeed = "1",
            uploadSpeed = "1",
            type = InstallationType.WIRELESS
        )
        val form = RegisterSubscriptionFormState(
            firstName = "Juan",
            lastName = "Perez",
            dni = "12345678",
            address = "Calle larga 12345",
            phone = "987654321",
            planList = listOf(plan),
            selectedPlan = plan,
            selectedPlace = Place(id = "1", name = "L"),
            installationType = InstallationType.WIRELESS,
        )
        assertFalse(form.isValid())
    }

    @Test
    fun `RegisterSubscriptionFormState isValid false when note too long`() {
        val longNote = "x".repeat(RegisterSubscriptionFormConstraints.MAX_NOTE_LENGTH + 1)
        val form = RegisterSubscriptionFormState(note = longNote)
        assertFalse(form.isValid())
    }

    @Test
    fun `validated sets noteError`() {
        val longNote = "y".repeat(RegisterSubscriptionFormConstraints.MAX_NOTE_LENGTH + 1)
        val form = RegisterSubscriptionFormState(note = longNote).validated(FormFieldKey.NOTE)
        assertNotNull(form.noteError)
    }

    @Test
    fun `validate client ip required when requiresClientIpAddress`() {
        val form = RegisterSubscriptionFormState(requiresClientIpAddress = true)
        assertEquals("Ingrese la IP del cliente", form.validate(FormFieldKey.CLIENT_IP_ADDRESS))
    }

    @Test
    fun `validate client ip null when not required`() {
        val form = RegisterSubscriptionFormState(requiresClientIpAddress = false)
        assertNull(form.validate(FormFieldKey.CLIENT_IP_ADDRESS))
    }

    @Test
    fun `requiresWifiConfig true only for FIBER`() {
        assertTrue(
            RegisterSubscriptionFormState(installationType = InstallationType.FIBER)
                .requiresWifiConfig()
        )
        assertFalse(
            RegisterSubscriptionFormState(installationType = InstallationType.WIRELESS)
                .requiresWifiConfig()
        )
        assertFalse(
            RegisterSubscriptionFormState(installationType = InstallationType.ONLY_TV_FIBER)
                .requiresWifiConfig()
        )
    }

    @Test
    fun `ONLY_TV requires explicit CPE kind before ONU`() {
        val unset = RegisterSubscriptionFormState(installationType = InstallationType.ONLY_TV_FIBER)
        assertTrue(unset.requiresTvCpeKind())
        assertFalse(unset.requiresOnu())
        assertEquals(
            "Seleccione ONU o receptor óptico CATV",
            unset.validate(FormFieldKey.TV_CPE_KIND)
        )
        assertTrue(unset.blockingFields().contains(FormFieldKey.TV_CPE_KIND))
        assertFalse(FormFieldKey.blockingForSubmit.contains(FormFieldKey.TV_CPE_KIND))
    }

    @Test
    fun `ONLY_TV ONU requires onu and CATV does not`() {
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        val onuPath = RegisterSubscriptionFormState(
            installationType = InstallationType.ONLY_TV_FIBER,
            tvCpeKind = TvCpeKind.ONU,
            onuList = listOf(onu),
        )
        assertTrue(onuPath.requiresOnu())
        assertNotNull(onuPath.validate(FormFieldKey.ONU))
        assertNull(onuPath.validate(FormFieldKey.TV_CPE_KIND))

        val catvPath = onuPath.copy(tvCpeKind = TvCpeKind.OPTICAL_RECEIVER, selectedOnu = null)
        assertFalse(catvPath.requiresOnu())
        assertNull(catvPath.validate(FormFieldKey.ONU))
        assertNull(catvPath.validate(FormFieldKey.TV_CPE_KIND))
    }

    @Test
    fun `subscriptionWifiSsidError null when wifi not required`() {
        assertNull(subscriptionWifiSsidError("", requiresWifi = false))
    }

    @Test
    fun `subscriptionWifiSsidError blank when required`() {
        assertEquals("Ingrese el SSID WiFi", subscriptionWifiSsidError("", requiresWifi = true))
        assertEquals("Ingrese el SSID WiFi", subscriptionWifiSsidError("   ", requiresWifi = true))
    }

    @Test
    fun `subscriptionWifiSsidError length between 1 and 32`() {
        assertNull(subscriptionWifiSsidError("A", requiresWifi = true))
        assertNull(subscriptionWifiSsidError("a".repeat(32), requiresWifi = true))
        assertEquals(
            "El SSID debe tener entre 1 y 32 caracteres",
            subscriptionWifiSsidError("a".repeat(33), requiresWifi = true)
        )
    }

    @Test
    fun `subscriptionWifiPasswordError null when wifi not required`() {
        assertNull(subscriptionWifiPasswordError("", requiresWifi = false))
    }

    @Test
    fun `subscriptionWifiPasswordError blank when required`() {
        assertEquals(
            "Ingrese la clave WiFi",
            subscriptionWifiPasswordError("", requiresWifi = true)
        )
    }

    @Test
    fun `subscriptionWifiPasswordError length between 8 and 63`() {
        assertEquals(
            "La clave WiFi debe tener entre 8 y 63 caracteres",
            subscriptionWifiPasswordError("1234567", requiresWifi = true)
        )
        assertNull(subscriptionWifiPasswordError("12345678", requiresWifi = true))
        assertNull(subscriptionWifiPasswordError("a".repeat(63), requiresWifi = true))
        assertEquals(
            "La clave WiFi debe tener entre 8 y 63 caracteres",
            subscriptionWifiPasswordError("a".repeat(64), requiresWifi = true)
        )
    }

    @Test
    fun `validate wifi fields null for wireless`() {
        val form = RegisterSubscriptionFormState(installationType = InstallationType.WIRELESS)
        assertNull(form.validate(FormFieldKey.WIFI_SSID_24))
        assertNull(form.validate(FormFieldKey.WIFI_PASSWORD_24))
        assertNull(form.validate(FormFieldKey.WIFI_SSID_5))
        assertNull(form.validate(FormFieldKey.WIFI_PASSWORD_5))
    }

    @Test
    fun `validate wifi fields required for FIBER`() {
        val form = RegisterSubscriptionFormState(installationType = InstallationType.FIBER)
        assertNotNull(form.validate(FormFieldKey.WIFI_SSID_24))
        assertNotNull(form.validate(FormFieldKey.WIFI_PASSWORD_24))
        assertNull(form.validate(FormFieldKey.WIFI_SSID_5))
        assertNull(form.validate(FormFieldKey.WIFI_PASSWORD_5))
    }

    @Test
    fun `blockingForSubmit includes shared wifi fields not split ssid or password 5`() {
        assertTrue(FormFieldKey.blockingForSubmit.contains(FormFieldKey.WIFI_SSID_24))
        assertTrue(FormFieldKey.blockingForSubmit.contains(FormFieldKey.WIFI_PASSWORD_24))
        assertFalse(FormFieldKey.blockingForSubmit.contains(FormFieldKey.WIFI_SSID_5))
        assertFalse(FormFieldKey.blockingForSubmit.contains(FormFieldKey.WIFI_PASSWORD_5))
    }

    @Test
    fun `derivedWifiSsid5 appends suffix to trimmed name`() {
        assertEquals("Casa - 5G", derivedWifiSsid5("Casa"))
        assertEquals("Casa - 5G", derivedWifiSsid5("  Casa  "))
    }

    @Test
    fun `unified wifi ssid error when derived 5ghz exceeds 32 chars`() {
        val name27 = "a".repeat(27)
        val name28 = "a".repeat(28)
        val formOk = RegisterSubscriptionFormState(
            installationType = InstallationType.FIBER,
            wifiSsid24 = name27
        )
        val formTooLong = RegisterSubscriptionFormState(
            installationType = InstallationType.FIBER,
            wifiSsid24 = name28
        )
        assertNull(formOk.validate(FormFieldKey.WIFI_SSID_24))
        assertEquals(
            "El nombre de red no puede superar 27 caracteres (el SSID 5 GHz añade « - 5G»)",
            formTooLong.validate(FormFieldKey.WIFI_SSID_24)
        )
    }

    @Test
    fun `split wifi names require both ssids and ignore password 5`() {
        val emptySplit = RegisterSubscriptionFormState(
            installationType = InstallationType.FIBER,
            useDifferentWifiNames = true
        )
        assertNotNull(emptySplit.validate(FormFieldKey.WIFI_SSID_24))
        assertNotNull(emptySplit.validate(FormFieldKey.WIFI_SSID_5))
        assertNull(emptySplit.validate(FormFieldKey.WIFI_PASSWORD_5))

        val filledSplit = emptySplit.copy(
            wifiSsid24 = "Casa24",
            wifiSsid5 = "Casa5"
        )
        assertNull(filledSplit.validate(FormFieldKey.WIFI_SSID_24))
        assertNull(filledSplit.validate(FormFieldKey.WIFI_SSID_5))
        assertTrue(filledSplit.blockingFields().contains(FormFieldKey.WIFI_SSID_5))
        assertFalse(
            RegisterSubscriptionFormState(installationType = InstallationType.FIBER)
                .blockingFields()
                .contains(FormFieldKey.WIFI_SSID_5)
        )
    }

    @Test
    fun `split wifi ssid 24 allows 32 chars because suffix is not applied`() {
        val form = RegisterSubscriptionFormState(
            installationType = InstallationType.FIBER,
            useDifferentWifiNames = true,
            wifiSsid24 = "a".repeat(32)
        )
        assertNull(form.validate(FormFieldKey.WIFI_SSID_24))
    }

    @Test
    fun `resolvedWifiSsid5 uses suffix unless different names are enabled`() {
        val unified = RegisterSubscriptionFormState(wifiSsid24 = "Casa", wifiSsid5 = "Otro")
        val split = unified.copy(useDifferentWifiNames = true, wifiSsid5 = "Casa5")
        assertEquals("Casa - 5G", unified.resolvedWifiSsid5())
        assertEquals("Casa5", split.resolvedWifiSsid5())
    }
}
