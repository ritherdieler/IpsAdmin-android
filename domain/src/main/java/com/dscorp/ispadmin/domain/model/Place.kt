package com.dscorp.ispadmin.domain.model

data class Place(
    val id: String? = null,
    val abbreviation: String = "",
    val name: String = "",
    val location: GeoLocation? = null,
) : java.io.Serializable {
    override fun toString(): String {
        return name.capitalize()
    }

    override fun equals(other: Any?): Boolean {
        return id == (other as Place).id
    }
}



