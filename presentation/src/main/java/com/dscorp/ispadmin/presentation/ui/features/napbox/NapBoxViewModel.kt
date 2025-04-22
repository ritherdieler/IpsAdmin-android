package com.dscorp.ispadmin.presentation.ui.features.napbox

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.ui.features.napbox.edit.EditNapBoxFormErrorUiState
import com.dscorp.ispadmin.presentation.ui.features.napbox.edit.EditNapBoxUiState
import com.dscorp.ispadmin.presentation.ui.features.napbox.register.RegisterNapBoxUiState
import com.dscorp.ispadmin.presentation.ui.features.formvalidation.FieldValidator
import com.dscorp.ispadmin.presentation.ui.features.formvalidation.FormField
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.Mufa
import com.dscorp.ispadmin.domain.model.NapBox
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.example.data2.data.repository.IRepository
import kotlinx.coroutines.launch

class NapBoxViewModel(val repository :IRepository) : ViewModel() {

    val editNapBoxUiState = MutableLiveData<EditNapBoxUiState>()
    val editFormErrorLiveData = MutableLiveData<EditNapBoxFormErrorUiState>()

    val uiState = MutableLiveData<RegisterNapBoxUiState>()

    var napBoxResponse: NapBoxResponse? = null

    val mufaField = FormField(
        hintResourceId = R.string.selectAMufa,
        errorResourceId = R.string.mustSelectMufa,
        fieldValidator = object : FieldValidator<Mufa?> {
            override fun validate(fieldValue: Mufa?) = fieldValue != null
        }
    )

    val codeField = FormField(
        hintResourceId = R.string.enterACode,
        errorResourceId = R.string.mustDigitACode,
        fieldValidator = object : FieldValidator<String> {
            override fun validate(fieldValue: String?) = !fieldValue.isNullOrEmpty()
        }
    )

    val addressField = FormField(
        hintResourceId = R.string.enterAddress,
        errorResourceId = R.string.mustDigitAddress,
        fieldValidator = object : FieldValidator<String?> {
            override fun validate(fieldValue: String?) = !fieldValue.isNullOrEmpty()
        }
    )

    val locationField = FormField(
        hintResourceId = R.string.selectALocation,
        errorResourceId = R.string.mustSelectLocation,
        fieldValidator = object : FieldValidator<GeoLocation?> {
            override fun validate(fieldValue: GeoLocation?) = fieldValue != null
        }
    )


    fun registerNapBox() = viewModelScope.launch {
        try {
            if (!formIsValid()) return@launch
            val registerNapBox = NapBox(
                code = codeField.value!!,
                address = addressField.value!!,
                latitude = locationField.value?.latitude!!,
                longitude= locationField.value?.longitude!!,
                mufaId = mufaField.value!!.id,
                placeName = "placeName",
                placeId = -1
            )
            val response = repository.registerNapBox(registerNapBox)
            uiState.postValue(RegisterNapBoxUiState.OnRegisterNapBoxSealedClassRegister(response))
        } catch (error: Exception) {
            uiState.postValue(RegisterNapBoxUiState.OnError(error))
        }
    }

    fun editNapBox(napBox: NapBox) = viewModelScope.launch {
        try {
            if (!editFormIsValid(napBox)) return@launch
            val response = repository.editNapBox(napBox)
            editNapBoxUiState.postValue(
                EditNapBoxUiState.EditNapBoxSuccess(
                    response
                )
            )
        } catch (e: Exception) {
            editNapBoxUiState.postValue(EditNapBoxUiState.EditNapBoxError(e.message))
        }
    }

    private fun formIsValid(): Boolean {

        val fields = listOf(mufaField, codeField, addressField, locationField)
        for (field in fields) {
            field.isValid
        }
        return fields.all { it.isValid == true }
    }

    private fun editFormIsValid(napBox: NapBox): Boolean {

        if (napBox.code.isEmpty()) {
            editFormErrorLiveData.value = EditNapBoxFormErrorUiState.OnEtCodeError()
            return false
        } else {
            editFormErrorLiveData.value = EditNapBoxFormErrorUiState.OnEtCodeCleanError
        }
        if (napBox.address.isEmpty()) {
            editFormErrorLiveData.value = EditNapBoxFormErrorUiState.OnEtAddressError()
            return false
        } else {
            editFormErrorLiveData.value = EditNapBoxFormErrorUiState.OnEtAddressCleanError
        }
        if (napBox.latitude == null) {
            editFormErrorLiveData.value = EditNapBoxFormErrorUiState.OnEtLocationError()
            return false
        }
        if (napBox.longitude == null) {
            editFormErrorLiveData.value = EditNapBoxFormErrorUiState.OnEtLocationError()
            return false
        }
        else {
            editFormErrorLiveData.value = EditNapBoxFormErrorUiState.OnEtLocationCleanError
        }

        return true
    }

    fun getMufas() = viewModelScope.launch {
        try {
            val response = repository.getMufas()
            uiState.postValue(RegisterNapBoxUiState.MufasReady(response))
        } catch (e: Exception) {
            uiState.postValue(RegisterNapBoxUiState.OnError(e))
            e.printStackTrace()
        }
    }
}
