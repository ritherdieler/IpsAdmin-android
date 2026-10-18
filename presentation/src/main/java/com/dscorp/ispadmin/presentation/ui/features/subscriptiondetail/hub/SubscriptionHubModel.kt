package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.PaymentMethodOption
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.planLabel
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
    /** Cash handed over by the client; blank means the exact amount. */
    val received: String = "",
    val discountOpen: Boolean = false,
    val discount: String = "",
    val discountReason: String? = null,
    /** Method of the previous charge on this phone, badged «Último usado». */
    val lastUsed: String? = null,
) {
    val selectedReceipts: List<Payment> get() = receipts.filter { it.id in selectedIds }
    val total: Double get() = selectedReceipts.sumOf { it.amountToPay }
    val discountValue: Double get() = if (discountOpen) discount.toMoney() ?: 0.0 else 0.0
    val toPay: Double get() = (total - discountValue).coerceAtLeast(0.0)
    val isCash: Boolean get() = method.equals(CASH_METHOD, ignoreCase = true)
    private val receivedValue: Double? get() = if (isCash) received.toMoney() else null
    val change: Double? get() = receivedValue?.let { it - toPay }?.takeIf { it >= -MONEY_EPSILON }?.coerceAtLeast(0.0)
    val pendingAfter: Int get() = receipts.size - selectedIds.size
    val canRestore: Boolean get() = status == ServiceStatus.CUT_OFF && pendingAfter <= MAX_PENDING_TO_RESTORE
    val requiresPayerName: Boolean get() = methods.any { it.name == method && it.requiresPayerName }

    val blockingReason: String?
        get() = when {
            selectedIds.isEmpty() -> "Elige al menos un recibo"
            method.isBlank() -> "Elige el método de pago"
            requiresPayerName && payerName.isBlank() -> "Escribe el nombre que aparece en $method"
            discountValue > total + MONEY_EPSILON -> "El descuento no puede ser mayor al total"
            discountValue > 0 && discountReason == null -> "Elige el motivo del descuento"
            receivedValue != null && receivedValue!! < toPay - MONEY_EPSILON -> "Lo recibido no alcanza para el total"
            else -> null
        }

    fun toggle(receiptId: Int): ChargeDraft =
        copy(selectedIds = if (receiptId in selectedIds) selectedIds - receiptId else selectedIds + receiptId)

    companion object {
        fun start(
            receipts: List<Payment>,
            methods: List<PaymentMethodOption>,
            status: ServiceStatus,
            lastUsed: String? = null,
        ): ChargeDraft {
            val unpaid = receipts.filter { !it.paid && it.id != null }.sortedBy { it.billingDate }
            val remembered = methods.firstOrNull { it.name == lastUsed }?.name
            val draft = ChargeDraft(
                receipts = unpaid,
                selectedIds = unpaid.mapNotNull { it.id }.toSet(),
                methods = methods,
                method = remembered ?: methods.firstOrNull { it.name == DEFAULT_METHOD }?.name ?: methods.firstOrNull()?.name.orEmpty(),
                payerName = "",
                status = status,
                restore = false,
                lastUsed = remembered,
            )
            return draft.copy(restore = draft.canRestore)
        }
    }
}

val DISCOUNT_REASONS = listOf("Promoción", "Reclamo", "Error de cobro", "Otro")

private const val CASH_METHOD = "Efectivo"
private const val MONEY_EPSILON = 0.005

private fun String.toMoney(): Double? = trim().replace(",", ".").toDoubleOrNull()?.takeIf { it >= 0 }

/** Due state of an unpaid receipt, from the Core due date. */
internal fun receiptStatus(receipt: Payment, today: java.time.LocalDate): StatusBadge? {
    val due = receipt.dueDate?.take(10)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: return null
    return when {
        due.isBefore(today) -> StatusBadge("Vencido", BadgeTone.ERROR)
        due == today -> StatusBadge("Vence hoy", BadgeTone.WARNING)
        due == today.plusDays(1) -> StatusBadge("Vence mañana", BadgeTone.WARNING)
        else -> StatusBadge("Pendiente", BadgeTone.NEUTRAL)
    }
}

/** What the Core receives in PUT payment/batch: all receipts in one transaction. */
data class ChargeBatch(
    val paymentIds: List<Int>,
    val method: String,
    val discountAmount: Double,
    val discountReason: String?,
    val electronicPayerName: String?,
)

/** a-caj-04: confirmation shown after a charge. */
data class ChargeReceipt(
    val number: String,
    val holder: String,
    val months: String,
    val method: String,
    val amount: String,
    val collector: String,
    val date: String,
    val restored: Boolean?,
)

/** «Cód. 1842 · Duo basico 80 · S/ 80.00 al mes» under the charge title. */
internal fun chargeSubtitle(subscription: SubscriptionResponse): String = listOfNotNull(
    "Cód. ${subscription.id}",
    subscription.plan?.name?.takeIf { it.isNotBlank() }?.let(::planLabel),
    subscription.plan?.price?.let { "${formatSoles(it)} al mes" },
).joinToString(" · ")

/** Plain text the cashier sends to the client from the receipt. */
internal fun ChargeReceipt.shareText(): String = buildString {
    appendLine("Comprobante de pago $number")
    appendLine("Titular: $holder")
    if (months.isNotBlank()) appendLine("Recibos: $months")
    appendLine("Método: $method")
    appendLine("Monto: $amount")
    append("Fecha: $date")
    if (restored == true) append("\nServicio restablecido")
}
