package com.dscorp.ispadmin.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmedActionHeadersTest {

    @Test
    fun `confirm header matches service-health contract`() {
        assertEquals("X-Confirm-Action", ConfirmedActionHeaders.CONFIRM_HEADER)
        assertEquals("true", ConfirmedActionHeaders.CONFIRM_VALUE)
        assertEquals("Idempotency-Key", ConfirmedActionHeaders.IDEMPOTENCY_HEADER)
    }

    @Test
    fun `idempotency key matches backend regex length and charset`() {
        val key = ConfirmedActionHeaders.newIdempotencyKey()
        assertTrue(key.matches(Regex("[A-Za-z0-9_-]{8,128}")))
        val other = ConfirmedActionHeaders.newIdempotencyKey()
        assertTrue(key != other)
    }
}
