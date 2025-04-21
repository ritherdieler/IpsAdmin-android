package com.dscorp.ispadmin.presentation.ui.features.ippool.register

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.dscorp.ispadmin.databinding.FragmentIpPoolBinding
import com.dscorp.ispadmin.presentation.extension.populate
import com.dscorp.ispadmin.presentation.extension.showSuccessDialog
import com.dscorp.ispadmin.presentation.ui.features.base.BaseFragment
import com.dscorp.ispadmin.domain.model.IpPool
import com.dscorp.ispadmin.domain.model.NetworkDevice
import org.koin.androidx.viewmodel.ext.android.viewModel

class IpPoolFragment : BaseFragment<IpPoolUiState, FragmentIpPoolBinding>(),
    IpPoolSelectionListener {

    override val viewModel: IpPoolViewModel by viewModel()
    override val binding by lazy { FragmentIpPoolBinding.inflate(layoutInflater) }
    override fun handleState(state: IpPoolUiState) = when (state) {
        is IpPoolUiState.IpPoolRegister -> showSuccessDialog(state)
        is IpPoolUiState.FormDataReady -> {
            fillRecycleView(state.ipPoolList)
            fillHostDevicesSpinner(state.hostDevices)
        }
    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.executePendingBindings()
    }

    private fun fillHostDevicesSpinner(hostDevices: List<NetworkDevice>) {
        binding.spnHostDevice.populate(hostDevices) {
            viewModel.hostDeviceField.liveData.value = it
        }
    }

    private fun showSuccessDialog(response: IpPoolUiState.IpPoolRegister) {
        showSuccessDialog("La Ip ${response.ipPool.ipSegment} ah sido registrado correctamente")
    }

    private fun fillRecycleView(it: List<IpPool>) {
        val adapter = IpPoolAdapter(this)
        binding.rvIpPool.adapter = adapter
        adapter.submitList(it)
        binding.rvIpPool.visibility =
            if (it.isNotEmpty()) View.VISIBLE else View.GONE
    }

    override fun onIpPoolSelected(ipPool: IpPool) {
        val action = IpPoolFragmentDirections.toIpList(ipPool)
        findNavController().navigate(action)
    }
}
