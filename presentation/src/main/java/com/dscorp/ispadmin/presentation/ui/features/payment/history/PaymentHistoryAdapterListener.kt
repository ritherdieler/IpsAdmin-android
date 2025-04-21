package com.dscorp.ispadmin.presentation.ui.features.payment.history

import com.dscorp.ispadmin.domain.model.Payment

interface PaymentHistoryAdapterListener {

    fun onPaymentHistoryItemClicked(payment: Payment)
}
