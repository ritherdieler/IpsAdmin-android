package com.dscorp.ispadmin.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnuRegistrationOperationPolicyTest {
    @Test
    fun `registration form is gated on confirmed ACS contact`() {
        assertFalse(OnuRegistrationOperation("1", "VSOL0031C0B6", null, "WAITING_FOR_ACS", "WAITING", 2).canOpenRegistrationForm())
        assertFalse(OnuRegistrationOperation("1", "VSOL0031C0B6", null, "OLT_AUTHORIZATION", "RUNNING", 2).canOpenRegistrationForm())
        assertTrue(OnuRegistrationOperation("1", "VSOL0031C0B6", null, "READY_FOR_FORM", "READY_FOR_FORM", 3).canOpenRegistrationForm())
    }

    @Test
    fun `manual retry is offered only while authorization or ACS confirmation is pending`() {
        assertTrue(OnuRegistrationOperation("1", "VSOL0031C0B6", null, "WAITING_FOR_ACS", "WAITING", 2).canManuallyRetry())
        assertTrue(OnuRegistrationOperation("1", "VSOL0031C0B6", null, "OLT_AUTHORIZATION", "FAILED", 2).canManuallyRetry())
        assertFalse(OnuRegistrationOperation("1", "VSOL0031C0B6", null, "READY_FOR_FORM", "READY_FOR_FORM", 3).canManuallyRetry())
    }
}
