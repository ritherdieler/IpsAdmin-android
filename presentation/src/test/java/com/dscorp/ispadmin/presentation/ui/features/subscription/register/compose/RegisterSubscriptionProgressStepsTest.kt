package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.RegistrationProgressCheckpoint
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterSubscriptionProgressStepsTest {

    @Test
    fun `uses backend message when present`() {
        assertEquals("Esperando ACS…", registrationProgressStepMessage("Esperando ACS…"))
        assertEquals("Aplicando WiFi…", registrationProgressStepMessage("Aplicando WiFi…"))
    }

    @Test
    fun `falls back to registering when blank`() {
        assertEquals("Registrando…", registrationProgressStepMessage(null))
        assertEquals("Registrando…", registrationProgressStepMessage(""))
        assertEquals("Registrando…", registrationProgressStepMessage("   "))
    }

    @Test
    fun `catalog load is not shown as registering overlay`() {
        assertEquals(
            RegisterScreenBusyMode.CATALOG,
            registerScreenBusyMode(isLoading = true, isRegistering = false)
        )
        assertEquals(
            RegisterScreenBusyMode.REGISTERING,
            registerScreenBusyMode(isLoading = true, isRegistering = true)
        )
        assertEquals(
            RegisterScreenBusyMode.NONE,
            registerScreenBusyMode(isLoading = false, isRegistering = false)
        )
    }

    @Test
    fun `registration checkpoints use the same labels as the provisioning screen`() {
        assertEquals("Limpieza de WAN", registrationProgressStageLabel("WAN_CLEANUP"))
        assertEquals("Requiere atención", registrationProgressStateLabel("FAILED"))
        assertEquals("Pendiente", registrationProgressStateLabel("PENDING"))
    }

    @Test
    fun `obsolete management checkpoint is not presented as a registration stage`() {
        assertEquals("Etapa desconocida", registrationProgressStageLabel("LEGACY_MANAGEMENT"))
        assertEquals("Aprovisionando registro…", registrationProgressStepMessage(null, listOf(
            RegistrationProgressCheckpoint(stage = "LEGACY_MANAGEMENT", state = "RUNNING", attempts = 1)
        )))
    }

    @Test
    fun `operation cancellation states are not presented as checkpoint states`() {
        assertEquals("Estado desconocido", registrationProgressStateLabel("CANCEL_REQUESTED"))
        assertEquals("Estado desconocido", registrationProgressStateLabel("CANCELLED"))
    }

    private fun registering(type: InstallationType, operation: OnuRegistrationOperation? = null) = RegisterSubscriptionState(
        registerSubscriptionForm = RegisterSubscriptionFormState(installationType = type),
        preauthorizationEnabled = true,
        serviceTypeChosen = true,
        preauthorizationOperation = operation,
        isRegistering = true,
    )

    private val operation = OnuRegistrationOperation(
        id = "op-1", serial = "VSOL0031C0B6", subscriptionId = null,
        phase = "READY_FOR_FORM", state = "READY_FOR_FORM", revision = 1,
    )

    @Test
    fun `the registering overlay offers the OLT cancel only on the fiber path`() {
        assertTrue(registrationOverlayCancelsOnu(registering(InstallationType.FIBER)))
        assertTrue(registrationOverlayCancelsOnu(registering(InstallationType.FIBER, operation)))
        assertFalse(registrationOverlayCancelsOnu(registering(InstallationType.ONLY_TV_FIBER)))
        assertFalse(registrationOverlayCancelsOnu(registering(InstallationType.WIRELESS)))
    }

    @Test
    fun `the catalog loader offers the OLT cancel only while a preauthorization request runs`() {
        val fiber = registering(InstallationType.FIBER).copy(isRegistering = false)
        assertTrue(catalogOverlayCancelsOnu(fiber.copy(isPreauthorizationRequestInProgress = true)))
        assertFalse(catalogOverlayCancelsOnu(fiber))
        assertFalse(
            catalogOverlayCancelsOnu(
                registering(InstallationType.ONLY_TV_FIBER).copy(isRegistering = false, isPreauthorizationRequestInProgress = true),
            ),
        )
    }
}
