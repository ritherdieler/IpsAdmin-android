package com.dscorp.ispadmin.presentation.fcm

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceAnnouncerTest {

    private val engines = mutableListOf<FakeEngine>()
    private val announcer = VoiceAnnouncer(log = {}) { onReady -> FakeEngine(onReady).also(engines::add) }

    @Test
    fun `a ticket that arrives while the engine starts is spoken once it is ready`() {
        announcer.announce("Nuevo ticket, prioridad alta.")
        assertEquals(emptyList<String>(), engines.single().spoken)

        engines.single().finishStart(success = true)

        assertEquals(listOf("Nuevo ticket, prioridad alta."), engines.single().spoken)
    }

    @Test
    fun `tickets in a row are all spoken in order with a single engine`() {
        announcer.announce("Primero.")
        announcer.announce("Segundo.")
        engines.single().finishStart(success = true)
        announcer.announce("Tercero.")

        assertEquals(1, engines.size)
        assertEquals(listOf("Primero.", "Segundo.", "Tercero."), engines.single().spoken)
    }

    @Test
    fun `an engine that fails to start is retried on the next ticket`() {
        announcer.announce("Se pierde.")
        engines.single().finishStart(success = false)

        announcer.announce("Se escucha.")
        engines.last().finishStart(success = true)

        assertEquals(2, engines.size)
        assertEquals(listOf("Se escucha."), engines.last().spoken)
    }

    @Test
    fun `an engine that stops working is rebuilt and the ticket is still spoken`() {
        announcer.announce("Primero.")
        engines.single().finishStart(success = true)
        engines.single().working = false

        announcer.announce("Segundo.")
        engines.last().finishStart(success = true)

        assertEquals(2, engines.size)
        assertEquals(listOf("Segundo."), engines.last().spoken)
    }

    @Test
    fun `a ticket reads the speech sent by the Core`() {
        assertEquals("Nuevo ticket, prioridad baja. Otros.", ticketSpeech(message(speech = "Nuevo ticket, prioridad baja. Otros.")))
    }

    @Test
    fun `a ticket from an older Core reads the fixed phrase`() {
        assertEquals(DEFAULT_TICKET_SPEECH, ticketSpeech(message(speech = null)))
        assertEquals(DEFAULT_TICKET_SPEECH, ticketSpeech(message(speech = "  ")))
    }

    private fun message(speech: String?) = FcmMessage(
        title = "Nuevo Ticket | Prioridad Baja",
        type = FcmMessageType.ASSISTANCE_TICKET,
        message = "Otros - Revisar router",
        id = "7",
        data = "",
        speech = speech,
    )

    private class FakeEngine(private val onReady: (Boolean) -> Unit) : SpeechEngine {
        val spoken = mutableListOf<String>()
        var working = true
        fun finishStart(success: Boolean) = onReady(success)
        override fun speak(text: String): Boolean {
            if (working) spoken += text
            return working
        }
        override fun shutdown() = Unit
    }
}
