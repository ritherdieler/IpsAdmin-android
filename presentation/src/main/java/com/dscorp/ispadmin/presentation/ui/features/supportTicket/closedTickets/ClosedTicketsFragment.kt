package com.dscorp.ispadmin.presentation.ui.features.supportTicket.closedTickets

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.dscorp.ispadmin.databinding.FragmentListTicketsBinding
import com.dscorp.ispadmin.presentation.ui.features.base.BaseFragment
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.SupportTicketAdapter
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.SupportTicketHelper
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.SupportTicketState
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.SupportTicketViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


class ClosedTicketsFragment : BaseFragment<SupportTicketState, FragmentListTicketsBinding>() {
    override val viewModel: SupportTicketViewModel by viewModel()
    override val binding by lazy { FragmentListTicketsBinding.inflate(layoutInflater) }

    private val adapter by lazy {
        SupportTicketAdapter(
            onTicketButtonClicked = {
                lifecycleScope.launch {
//                    viewModel.takeTicket(it.ticket.id)
                }
            },
            onCloseTicketButtonClicked = {
//            viewModel.closeTicket(it)
            },
            onCardClicked = {
                Intent(requireContext(), PhotoViewer::class.java).apply {
                    putExtra("ticket", it)
                }.also {
                    startActivity(it)
                }
            },
            user = viewModel.user,
            lifecycleOwner = this
        )
    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        binding.adapter = adapter
        binding.executePendingBindings()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getClosedTickets()
    }

    override fun handleState(state: SupportTicketState) {

        when (state) {
            is SupportTicketState.TicketList -> {
                populateRecyclerView(state.ticketList.map {
                    SupportTicketHelper(
                        MutableLiveData(
                            false
                        ), it
                    )
                })
            }

            else -> {}
        }
    }

    private fun populateRecyclerView(tickets: List<SupportTicketHelper>) {
        adapter.submitList(tickets)
    }

}