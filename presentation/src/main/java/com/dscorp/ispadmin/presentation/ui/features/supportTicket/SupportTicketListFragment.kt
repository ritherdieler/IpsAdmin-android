package com.dscorp.ispadmin.presentation.ui.features.supportTicket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dscorp.ispadmin.databinding.FragmentSupportTicketListBinding
import com.dscorp.ispadmin.databinding.ItemSupportTicketBinding
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.base.BaseFragment
import com.example.data2.data.response.AssistanceTicketResponse
import com.example.data2.data.response.AssistanceTicketStatus
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.androidx.viewmodel.ext.android.viewModel

class SupportTicketListFragment :
    BaseFragment<SupportTicketState, FragmentSupportTicketListBinding>() {

    override val binding by lazy { FragmentSupportTicketListBinding.inflate(layoutInflater) }

    override val viewModel: SupportTicketViewModel by viewModel()

    override fun handleState(state: SupportTicketState) {

    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        val adapter = TicketsPagerAdapter(childFragmentManager, lifecycle)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Pendiente"
                1 -> tab.text = "En Progreso"
                2 -> tab.text = "Cerrado"
            }
        }.attach()
    }
}

class SupportTicketAdapter(
    private val onTicketButtonClicked: (SupportTicketHelper) -> Unit = {},
    private val onCloseTicketButtonClicked: (SupportTicketHelper) -> Unit = {},
    private val onCardClicked: (AssistanceTicketResponse) -> Unit = {},
    private val user: User,
    private val lifecycleOwner: LifecycleOwner
) :
    ListAdapter<SupportTicketHelper, SupportTicketAdapter.SupportTicketViewHolder>(
        SupportTicketDiffUtil()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupportTicketViewHolder {
        val binding by lazy { ItemSupportTicketBinding.inflate(LayoutInflater.from(parent.context)) }
        return SupportTicketViewHolder(
            binding,
            onTicketButtonClicked,
            onCloseTicketButtonClicked,
            onCardClicked =  onCardClicked,
            lifecycleOwner
        )
    }

    override fun onBindViewHolder(holder: SupportTicketViewHolder, position: Int) {
        holder.bind(getItem(position), user)
    }

    class SupportTicketViewHolder(
        val binding: ItemSupportTicketBinding,
        val onTicketButtonClicked: (SupportTicketHelper) -> Unit,
        val onCloseTicketButtonClicked: (SupportTicketHelper) -> Unit,
        val onCardClicked: (AssistanceTicketResponse) -> Unit,
        val lifecycleOwner: LifecycleOwner
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(ticket: SupportTicketHelper, user: User) {

            binding.root.setOnClickListener {
                onCardClicked(ticket.ticket)
            }

            binding.ticket = ticket.ticket

            ticket.isLoading.observe(lifecycleOwner) {
                binding.buttonTakeTicket.isLoading = it
                binding.buttonCloseTicket.isLoading = it
            }

            binding.takeTicketButtonVisibility = getTakeTicketVisibility(ticket.ticket, user)
            binding.closeTicketVisivility = getCloseTicketVisibility(ticket.ticket, user)

            binding.buttonTakeTicket.clickListener = {
                onTicketButtonClicked(ticket)
            }

            binding.buttonCloseTicket.clickListener = {
                onCloseTicketButtonClicked(ticket)
            }

            binding.executePendingBindings()

        }

        private fun getTakeTicketVisibility(ticket: AssistanceTicketResponse, user: User): Int {
            if (user.type != User.UserType.TECHNICIAN && user.type != User.UserType.ADMIN) return View.GONE

            return when (ticket.status) {
                AssistanceTicketStatus.PENDING -> View.VISIBLE
                AssistanceTicketStatus.ASSIGNED, AssistanceTicketStatus.IN_PROGRESS, AssistanceTicketStatus.RESOLVED, AssistanceTicketStatus.CLOSED -> View.GONE
                AssistanceTicketStatus.CANCELLED -> View.GONE
            }
        }

        private fun getCloseTicketVisibility(ticket: AssistanceTicketResponse, user: User): Int {

            return when {
                ticket.status == AssistanceTicketStatus.PENDING && (user.type == User.UserType.SECRETARY || user.type == User.UserType.ADMIN) -> View.VISIBLE
                ticket.status == AssistanceTicketStatus.ASSIGNED && (user.type == User.UserType.TECHNICIAN || user.type == User.UserType.ADMIN) -> View.VISIBLE
                else -> View.GONE
            }
        }
    }

    class SupportTicketDiffUtil : DiffUtil.ItemCallback<SupportTicketHelper>() {
        override fun areItemsTheSame(
            oldItem: SupportTicketHelper,
            newItem: SupportTicketHelper
        ): Boolean {
            return oldItem.ticket.id == newItem.ticket.id
        }

        override fun areContentsTheSame(
            oldItem: SupportTicketHelper,
            newItem: SupportTicketHelper
        ): Boolean {
            return oldItem.ticket.id == newItem.ticket.id
        }
    }

}


