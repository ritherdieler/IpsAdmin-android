package com.dscorp.ispadmin.domain.model.subscription

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.FormFieldKey
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterSubscriptionFormValidationTest {

    @Test
    fun `validate equipment condition always null`() {
        assertNull(RegisterSubscriptionFormState().validate(FormFieldKey.EQUIPMENT_CONDITION))
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
    fun `blockingForSubmit excludes equipment only`() {
        assertTrue(FormFieldKey.blockingForSubmit.contains(FormFieldKey.NOTE))
        assertFalse(FormFieldKey.blockingForSubmit.contains(FormFieldKey.EQUIPMENT_CONDITION))
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
}
