package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import com.dscorp.ispadmin.data.datasource.remote.CpeStatusResponse
import com.dscorp.ispadmin.di.buildApiGson
import com.dscorp.ispadmin.domain.model.Mufa
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class CpeStatusResponseTest {

    @Test
    fun `core cpe status maps to a reading`() {
        val json = """{"online":true,"rxDbm":"-21.4","lastInform":"2026-10-14T14:56:00Z","sn":"HWTC9A3F21B0",
            "capabilities":{"vendor":"Huawei","model":"HG8145V5"},"actionsEnabled":false}"""

        val reading = buildApiGson().fromJson(json, CpeStatusResponse::class.java).toReading()

        assertEquals(CpeReading(online = true, rxDbm = -21.4, lastInform = Instant.parse("2026-10-14T14:56:00Z"), model = "Huawei HG8145V5"), reading)
    }

    @Test
    fun `missing optical values stay empty`() {
        val reading = buildApiGson().fromJson("""{"online":false,"rxDbm":null,"lastInform":null}""", CpeStatusResponse::class.java).toReading()

        assertEquals(CpeReading(online = false, rxDbm = null, lastInform = null, model = null), reading)
    }

    @Test
    fun `a mufa reads as its reference and thread count`() {
        assertEquals("Antena Claro (96 hilos)", mufaLabel(Mufa(id = 4, reference = "Antena Claro", threads = 96)))
        assertEquals("Antena Claro", mufaLabel(Mufa(id = 4, reference = "Antena Claro")))
    }
}
