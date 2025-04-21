package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dscorp.ispadmin.databinding.ItemFindSubscriptionBinding
import com.dscorp.ispadmin.domain.model.SubscriptionResponse

class FindSubscriptionAdapter(val listener: SelectableSubscriptionListener) :
    ListAdapter<SubscriptionResponse, FindSubscriptionAdapter.FindSubscriptionViewHolder>(
        SubscriptionDiffCallback()
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FindSubscriptionViewHolder {
        val binding =
            ItemFindSubscriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FindSubscriptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FindSubscriptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FindSubscriptionViewHolder(private val binding: ItemFindSubscriptionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(subscription: SubscriptionResponse) {
            binding.subscription = subscription
            binding.executePendingBindings()
            binding.root.setOnClickListener {
                listener.onSubscriptionPopupButtonSelected(subscription, it)
            }

        }
    }

    private class SubscriptionDiffCallback : DiffUtil.ItemCallback<SubscriptionResponse>() {
        override fun areItemsTheSame(
            oldItem: SubscriptionResponse,
            newItem: SubscriptionResponse
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: SubscriptionResponse,
            newItem: SubscriptionResponse
        ): Boolean {
            return oldItem == newItem
        }
    }
}
