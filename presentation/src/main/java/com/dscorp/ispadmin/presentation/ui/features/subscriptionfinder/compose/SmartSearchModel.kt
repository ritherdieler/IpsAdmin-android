package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.badge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import java.text.Normalizer

/** Reads what the operator typed in the single search box and picks the matching search. */
object SmartQuery {
    private val IP = Regex("^\\d{1,3}(\\.\\d{0,3}){1,3}$")
    private val DIGITS = Regex("^\\d+$")
    private const val MAX_CODE_LENGTH = 6

    fun toFilter(text: String): SubscriptionFilter? {
        val value = text.trim()
        return when (kindOf(value)) {
            Kind.NONE -> null
            Kind.IP -> SubscriptionFilter.BY_IP(value)
            Kind.DOCUMENT -> SubscriptionFilter.BY_DOCUMENT(value)
            Kind.CODE -> SubscriptionFilter.BY_CODE(value)
            Kind.NAME -> SubscriptionFilter.BY_NAME(name = value.uppercase(), lastName = "")
        }
    }

    fun kindLabel(text: String): String? = when (kindOf(text.trim())) {
        Kind.NONE -> null
        Kind.IP -> "IP"
        Kind.DOCUMENT -> "DNI o RUC"
        Kind.CODE -> "Código"
        Kind.NAME -> "Nombre"
    }

    private enum class Kind { NONE, IP, DOCUMENT, CODE, NAME }

    private fun kindOf(value: String): Kind = when {
        value.isEmpty() -> Kind.NONE
        IP.matches(value) -> Kind.IP
        DIGITS.matches(value) && (value.length == 8 || value.length == 11) -> Kind.DOCUMENT
        DIGITS.matches(value) && value.length <= MAX_CODE_LENGTH -> Kind.CODE
        else -> Kind.NAME
    }
}

data class SubscriptionRowModel(
    val name: String,
    val initials: String,
    val subtitle: String,
    val status: StatusBadge,
    val debt: String,
    val owes: Boolean,
) {
    companion object {
        fun from(resume: SubscriptionResume): SubscriptionRowModel {
            val name = resume.customerName.toTitleCase()
            val owes = resume.totalDebt > 0.0
            return SubscriptionRowModel(
                name = name,
                initials = name.split(' ').filter { it.isNotEmpty() }.take(2)
                    .joinToString("") { it.first().uppercase() },
                subtitle = listOf(planLabel(resume.planName), resume.placeName.toTitleCase())
                    .filter { it.isNotEmpty() }.joinToString(" · "),
                status = resume.serviceStatus.badge(),
                debt = if (owes) formatSoles(resume.totalDebt) else "Al día",
                owes = owes,
            )
        }
    }
}

/** Catalog names such as "basico_wireless 50" read as "Basico wireless 50". */
internal fun planLabel(planName: String): String =
    planName.replace('_', ' ').trim().replaceFirstChar { it.uppercase() }

/** Character ranges of [name] that match any word of [query], ignoring case and accents. */
internal fun highlightRanges(name: String, query: String): List<IntRange> {
    val haystack = name.foldForSearch()
    val ranges = query.foldForSearch().split(Regex("\\s+")).filter { it.isNotEmpty() }.flatMap { word ->
        generateSequence(haystack.indexOf(word)) { from -> haystack.indexOf(word, from + 1).takeIf { it >= 0 } }
            .takeWhile { it >= 0 }
            .map { it until it + word.length }
            .toList()
    }.sortedBy { it.first }
    return ranges.fold(mutableListOf()) { merged, range ->
        val last = merged.lastOrNull()
        if (last != null && range.first <= last.last + 1) {
            merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
        } else merged += range
        merged
    }
}

/** Lowercase without diacritics, keeping one character per input character. */
private fun String.foldForSearch(): String = map { char ->
    Normalizer.normalize(char.toString(), Normalizer.Form.NFD).first().lowercaseChar()
}.joinToString("")

enum class ResultFilter(val label: String) {
    ALL("Todos"),
    WITH_DEBT("Con deuda"),
    CUT_OFF("Cortados"),
    FIBER("Fibra"),
    WIRELESS("Inalámbrico"),
    ONLY_TV("Solo TV");

    fun matches(resume: SubscriptionResume): Boolean = when (this) {
        ALL -> true
        WITH_DEBT -> resume.totalDebt > 0.0
        CUT_OFF -> resume.serviceStatus == ServiceStatus.CUT_OFF
        FIBER -> resume.installationType == InstallationType.FIBER
        WIRELESS -> resume.installationType == InstallationType.WIRELESS
        ONLY_TV -> resume.installationType == InstallationType.ONLY_TV_FIBER
    }
}

internal fun resultCountLabel(count: Int, canLoadMore: Boolean): String {
    val number = if (canLoadMore) "$count+" else "$count"
    return if (count == 1 && !canLoadMore) "1 resultado" else "$number resultados"
}
