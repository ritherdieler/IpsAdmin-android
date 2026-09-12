package com.dscorp.ispadmin.domain.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionResponseAccessFieldsTest {

    private val gson = Gson()

    @Test
    fun `gson keeps access fields null when backend has not sent them yet`() {
        val json = """
            {
              "id": 10,
              "firstName": "Ana",
              "lastName": "Perez",
              "ip": "10.11.1.40",
              "serviceStatus": "ACTIVE",
              "isMigration": false,
              "installationType": "FIBER",
              "email": null,
              "pendingInvoiceQuantity": 0,
              "antiquityInMonths": 0,
              "qualification": 0,
              "ics": 0,
              "totalDebt": 0.0,
              "lastPaymentDate": null
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, SubscriptionResponse::class.java)

        assertEquals(10, parsed.id)
        assertEquals("10.11.1.40", parsed.ip)
        assertNull(parsed.accessMode)
        assertNull(parsed.pppoeUsername)
        assertNull(parsed.accessMigrationStage)
        assertNull(parsed.accessMigration)
        assertNull(parsed.resolvedPppoeUsername())
        assertNull(parsed.resolvedMigrationStage())
    }

    @Test
    fun `gson maps accessMode pppoeUsername and nested migration status`() {
        val json = """
            {
              "id": 11,
              "firstName": "Luis",
              "ip": null,
              "serviceStatus": "ACTIVE",
              "isMigration": false,
              "installationType": "FIBER",
              "email": null,
              "pendingInvoiceQuantity": 0,
              "antiquityInMonths": 0,
              "qualification": 0,
              "ics": 0,
              "totalDebt": 0.0,
              "lastPaymentDate": null,
              "accessMode": "PPPOE_DYNAMIC",
              "pppoeUsername": "gf-11",
              "accessMigrationStage": "DONE",
              "accessMigration": {
                "subscriptionId": 11,
                "stage": "DONE",
                "pppoeUsername": "gf-11",
                "done": true
              }
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, SubscriptionResponse::class.java)

        assertEquals("PPPOE_DYNAMIC", parsed.accessMode)
        assertEquals("gf-11", parsed.pppoeUsername)
        assertEquals("DONE", parsed.accessMigrationStage)
        assertEquals("gf-11", parsed.resolvedPppoeUsername())
        assertEquals("DONE", parsed.resolvedMigrationStage())
        assertEquals("DONE", parsed.accessMigration?.stage)
    }
}
