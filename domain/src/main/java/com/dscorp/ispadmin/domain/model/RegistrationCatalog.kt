package com.dscorp.ispadmin.domain.model

data class CatalogPlan(
    val id: String,
    val name: String? = null,
    val price: Double? = null,
    val downloadSpeed: String? = null,
    val uploadSpeed: String? = null,
    val type: String? = null
)

data class CatalogPlace(
    val id: String,
    val name: String? = null,
    val latitude: Float? = null,
    val longitude: Float? = null
)

data class CatalogNapBox(
    val id: String,
    val code: String = "",
    val address: String = "",
    val mufaId: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val portsNumber: Int? = null,
    val placeName: String = "",
    val placeId: Int = 0
)

data class CatalogOnu(
    val sn: String,
    val board: String = "",
    val oltId: String = "",
    val onu: String = "",
    val onuTypeId: String = "",
    val onuTypeName: String = "",
    val ponType: String = "",
    val port: String = ""
)

data class CatalogCoreDevice(
    val id: Int,
    val name: String,
    val password: String = "",
    val username: String = "",
    val ipAddress: String = "",
    val networkDeviceType: String? = null,
    val disabled: Boolean = false
)

data class RegistrationCatalog(
    val plans: List<CatalogPlan>,
    val places: List<CatalogPlace>,
    val napBoxes: List<CatalogNapBox>,
    val onus: List<CatalogOnu>,
    val coreDevices: List<CatalogCoreDevice>
)
