package com.dscorp.ispadmin.domain.model

import com.dscorp.ispadmin.di.buildApiGson
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceStatusTest {

    private data class Holder(val serviceStatus: ServiceStatus)

    private fun parse(json: String) = buildApiGson().fromJson(json, Holder::class.java).serviceStatus

    @Test
    fun `api gson reads the three Core statuses`() {
        assertEquals(ServiceStatus.ACTIVE, parse("""{"serviceStatus":"ACTIVE"}"""))
        assertEquals(ServiceStatus.CUT_OFF, parse("""{"serviceStatus":"CUT_OFF"}"""))
        assertEquals(ServiceStatus.CANCELLED, parse("""{"serviceStatus":"CANCELLED"}"""))
    }

    @Test
    fun `api gson maps unknown or null status to UNKNOWN`() {
        assertEquals(ServiceStatus.UNKNOWN, parse("""{"serviceStatus":"PAUSED"}"""))
        assertEquals(ServiceStatus.UNKNOWN, parse("""{"serviceStatus":"SUSPENDED"}"""))
        assertEquals(ServiceStatus.UNKNOWN, parse("""{"serviceStatus":null}"""))
    }

    @Test
    fun `formatted status names every state`() {
        assertEquals("Activo", ServiceStatus.ACTIVE.getFormattedStatus())
        assertEquals("Cortado", ServiceStatus.CUT_OFF.getFormattedStatus())
        assertEquals("Cancelado", ServiceStatus.CANCELLED.getFormattedStatus())
        assertEquals("Desconocido", ServiceStatus.UNKNOWN.getFormattedStatus())
    }
}
