package com.example.cleanarchitecture.domain.entity

data class NapBoxResponse(
    val id: String? = null,
    val code: String = "",
    val address: String = "",
    val mufaId: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val ports_number: Int? = null,
    val placeName: String,
    val placeId: Int,
):java.io.Serializable
{
    override fun toString(): String {
        return "$code / ${address.capitalize()}"
    }

    fun toDomain(): NapBox {
        return NapBox(
            id = id ?: "",
            code = code,
            address = address,
            mufaId = mufaId ?: 0,
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            ports_number = ports_number ?: 0,
            placeName = placeName,
            placeId = placeId
        )
    }
}