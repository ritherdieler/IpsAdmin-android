package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.ProvisioningAction
import com.dscorp.ispadmin.domain.model.ProvisioningProgress

interface ProvisioningRepository {
    suspend fun latest(subscriptionId: Int): Result<ProvisioningProgress?>
    suspend fun retry(action: ProvisioningAction): Result<ProvisioningProgress>
    suspend fun cancel(action: ProvisioningAction): Result<ProvisioningProgress>
}
