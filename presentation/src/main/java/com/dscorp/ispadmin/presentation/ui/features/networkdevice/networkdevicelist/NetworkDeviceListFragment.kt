package com.dscorp.ispadmin.presentation.ui.features.networkdevice.networkdevicelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.FragmentNetworkDeviceListBinding
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class NetworkDeviceListFragment : Fragment() {

    private lateinit var binding: FragmentNetworkDeviceListBinding
    private val viewModel: NetworkDeviceListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(
                layoutInflater,
                R.layout.fragment_network_device_list,
                null,
                true
            )
        observe()
        return binding.root
    }

    private fun observe() {
        lifecycleScope.launch {
            viewModel.responseLiveData.observe(viewLifecycleOwner) {
                when (it) {
                    is NetworkDeviceListResponse.OnError -> {}
                    is NetworkDeviceListResponse.OnNetworkDeviceListFound -> fillRecycleView(it)
                }
            }
        }
    }

    private fun fillRecycleView(it: NetworkDeviceListResponse.OnNetworkDeviceListFound) {
        val adapter = NetworkDeviceAdapter()
        adapter.submitList(it.networkDeviceList)
        binding.rvNetworkDeviceList.adapter = adapter

        binding.rvNetworkDeviceList.visibility =
            if (it.networkDeviceList.isNotEmpty()) View.VISIBLE else View.GONE
    }
}
