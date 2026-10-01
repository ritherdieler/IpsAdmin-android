package com.dscorp.ispadmin.data.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorBodyParserTest {

    @Test
    fun `prefers error field from backend envelope`() {
        val body = """{"timestamp":"x","status":409,"error":"La ONU HWTC15F610C6 ya está asignada","message":"otro","path":"/ispadmin/subscription"}"""
        assertEquals(
            "La ONU HWTC15F610C6 ya está asignada",
            ApiErrorBodyParser.parse(body, "fallback")
        )
    }

    @Test
    fun `uses message when error is missing`() {
        val body = """{"status":500,"message":"Detalle de negocio"}"""
        assertEquals("Detalle de negocio", ApiErrorBodyParser.parse(body, "fallback"))
    }

    @Test
    fun `extracts errorCode from conflict envelopes`() {
        val body = """{"status":409,"error":"La solicitud ya fue procesada","errorCode":"REGISTRATION_ALREADY_EXISTS"}"""
        assertEquals("REGISTRATION_ALREADY_EXISTS", ApiErrorBodyParser.errorCode(body))
        assertEquals(null, ApiErrorBodyParser.errorCode("""{"status":500}"""))
        assertEquals(null, ApiErrorBodyParser.errorCode("not-json"))
    }

    @Test
    fun `falls back when body empty or invalid`() {
        assertEquals("fallback", ApiErrorBodyParser.parse(null, "fallback"))
        assertEquals("fallback", ApiErrorBodyParser.parse(" ", "fallback"))
        assertEquals("fallback", ApiErrorBodyParser.parse("not-json", "fallback"))
    }
}
