package com.dscorp.ispadmin.presentation.ui.features.networkdevice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dscorp.ispadmin.databinding.FragmentNetworkDeviceBinding
import com.dscorp.ispadmin.presentation.extension.fillWithList
import com.dscorp.ispadmin.presentation.extension.showErrorDialog
import com.dscorp.ispadmin.presentation.extension.showSuccessDialog
import com.dscorp.ispadmin.presentation.ui.features.networkdevice.NetworkDeviceFormError.*
import com.dscorp.ispadmin.domain.model.NetworkDevice
import org.koin.androidx.viewmodel.ext.android.viewModel

class NetworkDeviceFragment : Fragment() {
    private val binding by lazy { FragmentNetworkDeviceBinding.inflate(layoutInflater) }
    private val viewModel: NetworkDeviceViewModel by viewModel()
    private var selectedNetworkDeviceType: NetworkDevice.NetworkDeviceType? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        observeNetWorkDeviceResponse()
        observeNetworkDeviceFormError()
        observeCleanNetworkDeviceErrorForm()
        binding.btRegisterNetworkDevice.setOnClickListener {
            registerNetworkDevice()
        }

        return binding.root
    }

    private fun observeNetWorkDeviceResponse() {
        viewModel.networkDeviceResponseLiveData.observe(viewLifecycleOwner) { response ->
            when (response) {
                is NetworkDeviceResponse.OnNetworkDeviceRegistered -> showSuccessDialog(response)
                is NetworkDeviceResponse.OnError -> showErrorDialog()
                is NetworkDeviceResponse.OnNetworkDeviceTypesReceived -> fillDeviceTypeSpinner(
                    response.networkDeviceTypes
                )
            }
        }
    }

    private fun showSuccessDialog(response: NetworkDeviceResponse.OnNetworkDeviceRegistered) {
        showSuccessDialog("Dispositivo ${response.networkDevice.name} Registrado Correctamente")
    }

    private fun fillDeviceTypeSpinner(networkDeviceTypes: List<String>) {
        binding.etDeviceType.fillWithList(networkDeviceTypes) {
//            selectedNetworkDeviceType = it as String
            selectedNetworkDeviceType = NetworkDevice.NetworkDeviceType.valueOf(it as String)
        }
    }

    private fun observeNetworkDeviceFormError() {
        viewModel.networkDeviceFormErrorLiveData.observe(viewLifecycleOwner) { formError ->
            when (formError) {
                is OnEtAddressError -> binding.tlIpAddress.error = formError.message
                is OnEtNameError -> binding.tlName.error = formError.message
                is OnEtPasswordError -> binding.tlPassword.error = formError.message
                is OnEtUserNameError -> binding.tlUserName.error = formError.message
                is OnDeviceTypeError -> binding.spnDeviceType.error = formError.message
                is OnEtPasswordIsInvalidError -> binding.tlPassword.error = formError.message
                is OnEtIpv4AddressIsInvalidError -> binding.tlIpAddress.error = formError.message
            }
        }
    }

    private fun observeCleanNetworkDeviceErrorForm() {
        viewModel.cleanNetworkDeviceErrorFormLiveData.observe(viewLifecycleOwner) { cleanForm ->
            when (cleanForm) {
                is CleanFormErrors.OnDeviceTypeError -> binding.spnDeviceType.error = null
                is CleanFormErrors.OnEtAddressError -> binding.tlIpAddress.error = null
                is CleanFormErrors.OnEtNameError -> binding.tlName.error = null
                is CleanFormErrors.OnEtPasswordError -> binding.tlPassword.error = null
                is CleanFormErrors.OnEtUserNameError -> binding.tlUserName.error = null
            }
        }
    }

    private fun registerNetworkDevice() {
        val networkDevice = NetworkDevice(
            name = binding.etName.text.toString(),
            password = binding.etPassword.text.toString(),
            username = binding.etUsername.text.toString(),
            ipAddress = binding.etIpAddress.text.toString(),
            networkDeviceType = selectedNetworkDeviceType
        )
        viewModel.registerNetworkDevice(networkDevice)
    }
}
