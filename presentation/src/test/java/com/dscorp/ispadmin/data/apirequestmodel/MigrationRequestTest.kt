package com.dscorp.ispadmin.data.apirequestmodel

import com.dscorp.ispadmin.domain.model.Onu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationRequestTest {

    @Test
    fun `migration request includes vlan for fiber migration`() {
        val request = MigrationRequest(
            onu = Onu(
                board = "1",
                olt_id = "2",
                onu = "0",
                onu_type_id = "80",
                onu_type_name = "F6600RV9.0.21",
                pon_type = "gpon",
                port = "11",
                sn = "ZTEGDC47BFD7",
            ),
            planId = "1",
            subscriptionId = 1205,
            price = "50",
            notes = "",
            vlan = "100",
        )

        assertTrue(request.isValid())
        assertEquals("100", request.vlan)
    }
}
