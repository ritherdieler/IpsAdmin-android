package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DD_MM_YYYY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun formatDatePickerUtcMillis(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(DD_MM_YYYY)

fun todayDdMmYyyy(clock: Clock = Clock.systemDefaultZone()): String =
    LocalDate.now(clock).format(DD_MM_YYYY)

fun parseDdMmYyyyToUtcMillis(value: String): Long? {
    if (value.isBlank()) return null
    return runCatching {
        LocalDate.parse(value, DD_MM_YYYY)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}
