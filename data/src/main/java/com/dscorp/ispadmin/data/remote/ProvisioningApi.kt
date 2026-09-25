package com.dscorp.ispadmin.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ProvisioningApi {
    @GET("subscription/{id}/provisioning")
    suspend fun latest(@Path("id") subscriptionId: Int): Response<ProvisioningProgressDto>
    @POST("subscription/{id}/provisioning/retry")
    suspend fun retry(@Path("id") subscriptionId: Int, @Body action: ProvisioningActionDto): Response<ProvisioningProgressDto>
    @POST("subscription/{id}/provisioning/cancel")
    suspend fun cancel(@Path("id") subscriptionId: Int, @Body action: ProvisioningActionDto): Response<ProvisioningProgressDto>
}

data class ProvisioningActionDto(val operationId: String, val expectedRevision: Long)
data class ProvisioningProgressDto(
    val operation: ProvisioningOperationDto,
    val canRetry: Boolean,
    val canCancel: Boolean,
    val canRetryCancellation: Boolean,
    val canStartAgain: Boolean,
)
data class ProvisioningOperationDto(
    val id: String,
    val subscriptionId: Int,
    val revision: Long,
    val state: String,
    val checkpoints: List<ProvisioningCheckpointDto>,
)
data class ProvisioningCheckpointDto(val stage: String, val state: String, val attempts: Int, val failure: ProvisioningFailureDto?)
data class ProvisioningFailureDto(val code: String, val message: String, val retryable: Boolean)
