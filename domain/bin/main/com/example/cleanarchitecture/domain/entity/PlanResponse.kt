package com.example.cleanarchitecture.domain.entity

data class PlanResponse(
    var id: String? = null,
    var name: String? = null,
    var price: Double? = null,
    var downloadSpeed: String? = null,
    var uploadSpeed: String? = null,
    var type: InstallationType
) : java.io.Serializable {
    override fun toString(): String {
        return "${name?.capitalize()} / Precio: ${priceToString()}"
    }

    override fun equals(other: Any?): Boolean {
        return id == (other as PlanResponse).id
    }

    fun getPlanType() = when (type) {
        InstallationType.WIRELESS -> "W"
        InstallationType.FIBER -> "F"
        InstallationType.ONLY_TV_FIBER -> "TV"
    }

    fun priceToString(): String {
        return "S/$price"
    }

    fun downloadSpeedToString(): String {
        return "${downloadSpeed}Mbps"
    }

    fun uploadSpeedToString(): String {
        return "${uploadSpeed}Mbps"
    }


}
