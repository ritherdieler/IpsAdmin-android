package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import java.text.Collator
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
enum class TicketDateFilter {
    TODAY,
    YESTERDAY,
    TWO_DAYS_AGO,
    ALL
}

enum class TicketSortOption {
    DATE_DESCENDING,
    DATE_ASCENDING,
    NAME_ASCENDING,
    NAME_DESCENDING
}

internal fun List<AssistanceTicketResponse>.filterAndSortTickets(
    dateFilter: TicketDateFilter,
    sortOption: TicketSortOption,
    now: Date = Date(),
    timeZone: TimeZone = TimeZone.getTimeZone("America/Lima")
): List<AssistanceTicketResponse> {
    val filteredTickets = when (dateFilter) {
        TicketDateFilter.ALL -> this

        TicketDateFilter.TODAY -> filterByDayOffset(
            dayOffset = 0,
            now = now,
            timeZone = timeZone
        )

        TicketDateFilter.YESTERDAY -> filterByDayOffset(
            dayOffset = -1,
            now = now,
            timeZone = timeZone
        )

        TicketDateFilter.TWO_DAYS_AGO -> filterByDayOffset(
            dayOffset = -2,
            now = now,
            timeZone = timeZone
        )
    }

    return filteredTickets.sortedWith(
        ticketComparator(sortOption)
    )
}

private fun List<AssistanceTicketResponse>.filterByDayOffset(
    dayOffset: Int,
    now: Date,
    timeZone: TimeZone
): List<AssistanceTicketResponse> {
    val startOfTargetDay = Calendar.getInstance(timeZone).apply {
        time = now
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val startOfNextDay = startOfTargetDay.clone() as Calendar
    startOfNextDay.add(Calendar.DAY_OF_YEAR, 1)

    return filter { ticket ->
        val ticketTime = ticket.effectiveDate().time

        ticketTime >= startOfTargetDay.timeInMillis &&
                ticketTime < startOfNextDay.timeInMillis
    }
}

private fun AssistanceTicketResponse.effectiveDate(): Date {
    return scheduledAt ?: createdAt
}

private fun ticketComparator(
    sortOption: TicketSortOption
): Comparator<AssistanceTicketResponse> {
    val nameCollator = Collator.getInstance(
        Locale.forLanguageTag("es-PE")
    ).apply {
        strength = Collator.PRIMARY
    }

    return when (sortOption) {
        TicketSortOption.DATE_DESCENDING ->
            compareByDescending<AssistanceTicketResponse> {
                it.effectiveDate().time
            }.thenByDescending {
                it.id
            }

        TicketSortOption.DATE_ASCENDING ->
            compareBy<AssistanceTicketResponse> {
                it.effectiveDate().time
            }.thenBy {
                it.id
            }

        TicketSortOption.NAME_ASCENDING ->
            Comparator<AssistanceTicketResponse> { firstTicket, secondTicket ->
                nameCollator.compare(
                    firstTicket.name.trim(),
                    secondTicket.name.trim()
                )
            }.thenByDescending {
                it.effectiveDate().time
            }

        TicketSortOption.NAME_DESCENDING ->
            Comparator<AssistanceTicketResponse> { firstTicket, secondTicket ->
                nameCollator.compare(
                    secondTicket.name.trim(),
                    firstTicket.name.trim()
                )
            }.thenByDescending {
                it.effectiveDate().time
            }
    }
}