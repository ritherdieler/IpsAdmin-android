package com.example.cleanarchitecture.domain.entity

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Payment(
    var id: Int? = null,
    var amountPaid: Double? = null,
    val discountAmount: Double? = 0.0,
    val discountReason: String? = null,
    var paymentDate: Long = 0,
    var billingDate: Long = 0,
    var method: String? = "",
    var paid: Boolean = false,
    var amountToPay: Double = 0.0,
    var responsibleId: Int = 0,
    var service: String? = null,
    var responsibleName: String? = null,
    val subscriptionId: Int? = null,
    var electronicPayerName: String? = null,
) : java.io.Serializable {

    //first letter of month in uppercase


    private val format = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

    private val detailedFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es", "ES"))

    fun paymentDateStr(): String {
        val date = Date(paymentDate)
        return format.format(date).uppercase(Locale.ROOT)
    }

    fun detailPaymentDateStr(): String {
        val date = Date(paymentDate)
        return detailedFormat.format(date).uppercase(Locale.ROOT)
    }

    fun billingDateStr(): String {
        val date = Date(billingDate)
        return format.format(date).uppercase(Locale.ROOT)
    }

    fun amountToPayStr(): String {
        return "S/.$amountToPay"
    }

    fun paidStatusStr(): String {
        return if (paid) "Pagado" else "Pendiente"
    }

    fun discountAmountStr(): String {
        return when {
            discountAmount == null -> ""
            discountAmount > 0.0 -> "S/.${discountAmount}"
            else -> ""
        }
    }
}
