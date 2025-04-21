package com.dscorp.ispadmin.domain.model

data class NetworkDeviceResponse(

    val id: Int? = null,
    val name: String?=null,
    val password: String?= null,
    val username: String?=null,
    val ipAddress: String?=null,
    val networkDeviceType: String? = null,
)
{
    override fun toString(): String {
        return name!!
    }
}
