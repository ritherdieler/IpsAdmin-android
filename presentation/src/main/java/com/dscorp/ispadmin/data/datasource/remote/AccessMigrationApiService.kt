package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.domain.model.AccessMigrationProgress
import com.dscorp.ispadmin.domain.model.AccessMigrationStage
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class AccessMigrationProgressDto(
    val subscriptionId: Int? = null,
    val stage: String? = null,
    val failureReason: String? = null,
    val quarantineUntil: String? = null,
    val pppoeUsername: String? = null,
    val attempt: Int? = null,
    val done: Boolean? = null,
    val message: String? = null,
)

data class AccessMigrationEligibleDto(
    val subscriptionId: Int? = null,
    val name: String? = null,
    val ip: String? = null,
    val onuSn: String? = null,
    val productClass: String? = null,
    val planName: String? = null,
    val reason: String? = null,
    val eligible: Boolean? = null,
)

data class AccessMigrationEligiblePageDto(
    val items: List<AccessMigrationEligibleDto> = emptyList(),
)

interface AccessMigrationApiService {
    @POST("subscription/{id}/access-migration")
    suspend fun startAccessMigration(
        @Path("id") subscriptionId: Int,
    ): Response<AccessMigrationProgressDto>

    @GET("subscription/{id}/access-migration")
    suspend fun getAccessMigration(
        @Path("id") subscriptionId: Int,
    ): Response<AccessMigrationProgressDto>

    @GET("subscription/access-migration/eligible")
    suspend fun getEligibleAccessMigrations(): Response<AccessMigrationEligiblePageDto>
}

fun AccessMigrationProgressDto.toDomain(): AccessMigrationProgress {
    val parsedStage = AccessMigrationStage.parse(stage)
    val terminal = parsedStage?.isTerminal() == true
    return AccessMigrationProgress(
        subscriptionId = subscriptionId ?: 0,
        stage = parsedStage,
        failureReason = failureReason,
        quarantineUntil = quarantineUntil,
        pppoeUsername = pppoeUsername,
        attempt = attempt,
        done = done ?: terminal,
        message = message,
    )
}
