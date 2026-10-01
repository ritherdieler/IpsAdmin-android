package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import org.junit.Assert.assertEquals
import org.junit.Test
import com.dscorp.ispadmin.domain.model.RegistrationProgressCheckpoint

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
}
