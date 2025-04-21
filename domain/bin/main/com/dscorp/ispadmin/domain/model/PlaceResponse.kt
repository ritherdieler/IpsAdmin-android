package com.dscorp.ispadmin.domain.model

data class PlaceResponse(
    val id: String? = null,
    val name: String? = null ,
    val latitude: Float? = null,
    val longitude: Float? = null,
):java.io.Serializable
{
    override fun toString(): String {
        return name!!.capitalize()
    }
}
