package com.dscorp.ispadmin.domain.model

data class IpPool(
    val id: Int?=null,
    val ipSegment: String? = null,
    val hostDeviceId:Int? = null,
):java.io.Serializable