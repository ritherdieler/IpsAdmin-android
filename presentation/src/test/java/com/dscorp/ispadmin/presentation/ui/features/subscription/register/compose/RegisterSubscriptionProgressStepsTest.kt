package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import org.junit.Assert.assertEquals
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
}
