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
    fun `a 404 without a message is an endpoint this Core does not have`() {
        // Spring Boot's default error page for a route the current Core does not map.
        val springDefault = """{"timestamp":"2026-10-20T10:00:00.000+00:00","status":404,"error":"Not Found","message":"No message available","path":"/ispadmin/onu/lookup"}"""
        assertEquals(true, ApiErrorBodyParser.isMissingEndpoint(404, springDefault))
        assertEquals(true, ApiErrorBodyParser.isMissingEndpoint(404, """{"status":404,"error":"Not Found","message":"","path":"/x"}"""))
        assertEquals(true, ApiErrorBodyParser.isMissingEndpoint(404, ""))
        assertEquals(true, ApiErrorBodyParser.isMissingEndpoint(404, "<html>Not Found</html>"))
    }

    @Test
    fun `a 404 that says what is missing comes from the Core itself`() {
        val notFound = """{"status":404,"error":"Not Found","message":"No se encontró la ONU en la OLT"}"""
        assertEquals(false, ApiErrorBodyParser.isMissingEndpoint(404, notFound))
        assertEquals(false, ApiErrorBodyParser.isMissingEndpoint(404, """{"code":"NOT_FOUND","message":"Subscription not found","error":"Subscription not found"}"""))
        assertEquals(false, ApiErrorBodyParser.isMissingEndpoint(400, ""))
    }

    @Test
    fun `falls back when body empty or invalid`() {
        assertEquals("fallback", ApiErrorBodyParser.parse(null, "fallback"))
        assertEquals("fallback", ApiErrorBodyParser.parse(" ", "fallback"))
        assertEquals("fallback", ApiErrorBodyParser.parse("not-json", "fallback"))
    }
}
