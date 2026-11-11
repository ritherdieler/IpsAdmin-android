package com.dscorp.ispadmin.presentation.util

import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.receivedLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class Clock12Test {

    @Test
    fun `morning, noon, afternoon and midnight in Peruvian Spanish`() {
        assertEquals("9:47 a. m.", clock12(9, 47))
        assertEquals("12:00 p. m.", clock12(12, 0))
        assertEquals("9:03 p. m.", clock12(21, 3))
        assertEquals("12:05 a. m.", clock12(0, 5))
    }

    @Test
    fun `received label of a pending proof uses the 12-hour clock`() {
        val today = LocalDate.of(2026, 11, 11)
        assertEquals("hoy 10:42 a. m.", receivedLabel("2026-11-11T10:42:10", today))
        assertEquals("ayer 9:03 p. m.", receivedLabel("2026-11-10T21:03:00-05:00", today))
        assertEquals("8 nov 9:15 a. m.", receivedLabel("2026-11-08T09:15:00", today))
        assertEquals("", receivedLabel("2026-11-11", today))
    }
}
