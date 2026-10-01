package com.dscorp.ispadmin.data.repository

import android.content.SharedPreferences
import com.dscorp.ispadmin.domain.repository.OnuRegistrationSelectionStore

class SharedPreferencesOnuRegistrationSelectionStore(
    private val preferences: SharedPreferences,
) : OnuRegistrationSelectionStore {

    override fun getSelectedOnuSerial(operatorId: Long): String? =
        preferences.getString(key(operatorId), null)

    override fun saveSelectedOnuSerial(operatorId: Long, serial: String) {
        require(serial.isNotBlank()) { "ONU serial must not be blank" }
        check(preferences.edit().putString(key(operatorId), serial).commit()) {
            "Could not persist ONU registration selection"
        }
    }

    override fun clearSelectedOnuSerial(operatorId: Long) {
        check(preferences.edit().remove(key(operatorId)).commit()) {
            "Could not clear ONU registration selection"
        }
    }

    private fun key(operatorId: Long) = "onu_registration_selection_$operatorId"
}
