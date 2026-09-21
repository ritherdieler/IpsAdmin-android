package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SubscriptionFinderDateUtilsTest {

    @Test
    fun `formats Material DatePicker UTC midnight as the selected civil day`() {
        val utcMidnight = LocalDate.of(2024, 3, 21)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertEquals("21/03/2024", formatDatePickerUtcMillis(utcMidnight))
    }

    @Test
    fun `does not shift the day when formatting UTC midnight for western offsets`() {
        val utcMidnight = LocalDate.of(2026, 9, 21)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertEquals("21/09/2026", formatDatePickerUtcMillis(utcMidnight))
    }

    @Test
    fun `today uses the local calendar day from the clock`() {
        val clock = Clock.fixed(Instant.parse("2026-09-21T08:30:00Z"), ZoneOffset.ofHours(-5))
        assertEquals("21/09/2026", todayDdMmYyyy(clock))
    }

    @Test
    fun `parses display date back to UTC midnight millis`() {
        val expected = LocalDate.of(2024, 3, 21)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertEquals(expected, parseDdMmYyyyToUtcMillis("21/03/2024"))
        assertNull(parseDdMmYyyyToUtcMillis(""))
        assertNull(parseDdMmYyyyToUtcMillis("invalid"))
    }
}
