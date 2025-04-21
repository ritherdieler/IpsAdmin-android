package com.example.cleanarchitecture.domain.entity

data class IpPool(
    val id: Int?=null,
    val ipSegment: String? = null,
    val hostDeviceId:Int? = null,
):java.io.Serializable