package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.remote.ProvisioningApi
import com.dscorp.ispadmin.data.remote.ProvisioningActionDto
import com.dscorp.ispadmin.data.remote.ProvisioningProgressDto
import com.dscorp.ispadmin.domain.model.ProvisioningAction
import com.dscorp.ispadmin.domain.model.ProvisioningProgress
import com.dscorp.ispadmin.domain.model.ProvisioningOperation
import com.dscorp.ispadmin.domain.model.ProvisioningCheckpoint
import com.dscorp.ispadmin.domain.model.ProvisioningFailure
import com.dscorp.ispadmin.domain.repository.ProvisioningRepository
import kotlinx.coroutines.CancellationException
import retrofit2.Response

class ProvisioningRepositoryImpl(private val api: ProvisioningApi) : ProvisioningRepository {
    override suspend fun latest(subscriptionId: Int): Result<ProvisioningProgress?> = request {
        val response = api.latest(subscriptionId)
        if (response.code() == 404) null else response.progress()
    }

    override suspend fun retry(action: ProvisioningAction): Result<ProvisioningProgress> = request {
        api.retry(action.subscriptionId, ProvisioningActionDto(action.operationId, action.expectedRevision)).progress()
    }

    override suspend fun cancel(action: ProvisioningAction): Result<ProvisioningProgress> = request {
        api.cancel(action.subscriptionId, ProvisioningActionDto(action.operationId, action.expectedRevision)).progress()
    }

    private suspend fun <T> request(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        Result.failure(IllegalStateException("No se pudo confirmar el estado del registro"))
    }

    private fun Response<ProvisioningProgressDto>.progress(): ProvisioningProgress {
        check(isSuccessful) { "PROVISIONING_HTTP_FAILED" }
        val dto = requireNotNull(body()) { "EMPTY_PROVISIONING_RESPONSE" }
        val operation = dto.operation
        return ProvisioningProgress(
            operation = ProvisioningOperation(id = operation.id, subscriptionId = operation.subscriptionId,
                revision = operation.revision, state = operation.state,
                checkpoints = operation.checkpoints.map { step -> ProvisioningCheckpoint(
                    stage = step.stage, state = step.state, attempts = step.attempts,
                    failure = step.failure?.let { ProvisioningFailure(code = it.code, message = it.message, retryable = it.retryable) },
                ) }),
            canRetry = dto.canRetry, canCancel = dto.canCancel,
            canRetryCancellation = dto.canRetryCancellation, canStartAgain = dto.canStartAgain,
        )
    }
}
