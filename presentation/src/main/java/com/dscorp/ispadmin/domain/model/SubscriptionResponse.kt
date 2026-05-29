package com.dscorp.ispadmin.domain.model

import com.dscorp.ispadmin.domain.model.extensions.toFormattedDateString
import retrofit2.http.Url
import java.util.Date

data class SubscriptionResponse(
    var id: Int,
    var address: String? = null,
    var dni: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var location: GeoLocation? = null,
    var napBox: NapBoxResponse? = null,
    var networkDevices: List<NetworkDevice>? = null,
    var new: Boolean? = null,
    var password: String? = null,
    var phone: String? = null,
    var place: Place? = null,
    var plan: PlanResponse? = null,
    var ip: String? = null,
    var serviceStatus: ServiceStatus,
    var technician: User? = null,
    var hostDevice: NetworkDevice? = null,
    var subscriptionDate: Long? = null,
    var isMigration: Boolean,
    var price: Double? = null,
    var paymentCommitmentDate: Long? = null,
    var isPaymentCommitment: Boolean = false,
    var lastCutOffDate: Date? = null,
    var isReactivation: Boolean = false,
    var reactivationDate: Long? = null,
    var cpeDeviceId: Int? = null,
    var note: String? = null,
    var installationType: InstallationType,
    var facadePhotoUrl: String? = null,
    val email: String?,
    val pendingInvoiceQuantity: Int,
    val antiquityInMonths: Int,
    val qualification: Any,
    val ics: Int,
    val totalDebt: Double,
    val lastPaymentDate: String?,
    val hasFiberOnu: Boolean = false,
) : java.io.Serializable {
    fun getFullName() = "$firstName $lastName"

    fun dateAsString() = subscriptionDate?.toFormattedDateString()
    fun toDomain() = SubscriptionResume(
        id = id,
        planName = plan?.name ?: "",
        customerName = getFullName(),
        antiquity = antiquityInMonths.toString(),
        qualification = qualification.toString(),
        placeName = place?.name ?: "",
        placeId = place?.id ?: "",
        ics = ics.toString(),
        lastPaymentDate = lastPaymentDate,
        pendingInvoicesQuantity = pendingInvoiceQuantity,
        totalDebt = totalDebt,
        ipAddress = ip ?: "",
        serviceStatus = serviceStatus,
        customer = CustomerData(
            subscriptionId = id,
            name = firstName ?: "",
            lastName = lastName ?: "",
            dni = dni ?: "",
            place = place?.name ?: "",
            address = address ?: "",
            phone = phone ?: "",
            email = email ?: "",
        ),
        installationType = installationType,
        napBox = napBox?.toDomain(),
        location = location!!,
        hasFiberOnu = hasFiberOnu,
    )
}


