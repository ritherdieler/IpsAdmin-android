package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.subscription.subscriptionLocationError
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterSubscriptionWizardStepTest {

    private val plan = PlanResponse(
        id = "p1",
        name = "Plan",
        price = 10.0,
        downloadSpeed = "100",
        uploadSpeed = "100",
        type = InstallationType.FIBER,
    )
    private val place = Place(id = "1", name = "Lima")
    private val nap = NapBoxResponse(id = "n1", placeName = "Lima", placeId = 1)
    private val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
    private val core = NetworkDevice(id = 10, name = "Core-A", disabled = false)

    @Test
    fun `preauthorization steps precede the current form and ACS wait has no forward transition`() {
        assertEquals(RegisterSubscriptionWizardStep.ONU_CONFIRMATION, RegisterSubscriptionWizardStep.ONU_SELECTION.next())
        assertEquals(RegisterSubscriptionWizardStep.WAITING_FOR_ACS, RegisterSubscriptionWizardStep.ONU_CONFIRMATION.next())
        assertNull(RegisterSubscriptionWizardStep.WAITING_FOR_ACS.next())
        assertEquals(RegisterSubscriptionWizardStep.CLIENT_LOCATION, RegisterSubscriptionWizardStep.WAITING_FOR_ACS.afterAcsConfirmed())
        assertNull(RegisterSubscriptionWizardStep.WAITING_FOR_ACS.previous())
    }

    @Test
    fun `next and previous keep wizard order`() {
        assertEquals(
            RegisterSubscriptionWizardStep.INSTALLATION,
            RegisterSubscriptionWizardStep.CLIENT_LOCATION.next(),
        )
        assertEquals(
            RegisterSubscriptionWizardStep.CONFIRMATION,
            RegisterSubscriptionWizardStep.INSTALLATION.next(),
        )
        assertNull(RegisterSubscriptionWizardStep.CONFIRMATION.next())
        assertEquals(
            RegisterSubscriptionWizardStep.INSTALLATION,
            RegisterSubscriptionWizardStep.CONFIRMATION.previous(),
        )
        assertEquals(
            RegisterSubscriptionWizardStep.CLIENT_LOCATION,
            RegisterSubscriptionWizardStep.INSTALLATION.previous(),
        )
        assertNull(RegisterSubscriptionWizardStep.CLIENT_LOCATION.previous())
    }

    @Test
    fun `subscriptionLocationError rejects missing and zero coordinates`() {
        assertEquals("Seleccione una ubicación", subscriptionLocationError(null, null))
        assertEquals("Seleccione una ubicación", subscriptionLocationError(0.0, 0.0))
        assertNull(subscriptionLocationError(-12.046374, -77.042793))
    }

    @Test
    fun `step 1 cannot advance without valid client address place and location`() {
        val empty = RegisterSubscriptionFormState()
        assertFalse(
            canAdvanceWizardStep(RegisterSubscriptionWizardStep.CLIENT_LOCATION, empty),
        )

        val zeroLocation = validClientAndAddress().copy(location = LatLng(0.0, 0.0))
        assertFalse(
            canAdvanceWizardStep(RegisterSubscriptionWizardStep.CLIENT_LOCATION, zeroLocation),
        )
    }

    @Test
    fun `step 1 can advance when client address place and location are valid`() {
        assertTrue(
            canAdvanceWizardStep(
                RegisterSubscriptionWizardStep.CLIENT_LOCATION,
                validClientAndAddress(),
            ),
        )
    }

    @Test
    fun `step 2 fiber requires plan nap onu and wifi`() {
        val step1Done = validClientAndAddress()
        assertFalse(
            canAdvanceWizardStep(RegisterSubscriptionWizardStep.INSTALLATION, step1Done),
        )

        val fiberReady = step1Done.copy(
            planList = listOf(plan),
            selectedPlan = plan,
            napBoxList = listOf(nap),
            selectedNapBox = nap,
            onuList = listOf(onu),
            selectedOnu = onu,
            coreDeviceList = listOf(core),
            selectedHostDevice = core,
            wifiSsid24 = "CasaFibra",
            wifiPassword24 = "clave24xx",
            installationType = InstallationType.FIBER,
        )
        assertTrue(
            canAdvanceWizardStep(RegisterSubscriptionWizardStep.INSTALLATION, fiberReady),
        )
    }

    @Test
    fun `step 2 wireless does not require nap or onu`() {
        val wirelessPlan = plan.copy(id = "w1", type = InstallationType.WIRELESS)
        val wireless = validClientAndAddress().copy(
            installationType = InstallationType.WIRELESS,
            planList = listOf(wirelessPlan),
            selectedPlan = wirelessPlan,
            coreDeviceList = listOf(core),
            selectedHostDevice = core,
        )
        assertTrue(
            canAdvanceWizardStep(RegisterSubscriptionWizardStep.INSTALLATION, wireless),
        )
        assertFalse(
            wizardFieldsFor(RegisterSubscriptionWizardStep.INSTALLATION, wireless)
                .contains(FormFieldKey.NAP_BOX),
        )
        assertFalse(
            wizardFieldsFor(RegisterSubscriptionWizardStep.INSTALLATION, wireless)
                .contains(FormFieldKey.ONU),
        )
    }

    @Test
    fun `step 2 offline requires client ip`() {
        val fiber = validClientAndAddress().copy(
            planList = listOf(plan),
            selectedPlan = plan,
            napBoxList = listOf(nap),
            selectedNapBox = nap,
            onuList = listOf(onu),
            selectedOnu = onu,
            coreDeviceList = listOf(core),
            selectedHostDevice = core,
            wifiSsid24 = "CasaFibra",
            wifiPassword24 = "clave24xx",
            installationType = InstallationType.FIBER,
            requiresClientIpAddress = true,
        )
        assertFalse(
            canAdvanceWizardStep(RegisterSubscriptionWizardStep.INSTALLATION, fiber),
        )
        assertTrue(
            canAdvanceWizardStep(
                RegisterSubscriptionWizardStep.INSTALLATION,
                fiber.copy(clientIpAddress = "192.168.1.10"),
            ),
        )
    }

    private fun validClientAndAddress() = RegisterSubscriptionFormState(
        firstName = "Juan",
        lastName = "Perez",
        dni = "12345678",
        phone = "987654321",
        address = "Calle larga 12345",
        selectedPlace = place,
        placeList = listOf(place),
        location = LatLng(-12.046374, -77.042793),
    )
}
