package com.dscorp.ispadmin.presentation.ui.features.place

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.Place
import com.example.data2.data.repository.IRepository
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent

class PlaceViewModel : ViewModel() {
    private val repository: IRepository by KoinJavaComponent.inject(IRepository::class.java)

    val placeResponseLiveData = MutableLiveData<PlaceResponse>()
    val formErrorLiveData = MutableLiveData<FormError>()
    val cleanErrorFormLiveData = MutableLiveData<CleanFormErrorsPlace>()
    fun registerPlace(place: Place) = viewModelScope.launch {

        try {
            if (formIsValid(place)) {
                val placeFromRepository = repository.registerPlace(place)
                placeResponseLiveData.postValue(PlaceResponse.OnPlaceRegister(placeFromRepository))
            }
        } catch (error: Exception) {
            placeResponseLiveData.postValue(PlaceResponse.OnError(error))
        }
    }

    private fun formIsValid(place: Place): Boolean {
        if (place.name.isEmpty()) {
            formErrorLiveData.value = FormError.OnEtNameError()
            return false
        } else {
            cleanErrorFormLiveData.value = CleanFormErrorsPlace.OnEtNamePlaceCleanError
        }

        if (place.location == null) {
            formErrorLiveData.value = FormError.OnEtLocationError()
            return false
        }
        if (place.abbreviation.isEmpty()) {
            formErrorLiveData.value = FormError.OnEtAbbreviationError()
            return false
        } else {
            cleanErrorFormLiveData.value = CleanFormErrorsPlace.OnEtAbbreviationCleanError
        }

        return true
    }
}
