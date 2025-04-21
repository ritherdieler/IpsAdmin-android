package com.example.cleanarchitecture.domain.entity

/**
 * Created by Sergio Carrillo Diestra on 30/11/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
data class Subscription(
    var subscriptionId: Int? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var dni: String? = null,
    var address: String? = null,
    var phone: String? = null,
    var subscriptionDate: Long? = null,
    var isNew: Boolean? = false,
    var serviceIsSuspended: Boolean? = false,
    var planId: String? = null,
    var additionalDeviceIds: List<Int> = emptyList(),
    var placeId: String? = null,
    var location: GeoLocation? = null,
    var technicianId: Int? = null,
    var napBoxId: String? = null,
    var hostDeviceId: Int? = null,
    var cpeDeviceId: Int? = null,
    var onu: Onu? = null,
    var ip: String? = null,
    var installationType: InstallationType? = null,
    var price: Double? = null,
    var coupon: String? = null,
    var isMigration: Boolean? = false,
    var note: String? = null,

    ) : java.io.Serializable {
    override fun toString(): String {
        return firstName ?: ""
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Subscription) {
            other.subscriptionId == subscriptionId
        } else {
            false
        }
    }


}
