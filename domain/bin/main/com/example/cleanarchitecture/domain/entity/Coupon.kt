package com.example.cleanarchitecture.domain.entity


data class Coupon(
    var id: Int? = null,
    var code: String? = null,
    var description: String? = null,
    var discount: Int? = null,
    var expirationDate: Long? = null,
    var isUsed: Boolean = false,
)