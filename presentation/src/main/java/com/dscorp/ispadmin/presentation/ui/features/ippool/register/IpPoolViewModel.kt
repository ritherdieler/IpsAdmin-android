package com.dscorp.ispadmin.presentation.ui.features.ippool.register

import androidx.lifecycle.MutableLiveData
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.extension.formIsValid
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel
import com.dscorp.ispadmin.presentation.ui.features.formvalidation.ReactiveFormField
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.extensions.IsValidIpv4Segment
import com.example.data2.data.apirequestmodel.IpPoolRequest
import com.example.data2.data.repository.IRepository
import kotlinx.coroutines.async
import org.koin.core.component.KoinComponent

class IpPoolViewModel(private val repository: IRepository) : BaseViewModel<IpPoolUiState>(),
    KoinComponent {

    val formShimmerVisibility = MutableLiveData(true)
    val registerIsLoading = MutableLiveData(false)

    val hostDeviceField = ReactiveFormField<NetworkDevice>(
        hintResourceId = R.string.host_device,
        errorResourceId = R.string.mustSelectHostDevice,
        validator = { it != null })

    val ipSegmentField = ReactiveFormField<String>(
        hintResourceId = R.string.ip_segment,
        errorResourceId = R.string.must_enter_a_valid_ip_segment,
        validator = { it.IsValidIpv4Segment() })


    fun registerIpPool() = executeNoProgress(
        doBefore = { registerIsLoading.value = true },
        doFinally = { registerIsLoading.value = false })
    {
        if (!formIsValid()) return@executeNoProgress
        val ipPool = IpPoolRequest(
            ipSegment = ipSegmentField.getValue()!!,
            hostDeviceId = hostDeviceField.getValue()?.id
        )

        val response = repository.registerIpPool(ipPool)
        uiState.value = BaseUiState(IpPoolUiState.IpPoolRegister(response))
    }

    private fun formIsValid() =
        listOf(hostDeviceField, ipSegmentField).formIsValid()


    init {
        getFormData()
    }

    private fun getFormData() = executeNoProgress(doFinally = {
        formShimmerVisibility.value = false
    }) {

        val responseHostDevices = it.async { repository.getHostDevices() }

        val responseIpPoolList = it.async { repository.getIpPoolList() }

        val hostDevices = responseHostDevices.await()

        val ipPoolList = responseIpPoolList.await()

        uiState.value = BaseUiState(IpPoolUiState.FormDataReady(hostDevices, ipPoolList))

    }
}
