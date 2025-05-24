package com.dscorp.ispadmin.data.apirequestmodel

data class SearchPaymentsRequest (
    var subscriptionId: Int?= null,
    var startDate: Long? = null,
    var endDate: Long? = null
)