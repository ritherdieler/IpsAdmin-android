package com.dscorp.ispadmin.data.datasource.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Core network map of a place and NAP box detail (read only). */
interface NetworkMapApi {
    @GET("napbox/map")
    suspend fun map(@Query("placeId") placeId: Int?): NapMapResponse

    @GET("napbox/{id}/detail")
    suspend fun detail(@Path("id") napId: Int): NapDetailResponse
}

data class NapMapPlaceResponse(val id: Int = 0, val name: String = "", val boxes: Int = 0)
data class NapMapBoxResponse(
    val id: Int = 0,
    val code: String = "",
    val address: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val mufaId: Int? = null,
    val totalPorts: Int = 16,
    val taken: Int = 0,
)
data class NapMapMufaResponse(val id: Int = 0, val reference: String? = null, val latitude: Double = 0.0, val longitude: Double = 0.0, val threads: Int? = null)
data class NapMapResponse(
    val placeId: Int? = null,
    val placeName: String? = null,
    val places: List<NapMapPlaceResponse> = emptyList(),
    val boxes: List<NapMapBoxResponse> = emptyList(),
    val mufas: List<NapMapMufaResponse> = emptyList(),
)
data class NapSubscriberResponse(
    val subscriptionId: Int = 0,
    val name: String = "",
    val port: Int? = null,
    val plan: String? = null,
    val status: String = "",
    /** Null when the session may not see debts. */
    val debt: Double? = null,
)
data class NapDetailResponse(
    val id: Int = 0,
    val code: String = "",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String? = null,
    val mufa: String? = null,
    val mufaThreads: Int? = null,
    val oltId: Int? = null,
    val oltSlot: String? = null,
    val totalPorts: Int = 16,
    val subscribers: List<NapSubscriberResponse> = emptyList(),
)
