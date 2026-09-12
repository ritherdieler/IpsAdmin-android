package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.datasource.remote.AccessMigrationApiService
import com.dscorp.ispadmin.data.datasource.remote.AccessMigrationProgressDto
import com.dscorp.ispadmin.data.datasource.remote.toDomain
import com.dscorp.ispadmin.domain.model.AccessMigrationProgress
import com.dscorp.ispadmin.domain.repository.AccessMigrationRepository
import com.google.gson.JsonParser
import retrofit2.Response

class AccessMigrationRepositoryImpl(
    private val api: AccessMigrationApiService,
) : AccessMigrationRepository {

    override suspend fun startMigration(subscriptionId: Int): AccessMigrationProgress {
        val response = api.startAccessMigration(subscriptionId)
        return map(response, "No se pudo iniciar la migración a PPPoE")
    }

    override suspend fun getProgress(subscriptionId: Int): AccessMigrationProgress {
        val response = api.getAccessMigration(subscriptionId)
        return map(response, "No se pudo obtener el progreso de la migración")
    }

    private fun map(
        response: Response<AccessMigrationProgressDto>,
        fallback: String,
    ): AccessMigrationProgress {
        if (response.code() !in 200..299) {
            val body = response.errorBody()?.string()
            throw Exception(errorMessage(body, fallback))
        }
        return response.body()?.toDomain()
            ?: throw Exception("Respuesta vacía al consultar la migración a PPPoE")
    }

    private fun errorMessage(errorBody: String?, fallback: String): String {
        val body = errorBody?.takeIf { it.isNotBlank() } ?: return fallback
        return runCatching {
            val element = JsonParser.parseString(body)
            if (!element.isJsonObject) return@runCatching null
            val obj = element.asJsonObject
            sequenceOf("error", "message", "failureReason")
                .mapNotNull { key ->
                    obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString
                }
                .firstOrNull { it.isNotBlank() }
        }.getOrNull() ?: fallback
    }
}
