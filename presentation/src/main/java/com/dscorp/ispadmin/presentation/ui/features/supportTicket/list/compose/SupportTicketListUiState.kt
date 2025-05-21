package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import com.dscorp.ispadmin.domain.model.User
import com.example.data2.data.response.AssistanceTicketResponse

/**
 * Clase que representa el estado de la UI para la lista de tickets de soporte
 */
data class SupportTicketListUiState(
    val user: User? = null,
    val activeTab: Int = 0,
    val tabTitles: List<String> = listOf("Pendientes", "En Progreso", "Cerrados"),
    
    // Tickets
    val pendingTickets: List<AssistanceTicketResponse> = emptyList(),
    val inProgressTickets: List<AssistanceTicketResponse> = emptyList(),
    val closedTickets: List<AssistanceTicketResponse> = emptyList(),
    
    // Estados de carga
    val isLoading: Boolean = false,
    val pendingTicketsLoading: Map<Int, Boolean> = emptyMap(),
    val inProgressTicketsLoading: Map<Int, Boolean> = emptyMap(),
    
    // Error
    val error: String? = null
) 