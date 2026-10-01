package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.OnuRegistrationCancellationIntent

interface OnuRegistrationCancellationIntentStore {
    suspend fun get(operatorId: Long): OnuRegistrationCancellationIntent?
    suspend fun save(intent: OnuRegistrationCancellationIntent)
    suspend fun clear(operatorId: Long, requestKey: String)
}
