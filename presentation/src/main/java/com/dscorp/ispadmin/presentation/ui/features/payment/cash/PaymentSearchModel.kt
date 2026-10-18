package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import com.dscorp.ispadmin.data.datasource.remote.CashPaymentResponse
import com.dscorp.ispadmin.data.datasource.remote.CashReceiptResponse
import com.dscorp.ispadmin.data.datasource.remote.CashSearchResponse
import com.dscorp.ispadmin.presentation.ui.features.home.dayMonth
import com.dscorp.ispadmin.presentation.ui.features.home.monthShort
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

enum class PaymentPeriod(val days: Int, val label: String) {
    LAST_7(7, "Últimos 7 días"),
    LAST_30(30, "Últimos 30 días"),
    LAST_90(90, "Últimos 90 días"),
}

data class PaymentHit(val receiptId: Int, val name: String, val detail: String, val amount: String)
data class PaymentDayGroup(val label: String, val hits: List<PaymentHit>)

/** a-caj-06: payments found, grouped by the day they came in. */
data class PaymentSearchResult(val groups: List<PaymentDayGroup>, val footer: String) {
    companion object {
        fun from(response: CashSearchResponse, period: PaymentPeriod, today: LocalDate): PaymentSearchResult {
            val groups = response.receipts
                .groupBy { paidAt(it.paidAt)?.toLocalDate() }
                .map { (day, receipts) -> PaymentDayGroup(dayLabel(day, today), receipts.map(::hit)) }
            val count = response.receipts.size
            val window = "en los ${period.label.lowercase()}"
            val footer = when (count) {
                0 -> "Sin pagos $window"
                1 -> "1 pago $window"
                else -> "$count pagos $window"
            }
            return PaymentSearchResult(groups, footer)
        }

        private fun hit(receipt: CashReceiptResponse): PaymentHit {
            val payer = receipt.payerName?.takeIf { it.isNotBlank() }?.let { "pagó: $it" }
            val time = paidAt(receipt.paidAt)?.let { "%02d:%02d".format(it.hour, it.minute) }
            return PaymentHit(
                receiptId = receipt.id,
                name = receipt.subscriber.orEmpty().toTitleCase().ifBlank { "Sin titular" },
                detail = listOfNotNull(methodLabel(receipt.method), payer, time).joinToString(" · "),
                amount = formatSoles(receipt.amount),
            )
        }

        private fun dayLabel(day: LocalDate?, today: LocalDate): String = when (day) {
            null -> "Sin fecha"
            today -> "Hoy"
            today.minusDays(1) -> "Ayer"
            else -> dayMonth(day)
        }
    }
}

/** a-caj-05: one receipt as the client would read it. */
data class PaymentDetail(
    val receiptId: Int,
    val title: String,
    val amount: String,
    val holder: String,
    val rows: List<Pair<String, String>>,
    val method: String,
    val subscriptionId: Int?,
    val hasProof: Boolean,
) {
    companion object {
        fun from(response: CashPaymentResponse): PaymentDetail {
            val month = response.serviceMonth?.let { runCatching { YearMonth.parse(it) }.getOrNull() }?.let(::monthYear)
            val method = methodLabel(response.method) ?: "Sin método"
            val paid = paidAt(response.paidAt)
            val due = response.dueDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val discount = response.discountAmount?.takeIf { it > 0 }
                ?.let { listOfNotNull(formatSoles(it), response.discountReason?.takeIf(String::isNotBlank)).joinToString(" · ") }
            return PaymentDetail(
                receiptId = response.id,
                title = month?.let { "Pago de $it" } ?: "Pago",
                amount = formatSoles(response.amount),
                holder = listOfNotNull(
                    response.subscriber?.toTitleCase()?.takeIf { it.isNotBlank() },
                    response.subscriptionId?.let { "Cód. $it" },
                ).joinToString(" · "),
                rows = listOfNotNull(
                    month?.let { "Recibo" to it },
                    due?.let { "Vencía" to "${dayMonth(it)} ${it.year}" },
                    paid?.let { "Pagado" to "${dayMonth(it.toLocalDate())} ${it.year} · ${"%02d:%02d".format(it.hour, it.minute)}" },
                    "Método" to method,
                    response.payerName?.takeIf { it.isNotBlank() }?.let { "Nombre en $method" to it },
                    "Descuento" to (discount ?: "—"),
                    "Registrado por" to (response.collector?.takeIf { it.isNotBlank() }?.let(::shortName) ?: "Sin registrar"),
                    "N.º" to "P-${response.id}",
                ),
                method = method,
                subscriptionId = response.subscriptionId,
                hasProof = response.hasProof,
            )
        }
    }

    /** Plain text the cashier sends to the client. */
    fun shareText(): String = buildString {
        appendLine("Comprobante de pago P-$receiptId")
        if (holder.isNotBlank()) appendLine(holder)
        appendLine("Monto: $amount")
        rows.filter { it.first != "N.º" }.forEach { (label, value) -> appendLine("$label: $value") }
    }.trimEnd()
}

/** Index ranges of [text] that match any of [words], ignoring case and accents. */
internal fun matchRanges(text: String, words: List<String>): List<IntRange> {
    val folded = fold(text)
    return words.map(::fold).filter { it.isNotBlank() }.flatMap { word ->
        generateSequence(folded.indexOf(word).takeIf { it >= 0 }) { from ->
            folded.indexOf(word, from + word.length).takeIf { it >= 0 }
        }.map { it until it + word.length }.toList()
    }.sortedBy { it.first }
}

/** Lower case without accents, one char per char so indexes keep matching the original text. */
private fun fold(text: String): String = text.map { char ->
    Normalizer.normalize(char.toString(), Normalizer.Form.NFD).first().lowercaseChar()
}.joinToString("")

private fun monthYear(month: YearMonth) = "${monthShort(month.atDay(1))} ${month.year}"

private fun methodLabel(method: String?): String? =
    method?.trim()?.lowercase()?.replaceFirstChar { it.uppercase() }?.takeIf { it.isNotBlank() }

private fun paidAt(value: String): LocalDateTime? = runCatching { LocalDateTime.parse(value) }.getOrNull()
