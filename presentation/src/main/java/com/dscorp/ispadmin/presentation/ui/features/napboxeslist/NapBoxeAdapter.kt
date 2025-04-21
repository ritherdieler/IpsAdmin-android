package com.dscorp.ispadmin.presentation.ui.features.napboxeslist
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dscorp.ispadmin.databinding.ItemNapBoxesListBinding
import com.dscorp.ispadmin.domain.model.NapBoxResponse

class NapBoxeAdapter(val listener: OnItemClickListener) : ListAdapter<NapBoxResponse, NapBoxeAdapter.NapBoxAdapterViewHolder>(
    NapBoxDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NapBoxAdapterViewHolder {
        val binding =
            ItemNapBoxesListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NapBoxAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NapBoxAdapterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    inner class NapBoxAdapterViewHolder(private val binding: ItemNapBoxesListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(napBox: NapBoxResponse) {
            binding.root.setOnClickListener { listener.onItemClick(napBox) }
            binding.napBoxList = napBox
            binding.executePendingBindings()
            binding.btnMenu.setOnClickListener {
                listener.onNapBoxPopupButtonSelected(napBox, it)
            }
        }
    }
}

private class NapBoxDiffCallback : DiffUtil.ItemCallback<NapBoxResponse>() {
    override fun areItemsTheSame(oldItem: NapBoxResponse, newItem: NapBoxResponse): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NapBoxResponse, newItem: NapBoxResponse): Boolean {
        return oldItem == newItem
    }
}
