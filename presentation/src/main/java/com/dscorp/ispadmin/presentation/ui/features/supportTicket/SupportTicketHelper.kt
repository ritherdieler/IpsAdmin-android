package com.dscorp.ispadmin.presentation.ui.features.supportTicket

import androidx.lifecycle.MutableLiveData
import com.example.data2.data.response.AssistanceTicketResponse

data class SupportTicketHelper(
    var isLoading: MutableLiveData<Boolean>,
    val ticket: AssistanceTicketResponse
)