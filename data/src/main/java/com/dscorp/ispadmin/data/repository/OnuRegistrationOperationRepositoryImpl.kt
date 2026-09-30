package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.remote.OnuRegistrationOperationApi
import com.dscorp.ispadmin.data.remote.RegistrationOperationRevisionDto
import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.OnuRegistrationCleanupReport
import com.dscorp.ispadmin.domain.repository.OnuRegistrationOperationRepository
import com.dscorp.ispadmin.domain.repository.StartOnuRegistrationRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import retrofit2.Response

class OnuRegistrationOperationRepositoryImpl(
    private val api: OnuRegistrationOperationApi,
) : OnuRegistrationOperationRepository {
    override suspend fun active(): OnuRegistrationOperation? {
        val response = api.active()
        if (response.code() == 404) return null
        check(response.isSuccessful) { "ACTIVE_OPERATION_REQUEST_FAILED (HTTP ${response.code()})" }
        return response.body()
    }

    override suspend fun start(request: StartOnuRegistrationRequest): OnuRegistrationOperation =
        api.start(request).requiredBody("ONU_PREAUTHORIZATION_FAILED")

    override suspend fun draft(operationId: String): Map<String, Any?>? {
        val response = api.draft(operationId)
        if (response.code() == 404) return null
        return response.requiredBody("REGISTRATION_DRAFT_LOAD_FAILED")
    }

    override suspend fun saveDraft(operationId: String, draft: Map<String, Any?>) {
        api.saveDraft(operationId, draft).requiredBody("REGISTRATION_DRAFT_SAVE_FAILED")
    }

    override suspend fun uploadPhoto(operationId: String, file: File): String {
        require(file.isFile && file.length() > 0) { "INVALID_FACADE_PHOTO" }
        val body = file.asRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("facadePhoto", file.name, body)
        return api.uploadPhoto(operationId, part).requiredBody("FACADE_PHOTO_UPLOAD_FAILED").url
    }

    override suspend fun retry(operationId: String, expectedRevision: Long): OnuRegistrationOperation =
        api.retry(operationId, RegistrationOperationRevisionDto(expectedRevision))
            .requiredBody("ONU_PREAUTHORIZATION_RETRY_FAILED")

    override suspend fun cancel(operationId: String, expectedRevision: Long): OnuRegistrationOperation =
        api.cancel(operationId, RegistrationOperationRevisionDto(expectedRevision))
            .requiredBody("ONU_PREAUTHORIZATION_CANCEL_FAILED")

    override suspend fun cleanupCancelled(operationId: String): OnuRegistrationCleanupReport {
        val response = api.cleanupCancelled(operationId)
        if (response.code() == 404) return OnuRegistrationCleanupReport("COMPLETE")
        return response.requiredBody("ONU_REGISTRATION_CLEANUP_FAILED")
    }

    override suspend fun get(operationId: String): OnuRegistrationOperation =
        api.get(operationId).requiredBody("ONU_REGISTRATION_OPERATION_LOAD_FAILED")

    private fun <T : Any> Response<T>.requiredBody(code: String): T {
        if (!isSuccessful) throw IllegalStateException("$code (HTTP ${this.code()})")
        return requireNotNull(body()) { "$code (empty response)" }
    }
}
