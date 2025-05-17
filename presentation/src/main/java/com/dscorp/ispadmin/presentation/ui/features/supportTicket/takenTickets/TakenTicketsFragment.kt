package com.dscorp.ispadmin.presentation.ui.features.supportTicket.takenTickets

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.dscorp.ispadmin.databinding.FragmentListTicketsBinding
import com.dscorp.ispadmin.presentation.ui.features.base.BaseFragment
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.SupportTicketAdapter
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.SupportTicketHelper
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.SupportTicketState
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.SupportTicketViewModel
import com.dscorp.ispadmin.presentation.util.ImageCaptureManager
import com.dscorp.ispadmin.presentation.util.PermissionManager
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class TakenTicketsFragment : BaseFragment<SupportTicketState, FragmentListTicketsBinding>() {
    val permissionManager = PermissionManager(
        this,
        onDeny = {

        },
        onRationale = {

        }
    )
    private var selectedTicket: SupportTicketHelper? = null

    override val viewModel: SupportTicketViewModel by viewModel()
    override val binding by lazy { FragmentListTicketsBinding.inflate(layoutInflater) }
    private val adapter by lazy {
        SupportTicketAdapter(
            onTicketButtonClicked = {
                lifecycleScope.launch {
                    try {
                        viewModel.takeTicket(it.ticket.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        it.isLoading.value = false
                    }
                }
            },
            onCloseTicketButtonClicked = {
                selectedTicket = it
                permissionManager.requestPermission(android.Manifest.permission.CAMERA) {
                    imageCaptureManager.takeImage()
                }
            },
            user = viewModel.user,
            lifecycleOwner = this
        )
    }


    private val imageCaptureManager = ImageCaptureManager(
        lifecycleOwner = this,
        onImageCaptured = { imageUri ->
            selectedTicket?.let { ticket ->
                lifecycleScope.launch {
                    ticket.isLoading.value = true
                    try {
                        viewModel.closeTicket(ticket.ticket, imageUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        ticket.isLoading.value = false
                    }
                }
            }
        },
    )

    override fun onViewReady(savedInstanceState: Bundle?) {
        binding.adapter = adapter
        binding.executePendingBindings()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getTakenTickets()
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

            is SupportTicketState.UpdatedTicket -> {
                val list = adapter.currentList.filter {
                    it.ticket.id != state.ticket.id
                }
                adapter.submitList(list)
            }

            else -> {}
        }
    }

    private fun populateRecyclerView(tickets: List<SupportTicketHelper>) {
        adapter.submitList(tickets)
    }

}