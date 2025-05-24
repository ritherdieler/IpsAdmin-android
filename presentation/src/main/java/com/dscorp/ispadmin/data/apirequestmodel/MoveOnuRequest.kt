package com.dscorp.ispadmin.data.apirequestmodel

data class MoveOnuRequest(
    val subscriptionId: Int,
    val newNapBoxId: Int,
)