package com.dscorp.ispadmin.data.remote

import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.OnuRegistrationCleanupReport
import com.dscorp.ispadmin.domain.model.OnuRegistrationOutcome
import com.dscorp.ispadmin.domain.repository.StartOnuRegistrationRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface OnuRegistrationOperationApi {
    @POST("onu-registration-operations")
    suspend fun start(@Body request: StartOnuRegistrationRequest): Response<OnuRegistrationOperation>

    @GET("onu-registration-operations/active")
    suspend fun active(): Response<OnuRegistrationOperation>

    @GET("onu-registration-operations/{operationId}")
    suspend fun get(@Path("operationId") operationId: String): Response<OnuRegistrationOperation>

    @GET("onu-registration-operations/{operationId}/outcome")
    suspend fun outcome(@Path("operationId") operationId: String): Response<OnuRegistrationOutcome>

    @GET("onu-registration-operations/{operationId}/draft")
    suspend fun draft(@Path("operationId") operationId: String): Response<Map<String, Any?>>

    @PUT("onu-registration-operations/{operationId}/draft")
    suspend fun saveDraft(
        @Path("operationId") operationId: String,
        @Body draft: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<Map<String, Any?>>

    @Multipart
    @POST("onu-registration-operations/{operationId}/photo")
    suspend fun uploadPhoto(
        @Path("operationId") operationId: String,
        @Part facadePhoto: MultipartBody.Part,
    ): Response<RegistrationPhotoResponseDto>

    @POST("onu-registration-operations/{operationId}/retry-acs")
    suspend fun retry(
        @Path("operationId") operationId: String,
        @Body request: RegistrationOperationRevisionDto,
    ): Response<OnuRegistrationOperation>

    @POST("onu-registration-operations/{operationId}/cancel")
    suspend fun cancel(
        @Path("operationId") operationId: String,
        @Body request: RegistrationOperationRevisionDto,
    ): Response<OnuRegistrationOperation>

    @POST("onu-registration-operations/{operationId}/cleanup")
    suspend fun cleanupCancelled(@Path("operationId") operationId: String): Response<OnuRegistrationCleanupReport>
}

data class RegistrationPhotoResponseDto(val url: String)
data class RegistrationOperationRevisionDto(val expectedRevision: Long)
