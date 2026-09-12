package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.domain.model.AccessMigrationStage
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessMigrationProgressDtoTest {

    private val gson = Gson()

    @Test
    fun `gson maps Core progress JSON including done and message`() {
        val json = """
            {
              "subscriptionId": 1001,
              "stage": "QUARANTINE",
              "attempt": 1,
              "pppoeUsername": "gf1001",
              "failureReason": null,
              "quarantineUntil": "2026-09-18T03:15:00",
              "done": true,
              "message": "En cuarentena"
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, AccessMigrationProgressDto::class.java).toDomain()

        assertEquals(1001, parsed.subscriptionId)
        assertEquals(AccessMigrationStage.QUARANTINE, parsed.stage)
        assertEquals("gf1001", parsed.pppoeUsername)
        assertEquals("2026-09-18T03:15:00", parsed.quarantineUntil)
        assertEquals(1, parsed.attempt)
        assertTrue(parsed.done)
        assertEquals("En cuarentena", parsed.message)
    }

    @Test
    fun `gson derives done from QUARANTINE when Core omits the flag`() {
        val json = """
            {
              "subscriptionId": 11,
              "stage": "QUARANTINE",
              "attempt": 1,
              "pppoeUsername": "gf-11"
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, AccessMigrationProgressDto::class.java).toDomain()

        assertTrue(parsed.done)
        assertNull(parsed.message)
        assertEquals(AccessMigrationStage.QUARANTINE, parsed.stage)
    }

    @Test
    fun `gson keeps in-flight stages as not done when Core omits the flag`() {
        val json = """
            {
              "subscriptionId": 11,
              "stage": "CPE_APPLIED",
              "attempt": 1
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, AccessMigrationProgressDto::class.java).toDomain()

        assertFalse(parsed.done)
        assertEquals(AccessMigrationStage.CPE_APPLIED, parsed.stage)
    }

    @Test
    fun `gson maps Core eligible page wrapper with name not customerName`() {
        val json = """
            {
              "items": [
                {
                  "subscriptionId": 1001,
                  "name": "Ana Fiber",
                  "ip": "192.168.1.50",
                  "onuSn": "ZTEGDC47BFFD",
                  "productClass": "F6600R",
                  "planName": "INTERNET 60",
                  "reason": null,
                  "eligible": true
                }
              ]
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, AccessMigrationEligiblePageDto::class.java)

        assertEquals(1, parsed.items.size)
        assertEquals(1001, parsed.items.first().subscriptionId)
        assertEquals("Ana Fiber", parsed.items.first().name)
        assertEquals("ZTEGDC47BFFD", parsed.items.first().onuSn)
        assertEquals(true, parsed.items.first().eligible)
    }
}
