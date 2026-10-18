package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.repository.PendingSubscriptionRepository
import com.dscorp.ispadmin.domain.storage.FacadePhotoStorage

class DiscardPendingSubscriptionUseCase(
    private val pendingSubscriptionRepository: PendingSubscriptionRepository,
    private val facadePhotoStorage: FacadePhotoStorage,
) {
    /** Drops a queued offline registration; its facade photo goes only once the row is gone. */
    suspend operator fun invoke(localId: String): Result<Unit> =
        pendingSubscriptionRepository.deleteByLocalId(localId).onSuccess { facadePhotoStorage.delete(localId) }
}
