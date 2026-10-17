package com.dscorp.ispadmin.presentation.ui.features.supportTicket.create

import org.junit.Assert.assertEquals
import org.junit.Test

class TicketCategoriesTest {

    @Test
    fun `categories keep the Core names, most used first`() {
        assertEquals(
            listOf("Sin Conexión a Internet", "Otros", "Internet Lento", "Instalación de Internet", "No tiene señal de Tv Cable"),
            TICKET_CATEGORIES.take(TOP_CATEGORIES),
        )
        assertEquals(16, TICKET_CATEGORIES.size)
        assertEquals(TICKET_CATEGORIES.size, TICKET_CATEGORIES.distinct().size)
        assertEquals("Ver las 16 categorías", allCategoriesLabel())
    }

    @Test
    fun `priority follows the Core rule for each category`() {
        assertEquals("Alta", categoryPriority("Sin Conexión a Internet"))
        assertEquals("Media", categoryPriority("Cambio de Domicilio"))
        assertEquals("Media", categoryPriority("Otros"))
        assertEquals("Baja", categoryPriority("Cambio de Contraseña"))
        assertEquals("Baja", categoryPriority("Internet Lento"))
    }
}
