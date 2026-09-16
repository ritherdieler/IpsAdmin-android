package com.dscorp.ispadmin.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccessModeTest {

    @Test
    fun `parse accepts enum names and pppoe static aliases`() {
        assertEquals(AccessMode.PPPOE_DYNAMIC, AccessMode.parse("PPPOE_DYNAMIC"))
        assertEquals(AccessMode.PPPOE_DYNAMIC, AccessMode.parse("pppoe"))
        assertEquals(AccessMode.PPPOE_DYNAMIC, AccessMode.parse(" PPPoE "))
        assertEquals(AccessMode.STATIC_IP, AccessMode.parse("STATIC_IP"))
        assertEquals(AccessMode.STATIC_IP, AccessMode.parse("static"))
        assertEquals(AccessMode.STATIC_IP, AccessMode.parse("static_ip"))
        assertEquals(AccessMode.PPPOE_FIXED, AccessMode.parse("PPPOE_FIXED"))
        assertNull(AccessMode.parse(null))
        assertNull(AccessMode.parse(""))
        assertNull(AccessMode.parse("wifi"))
    }

    @Test
    fun `parseOrDefault is PPPoE when blank or unknown`() {
        assertEquals(AccessMode.PPPOE_DYNAMIC, AccessMode.parseOrDefault(null))
        assertEquals(AccessMode.PPPOE_DYNAMIC, AccessMode.parseOrDefault(""))
        assertEquals(AccessMode.PPPOE_DYNAMIC, AccessMode.parseOrDefault("   "))
        assertEquals(AccessMode.PPPOE_DYNAMIC, AccessMode.parseOrDefault("unknown"))
        assertEquals(AccessMode.STATIC_IP, AccessMode.parseOrDefault("static"))
    }

    @Test
    fun `register choices are PPPoE and static IP`() {
        assertEquals(
            listOf(AccessMode.PPPOE_DYNAMIC, AccessMode.STATIC_IP),
            AccessMode.registerChoices(),
        )
    }
}
