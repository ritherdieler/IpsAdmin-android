package com.example.cleanarchitecture.domain.entity

/**
 * Created by Sergio Carrillo Diestra on 24/11/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
data class NetworkDevice(
    val id: Int? = null,
    val name: String = "",
    val password: String = "",
    val username: String = "",
    val ipAddress: String = "",
    val networkDeviceType: NetworkDeviceType? = null,

    ) : java.io.Serializable {
    override fun toString(): String {
        return name
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return id == (other as NetworkDevice).id

    }

    enum class NetworkDeviceType {
        FIBER_ROUTER, CLOUD_CORE_ROUTER, WIRELESS_ROUTER, GENERIC
    }

}

