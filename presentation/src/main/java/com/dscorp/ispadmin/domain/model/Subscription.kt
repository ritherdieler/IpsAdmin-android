package com.dscorp.ispadmin.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Created by Sergio Carrillo Diestra on 30/11/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
data class Subscription(
    @SerializedName("id")
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
    var facadePhotoUrl: String? = null, //URL de la foto de fachada subida a Firebase Storage
    var borneNumber: String? = null,
    var equipmentCondition: EquipmentCondition? = null,
    var autoCut: Boolean = true,
    var clientRequestId: String? = null,
    var installationOrderId: Int? = null,
    var clientIpAddress: String? = null,
    var provisioningPending: Boolean = false,
    var mikrotikProvisionStatus: String? = null,
    var oltProvisionStatus: String? = null,
    var vlan: String? = "1",
    var wifiSsid24: String? = null,
    var wifiPassword24: String? = null,
    var wifiSsid5: String? = null,
    var wifiPassword5: String? = null,
    var tr069ProvisionStatus: String? = null,
    var tr069RequiresManualConfig: Boolean = false,
    var tr069Message: String? = null,
    var accessMode: String? = null,
    var pppoeUsername: String? = null,
    var accessMigrationStage: String? = null,

    ) : java.io.Serializable {
    fun resolvedSubscriptionId(): Int? = subscriptionId

    fun networkAccessLabel(): String =
        if (AccessMode.parse(accessMode)?.usesPppoe() == true) "Usuario PPPoE" else "IP"

    fun networkAccessValue(): String =
        if (AccessMode.parse(accessMode)?.usesPppoe() == true) {
            pppoeUsername?.trim()?.takeIf { it.isNotEmpty() } ?: "No asignado"
        } else {
            ip ?: "No asignada"
        }

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
