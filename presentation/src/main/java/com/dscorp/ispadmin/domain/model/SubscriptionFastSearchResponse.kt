package com.dscorp.ispadmin.domain.model

data class SubscriptionFastSearchResponse(
    var id: Int,
    var fullName: String,
){
    override fun toString(): String {
        return fullName
    }
}