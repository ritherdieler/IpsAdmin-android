package com.example.cleanarchitecture.domain.entity

data class NapBox(
    val id: String? = null,
    val code: String = "",
    val address: String = "",
    var latitude: Double? = null,
    var longitude: Double? = null,
    var threads: Int? = null,
    var mufaId: Int? = null,
    var ports_number: Int? = null,
    val placeName: String?=null,
    val placeId: Int?=null,

    ) : java.io.Serializable {
    override fun toString(): String {
        return "$id / $code"
    }

    override fun equals(other: Any?): Boolean {
        return id == (other as NapBox).id
    }

}



