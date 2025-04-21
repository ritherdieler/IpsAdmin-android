package com.dscorp.ispadmin.presentation.ui.features.plan.planlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dscorp.ispadmin.databinding.ItemPlanListBinding
import com.dscorp.ispadmin.domain.model.PlanResponse

class PlanAdapter(val onItemSelected: OnPlanSelectedListener) :
    ListAdapter<PlanResponse, PlanAdapter.PlanListAdapterViewHolder>(PlanListDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanListAdapterViewHolder {
        val binding =
            ItemPlanListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlanListAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanListAdapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlanListAdapterViewHolder(private val binding: ItemPlanListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(plan: PlanResponse) {
            binding.planList = plan
            binding.executePendingBindings()

            binding.root.setOnClickListener {
                onItemSelected.onPlanSelected(plan, it)
            }
        }
    }
}

private class PlanListDiffCallback : DiffUtil.ItemCallback<PlanResponse>() {
    override fun areItemsTheSame(oldItem: PlanResponse, newItem: PlanResponse): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PlanResponse, newItem: PlanResponse): Boolean {
        return oldItem.id == newItem.id
    }
}
