package com.dscorp.ispadmin.data.apirequestmodel

import com.dscorp.ispadmin.domain.model.Onu

data class MigrationRequest(
    val onu: Onu?,
    val planId: String?,
    var subscriptionId: Int?,
    val price: String?,
    val notes: String?
) {
    fun isValid() = onu != null && planId != null && subscriptionId != null
}