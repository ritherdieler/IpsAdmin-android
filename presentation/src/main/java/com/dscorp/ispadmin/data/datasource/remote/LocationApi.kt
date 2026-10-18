package com.dscorp.ispadmin.data.datasource.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** Core place check for a point (GET smart-map/coverage-check). */
interface LocationApi {
    @GET("smart-map/coverage-check")
    suspend fun coverage(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("assignedPlace") assignedPlace: String?,
    ): CoverageResponse
}

data class CoverageResponse(
    val hasCoverage: Boolean = false,
    val detectedPlaceName: String? = null,
    val assignedPlaceName: String? = null,
    val matchesAssignedPlace: Boolean? = null,
    val message: String = "",
)
