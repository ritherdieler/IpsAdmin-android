package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.PaymentMethodOption
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

enum class BadgeTone { SUCCESS, WARNING, ERROR, INFO, NEUTRAL }

data class StatusBadge(val text: String, val tone: BadgeTone)

private val PERU = Locale("es", "PE")
private const val DEFAULT_METHOD = "Yape"
/** With two or more unpaid receipts the Core refuses to restore a cut service. */
private const val MAX_PENDING_TO_RESTORE = 1
private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "set", "oct", "nov", "dic")

/** Most names are stored in capitals: the UI shows them in title case without touching the data. */
internal fun displayName(firstName: String?, lastName: String?): String =
    listOfNotNull(firstName, lastName).joinToString(" ").toTitleCase()

internal fun String.toTitleCase(): String = trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    .joinToString(" ") { word -> word.lowercase(PERU).replaceFirstChar { it.titlecase(PERU) } }

internal fun formatSoles(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    return "S/ " + DecimalFormat("#,##0.00", symbols).format(amount)
}

/** «ago 2026» for a receipt's billing month. */
internal fun billingMonthLabel(billingDate: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = billingDate }
    val month = MONTHS[calendar.get(Calendar.MONTH)].replaceFirstChar { it.titlecase(PERU) }
    return "$month ${calendar.get(Calendar.YEAR)}"
}

internal fun InstallationType.label(): String = when (this) {
    InstallationType.FIBER -> "Fibra"
    InstallationType.WIRELESS -> "Inalámbrico"
    InstallationType.ONLY_TV_FIBER -> "Solo TV"
}

internal fun ServiceStatus.badge(): StatusBadge = when (this) {
    ServiceStatus.ACTIVE -> StatusBadge("Activo", BadgeTone.SUCCESS)
    ServiceStatus.CUT_OFF -> StatusBadge("Cortado", BadgeTone.WARNING)
    ServiceStatus.CANCELLED -> StatusBadge("Cancelado", BadgeTone.NEUTRAL)
    ServiceStatus.UNKNOWN -> StatusBadge("Desconocido", BadgeTone.NEUTRAL)
}

data class HubHeader(
    val name: String,
    val subtitle: String,
    val status: StatusBadge,
    val debtBadge: StatusBadge?,
    val debtAmount: String?,
    val canCharge: Boolean,
) {
    companion object {
        fun from(subscription: SubscriptionResponse): HubHeader {
            val pending = subscription.pendingInvoiceQuantity
            val hasDebt = pending > 0 && subscription.totalDebt > 0
            return HubHeader(
                name = displayName(subscription.firstName, subscription.lastName),
                subtitle = listOfNotNull(
                    "Cód. ${subscription.id}",
                    subscription.installationType.label(),
                    subscription.place?.name?.toTitleCase(),
                ).joinToString(" · "),
                status = subscription.serviceStatus.badge(),
                debtBadge = if (hasDebt) StatusBadge(debtText(pending), BadgeTone.ERROR) else null,
                debtAmount = if (hasDebt) formatSoles(subscription.totalDebt) else null,
                canCharge = hasDebt,
            )
        }

        private fun debtText(months: Int) = if (months == 1) "Debe 1 mes" else "Debe $months meses"
    }
}

/** The charge being prepared in the sheet: which receipts, how they are paid and whether to restore. */
data class ChargeDraft(
    val receipts: List<Payment>,
    val selectedIds: Set<Int>,
    val methods: List<PaymentMethodOption>,
    val method: String,
    val payerName: String,
    val status: ServiceStatus,
    val restore: Boolean,
) {
    val selectedReceipts: List<Payment> get() = receipts.filter { it.id in selectedIds }
    val total: Double get() = selectedReceipts.sumOf { it.amountToPay }
    val pendingAfter: Int get() = receipts.size - selectedIds.size
    val canRestore: Boolean get() = status == ServiceStatus.CUT_OFF && pendingAfter <= MAX_PENDING_TO_RESTORE
    val requiresPayerName: Boolean get() = methods.any { it.name == method && it.requiresPayerName }

    val blockingReason: String?
        get() = when {
            selectedIds.isEmpty() -> "Elige al menos un recibo"
            method.isBlank() -> "Elige el método de pago"
            requiresPayerName && payerName.isBlank() -> "Escribe el nombre que aparece en $method"
            else -> null
        }

    fun toggle(receiptId: Int): ChargeDraft =
        copy(selectedIds = if (receiptId in selectedIds) selectedIds - receiptId else selectedIds + receiptId)

    companion object {
        fun start(receipts: List<Payment>, methods: List<PaymentMethodOption>, status: ServiceStatus): ChargeDraft {
            val unpaid = receipts.filter { !it.paid && it.id != null }.sortedBy { it.billingDate }
            val draft = ChargeDraft(
                receipts = unpaid,
                selectedIds = unpaid.mapNotNull { it.id }.toSet(),
                methods = methods,
                method = methods.firstOrNull { it.name == DEFAULT_METHOD }?.name ?: methods.firstOrNull()?.name.orEmpty(),
                payerName = "",
                status = status,
                restore = false,
            )
            return draft.copy(restore = draft.canRestore)
        }
    }
}
