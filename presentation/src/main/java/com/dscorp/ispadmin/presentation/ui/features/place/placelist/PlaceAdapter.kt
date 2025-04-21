package com.dscorp.ispadmin.presentation.ui.features.place.placelist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dscorp.ispadmin.databinding.ItemPlaceListBinding
import com.dscorp.ispadmin.domain.model.PlaceResponse

class PlaceAdapter : ListAdapter<PlaceResponse, PlaceAdapter.PlaceListAdapterViewHolder>(
    NetworkDeviceListDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceListAdapterViewHolder {
        val binding =
            ItemPlaceListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaceListAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceListAdapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaceListAdapterViewHolder(private val binding: ItemPlaceListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(place: PlaceResponse) {
            binding.placeList = place
            binding.executePendingBindings()
        }
    }
}

private class NetworkDeviceListDiffCallback : DiffUtil.ItemCallback<PlaceResponse>() {
    override fun areItemsTheSame(oldItem: PlaceResponse, newItem: PlaceResponse): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PlaceResponse, newItem: PlaceResponse): Boolean {
        return oldItem == newItem
    }
}
