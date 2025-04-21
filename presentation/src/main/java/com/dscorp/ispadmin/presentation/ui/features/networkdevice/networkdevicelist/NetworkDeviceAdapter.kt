package com.dscorp.ispadmin.presentation.ui.features.networkdevice.networkdevicelist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dscorp.ispadmin.databinding.ItemNetworkDeviceListBinding
import com.dscorp.ispadmin.domain.model.NetworkDeviceResponse

class NetworkDeviceAdapter : ListAdapter<NetworkDeviceResponse, NetworkDeviceAdapter.NetworkDeviceListAdapterViewHolder>(
    NetworkDeviceListDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkDeviceListAdapterViewHolder {
        val binding =
            ItemNetworkDeviceListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NetworkDeviceListAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NetworkDeviceListAdapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NetworkDeviceListAdapterViewHolder(private val binding: ItemNetworkDeviceListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(networkDevice: NetworkDeviceResponse) {
            binding.networkdevicelist = networkDevice
            binding.executePendingBindings()
        }
    }
}

private class NetworkDeviceListDiffCallback : DiffUtil.ItemCallback<NetworkDeviceResponse>() {
    override fun areItemsTheSame(oldItem: NetworkDeviceResponse, newItem: NetworkDeviceResponse): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NetworkDeviceResponse, newItem: NetworkDeviceResponse): Boolean {
        return oldItem == newItem
    }
}
