package com.dscorp.ispadmin.presentation.ui.features.networkdevice

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.presentation.ui.features.networkdevice.NetworkDeviceFormError.*
import com.dscorp.ispadmin.presentation.ui.features.networkdevice.NetworkDeviceResponse.*
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.extensions.isValidIpv4
import com.example.data2.data.repository.IRepository
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent

class NetworkDeviceViewModel() : ViewModel() {

    private val repository: IRepository by KoinJavaComponent.inject(IRepository::class.java)

    val networkDeviceResponseLiveData = MutableLiveData<NetworkDeviceResponse>()
    val networkDeviceFormErrorLiveData = MutableLiveData<NetworkDeviceFormError>()
    val cleanNetworkDeviceErrorFormLiveData = MutableLiveData<CleanFormErrors>()

    init {
        getFormData()
    }

    private fun getFormData() = viewModelScope.launch {
        try {
            val networkDeviceTypes = repository.getNetworkDeviceTypes()
            networkDeviceResponseLiveData.postValue(OnNetworkDeviceTypesReceived(networkDeviceTypes))
        } catch (error: Exception) {
            networkDeviceResponseLiveData.postValue(OnError(error))
        }
    }

    fun registerNetworkDevice(networkDevice: NetworkDevice) = viewModelScope.launch {
        try {
            if (!formIsValid(networkDevice)) return@launch
            val networkDeviceFromRepository = repository.registerNetworkDevice(networkDevice)
            networkDeviceResponseLiveData.postValue(
                OnNetworkDeviceRegistered(
                    networkDeviceFromRepository
                )
            )
        } catch (error: Exception) {
            networkDeviceResponseLiveData.postValue(OnError(error))
        }
    }

    private fun formIsValid(networkDevice: NetworkDevice): Boolean {

        if (networkDevice.name.isEmpty()) {
            networkDeviceFormErrorLiveData.value = OnEtNameError()
            return false
        } else {
            cleanNetworkDeviceErrorFormLiveData.value = CleanFormErrors.OnEtNameError
        }
        if (networkDevice.username.isEmpty()) {
            networkDeviceFormErrorLiveData.value = OnEtUserNameError()
            return false
        } else {
            cleanNetworkDeviceErrorFormLiveData.value = CleanFormErrors.OnEtUserNameError
        }
        if (networkDevice.password.isEmpty()) {
            networkDeviceFormErrorLiveData.value = OnEtPasswordError()
            return false
        }
        if (networkDevice.password.length < 8) {
            networkDeviceFormErrorLiveData.value = OnEtPasswordIsInvalidError()
            return false
        } else {
            cleanNetworkDeviceErrorFormLiveData.value = CleanFormErrors.OnEtPasswordError
        }
        if (networkDevice.ipAddress.isEmpty()) {
            networkDeviceFormErrorLiveData.value = OnEtAddressError()
            return false
        } else {
            cleanNetworkDeviceErrorFormLiveData.value = CleanFormErrors.OnEtAddressError
        }
        if (!networkDevice.ipAddress.isValidIpv4()) {
            networkDeviceFormErrorLiveData.value = OnEtIpv4AddressIsInvalidError()
            return false
        }
        if (networkDevice.networkDeviceType == null) {
            networkDeviceFormErrorLiveData.value = OnDeviceTypeError()
            return false
        } else {
            cleanNetworkDeviceErrorFormLiveData.value = CleanFormErrors.OnDeviceTypeError
        }
        return true
    }
}
