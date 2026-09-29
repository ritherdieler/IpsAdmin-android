package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.repository.OnuRegistrationOperationRepository
import com.dscorp.ispadmin.domain.repository.StartOnuRegistrationRequest
import java.io.File

class OnuRegistrationOperationUseCase(private val repository: OnuRegistrationOperationRepository) {
    suspend fun active(): OnuRegistrationOperation? = repository.active()
    suspend fun start(request: StartOnuRegistrationRequest): OnuRegistrationOperation = repository.start(request)
    suspend fun draft(operationId: String): Map<String, Any?>? = repository.draft(operationId)
    suspend fun saveDraft(operationId: String, draft: Map<String, Any?>) = repository.saveDraft(operationId, draft)
    suspend fun uploadPhoto(operationId: String, file: File): String = repository.uploadPhoto(operationId, file)
    suspend fun retry(operationId: String, revision: Long): OnuRegistrationOperation = repository.retry(operationId, revision)
    suspend fun cancel(operationId: String, revision: Long): OnuRegistrationOperation = repository.cancel(operationId, revision)
    suspend fun get(operationId: String): OnuRegistrationOperation = repository.get(operationId)
}
