package com.dscorp.ispadmin.data.datasource.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/** Core «Cambiar caja NAP»: boxes of the place with their taken ports, and a move to a chosen port. */
interface NapMoveApi {
    @GET("subscription/{id}/nap-options")
    suspend fun options(@Path("id") subscriptionId: Int): NapOptionsResponse

    @PUT("subscription/{id}/nap-box")
    suspend fun move(@Path("id") subscriptionId: Int, @Body request: NapMoveRequest): NapMoveResponse
}

data class NapOptionResponse(
    val id: Int = 0,
    val code: String = "",
    val address: String? = null,
    val mufaId: Int? = null,
    val distanceMeters: Int? = null,
    val totalPorts: Int = 16,
    val occupied: List<Int> = emptyList(),
)

data class NapOptionsResponse(
    val placeName: String? = null,
    val currentNapId: Int? = null,
    val currentPort: Int? = null,
    val options: List<NapOptionResponse> = emptyList(),
)

data class NapMoveRequest(val napBoxId: Int, val port: Int)
data class NapMoveResponse(val napCode: String = "", val port: Int? = null)
