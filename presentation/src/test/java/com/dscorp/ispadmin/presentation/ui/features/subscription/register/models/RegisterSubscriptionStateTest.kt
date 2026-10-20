package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.registrationProgressPhases
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.SERVICE_TYPE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterSubscriptionStateTest {

    private fun state(
        type: InstallationType,
        chosen: Boolean = true,
        operation: OnuRegistrationOperation? = null,
    ) = RegisterSubscriptionState(
        preauthorizationEnabled = true,
        preauthorizationOperation = operation,
        serviceTypeChosen = chosen,
        registerSubscriptionForm = RegisterSubscriptionFormState(installationType = type),
    )

    @Test
    fun `usesPreauthorization is false before a type is chosen`() {
        assertFalse(RegisterSubscriptionState(preauthorizationEnabled = true).usesPreauthorization)
    }

    @Test
    fun `fiber chosen uses preauthorization`() {
        assertTrue(state(InstallationType.FIBER).usesPreauthorization)
    }

    @Test
    fun `solo TV and wireless do not use preauthorization`() {
        assertFalse(state(InstallationType.ONLY_TV_FIBER).usesPreauthorization)
        assertFalse(state(InstallationType.WIRELESS).usesPreauthorization)
    }

    @Test
    fun `an active operation always uses preauthorization`() {
        val operation = OnuRegistrationOperation(
            id = "op-1",
            serial = "ZTEGC8F01A22",
            subscriptionId = null,
            phase = "WAITING_FOR_ACS",
            state = "WAITING",
            revision = 1,
        )
        assertTrue(state(InstallationType.ONLY_TV_FIBER, operation = operation).usesPreauthorization)
    }

    @Test
    fun `SERVICE_TYPE is the first step and has no fields`() {
        assertEquals(SERVICE_TYPE, RegisterSubscriptionWizardStep.values().first())
        assertNull(SERVICE_TYPE.next())
        assertNull(SERVICE_TYPE.previous())
        assertEquals(emptyList<FormFieldKey>(), wizardFieldsFor(SERVICE_TYPE, RegisterSubscriptionFormState()))
    }

    @Test
    fun `progress phases follow the path`() {
        assertEquals(
            listOf("Equipo", "Cliente", "Servicio", "Revisión"),
            registrationProgressPhases(true).map { it.label },
        )
        assertEquals(
            listOf("Cliente", "Servicio", "Revisión"),
            registrationProgressPhases(false).map { it.label },
        )
    }
}
