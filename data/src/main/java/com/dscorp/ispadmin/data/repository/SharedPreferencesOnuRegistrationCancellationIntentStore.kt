package com.dscorp.ispadmin.data.repository

import android.content.SharedPreferences
import com.dscorp.ispadmin.domain.model.OnuRegistrationCancellationIntent
import com.dscorp.ispadmin.domain.repository.OnuRegistrationCancellationIntentStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SharedPreferencesOnuRegistrationCancellationIntentStore(
    private val preferences: SharedPreferences,
) : OnuRegistrationCancellationIntentStore {

    override suspend fun get(operatorId: Long): OnuRegistrationCancellationIntent? = withContext(Dispatchers.IO) {
        val prefix = keyPrefix(operatorId)
        val requestKey = preferences.getString("${prefix}_request_key", null)
        val serial = preferences.getString("${prefix}_serial", null)
        if (requestKey.isNullOrBlank() || serial.isNullOrBlank()) return@withContext null
        OnuRegistrationCancellationIntent(
            operatorId = operatorId,
            requestKey = requestKey,
            serial = serial,
            operationId = preferences.getString("${prefix}_operation_id", null),
        )
    }

    override suspend fun save(intent: OnuRegistrationCancellationIntent) = withContext(Dispatchers.IO) {
        require(intent.requestKey.isNotBlank()) { "Cancellation request key must not be blank" }
        require(intent.serial.isNotBlank()) { "Cancellation serial must not be blank" }
        val prefix = keyPrefix(intent.operatorId)
        val editor = preferences.edit()
            .putString("${prefix}_request_key", intent.requestKey)
            .putString("${prefix}_serial", intent.serial)
        if (intent.operationId.isNullOrBlank()) {
            editor.remove("${prefix}_operation_id")
        } else {
            editor.putString("${prefix}_operation_id", intent.operationId)
        }
        check(editor.commit()) { "Could not persist ONU registration cancellation intent" }
    }

    override suspend fun clear(operatorId: Long, requestKey: String) = withContext(Dispatchers.IO) {
        val prefix = keyPrefix(operatorId)
        if (preferences.getString("${prefix}_request_key", null) == requestKey) {
            check(
                preferences.edit()
                    .remove("${prefix}_request_key")
                    .remove("${prefix}_serial")
                    .remove("${prefix}_operation_id")
                    .commit()
            ) { "Could not clear ONU registration cancellation intent" }
        }
    }

    private fun keyPrefix(operatorId: Long) = "onu_registration_cancellation_$operatorId"
}
