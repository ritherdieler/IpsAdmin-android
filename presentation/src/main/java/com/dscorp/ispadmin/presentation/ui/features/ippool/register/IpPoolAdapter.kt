package com.dscorp.ispadmin.presentation.ui.features.ippool.register

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dscorp.ispadmin.databinding.ItemIpPoolBinding
import com.dscorp.ispadmin.domain.model.IpPool

class IpPoolAdapter(private val listener: IpPoolSelectionListener) :
    ListAdapter<IpPool, IpPoolAdapter.IpPoolAdapterViewHolder>(
        IpPoolDiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IpPoolAdapterViewHolder {
        val binding =
            ItemIpPoolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IpPoolAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IpPoolAdapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IpPoolAdapterViewHolder(private val binding: ItemIpPoolBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(ipPool: IpPool) {
            binding.ipPool = ipPool
            binding.executePendingBindings()

            binding.btnSeeIps.setOnClickListener {
                listener.onIpPoolSelected(ipPool)
            }
        }
    }
}

private class IpPoolDiffCallback : DiffUtil.ItemCallback<IpPool>() {
    override fun areItemsTheSame(oldItem: IpPool, newItem: IpPool): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: IpPool, newItem: IpPool): Boolean {
        return oldItem == newItem
    }
}
