package com.dscorp.ispadmin.data.network

import java.util.UUID

object ConfirmedActionHeaders {
    const val CONFIRM_HEADER = "X-Confirm-Action"
    const val CONFIRM_VALUE = "true"
    const val IDEMPOTENCY_HEADER = "Idempotency-Key"

    fun newIdempotencyKey(): String = UUID.randomUUID().toString()
}
