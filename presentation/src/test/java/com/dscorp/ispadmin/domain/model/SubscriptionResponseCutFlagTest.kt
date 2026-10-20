package com.dscorp.ispadmin.domain.model

import com.dscorp.ispadmin.di.buildApiGson
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionResponseCutFlagTest {

    private fun parse(cutFlag: String) = buildApiGson().fromJson(
        """
            {
              "id": 1842,
              "serviceStatus": "ACTIVE",
              "isMigration": false,
              "installationType": "FIBER",
              "pendingInvoiceQuantity": 0,
              "antiquityInMonths": 19,
              "qualification": 0,
              "ics": 10,
              "totalDebt": 0.0
              $cutFlag
            }
        """.trimIndent(),
        SubscriptionResponse::class.java,
    )

    @Test
    fun `reads the cut flag the current Core sends next to an ACTIVE status`() {
        assertEquals(true, parse(""", "isServiceCutOff": true""").isServiceCutOff)
        assertEquals(true, parse(""", "serviceCutOff": true""").isServiceCutOff)
        assertEquals(false, parse("").isServiceCutOff)
    }
}
