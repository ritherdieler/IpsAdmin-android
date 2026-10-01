package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.PendingSubscription
import com.dscorp.ispadmin.domain.model.RegistrationConflictException
import com.dscorp.ispadmin.domain.model.Subscription
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException

sealed interface RegistrationSubmission {
    data class Accepted(val subscription: Subscription, val reconciled: Boolean = false) : RegistrationSubmission
    data class Queued(val pending: PendingSubscription) : RegistrationSubmission
    data class Uncertain(val error: Throwable) : RegistrationSubmission
    data class Rejected(val error: Throwable) : RegistrationSubmission
}

class SubmitAndTrackRegistrationUseCase(
    private val registerSubscriptionUseCase: RegisterSubscriptionUseCase,
    private val onuRegistrationOperationUseCase: OnuRegistrationOperationUseCase?,
) {
    suspend fun submit(
        subscription: Subscription,
        orderId: Int?,
        facadePhotoFile: File?,
        onReconciling: () -> Unit = {},
    ): RegistrationSubmission = registerSubscriptionUseCase(subscription, orderId, facadePhotoFile).fold(
        onSuccess = { outcome ->
            when (outcome) {
                is RegisterSubscriptionResult.Registered -> RegistrationSubmission.Accepted(outcome.subscription)
                is RegisterSubscriptionResult.QueuedOffline -> RegistrationSubmission.Queued(outcome.pending)
            }
        },
        onFailure = { error ->
            if (!isUncertain(error)) return@fold RegistrationSubmission.Rejected(error)
            reconcile(subscription, onReconciling) ?: RegistrationSubmission.Uncertain(error)
        },
    )

    private suspend fun reconcile(subscription: Subscription, onReconciling: () -> Unit): RegistrationSubmission? {
        val operationId = subscription.registrationOperationId?.takeIf { it.isNotBlank() } ?: return null
        val useCase = onuRegistrationOperationUseCase ?: return null
        onReconciling()
        val outcome = try {
            useCase.outcome(operationId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val subscriptionId = outcome?.subscriptionId ?: return null
        return RegistrationSubmission.Accepted(
            subscription = subscription.copy(subscriptionId = subscriptionId, provisioningPending = true),
            reconciled = true,
        )
    }

    private fun isUncertain(error: Throwable): Boolean =
        generateSequence(error) { it.cause }.any { it is IOException } ||
            (error as? RegistrationConflictException)?.errorCode == REGISTRATION_ALREADY_EXISTS

    private companion object {
        const val REGISTRATION_ALREADY_EXISTS = "REGISTRATION_ALREADY_EXISTS"
    }
}
