package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.SupportTicketListScreen
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.SupportTicketListViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Actividad que muestra la lista de tickets de soporte directamente con Compose.
 * Se usa principalmente para abrir tickets desde notificaciones.
 */
class TicketActivity : AppCompatActivity() {
    
    private val viewModel: SupportTicketListViewModel by viewModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtener ID del ticket si viene en los extras
        val ticketId = intent.getStringExtra(TICKET_ID)
        
        setContentView(
            ComposeView(this).apply {
                setContent {
//                    setStatusBarColor(this)
                    
                    val uiState by viewModel.uiState.collectAsState()
                    
                    MyTheme {
                        SupportTicketListScreen(
                            uiState = uiState,
                            onTabChange = viewModel::onTabChange,
                            onTakeTicket = viewModel::takeTicket,
                            onCloseUnattendedTicket = viewModel::closeUnattendedTicket,
                            onCloseTicket = viewModel::closeTicket,
                            onTicketCardClick = { /* Implementar navegación a detalles */ },
                            onRefresh = viewModel::refreshData,
                            onDismissError = viewModel::dismissError
                        )
                    }
                }
            }
        )
    }
} 