package com.dscorp.ispadmin.domain.repository

interface OnuRegistrationSelectionStore {
    fun getSelectedOnuSerial(operatorId: Long): String?
    fun saveSelectedOnuSerial(operatorId: Long, serial: String)
    fun clearSelectedOnuSerial(operatorId: Long)
}
