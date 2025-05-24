package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.presentation.extension.firstDayFromCurrentMonth
import com.dscorp.ispadmin.presentation.extension.lastDayFromCurrentMonth
import com.dscorp.ispadmin.presentation.util.compressImage
import com.dscorp.ispadmin.presentation.util.rotateImageIfNeeded
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Calendar

class SupportTicketListViewModel(
    private val repository: IRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportTicketListUiState())
    val uiState: StateFlow<SupportTicketListUiState> = _uiState.asStateFlow()
    
    init {
        loadUserData()
        loadPendingTickets()
    }
    
    private fun loadUserData() {
        viewModelScope.launch {
            try {
                repository.getUserSession()?.let { user ->
                    _uiState.update { it.copy(user = user) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al cargar datos del usuario") }
            }
        }
    }
    
    fun onTabChange(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
        loadTicketsForActiveTab()
    }
    
    fun refreshData() {
        loadTicketsForActiveTab()
    }
    
    private fun loadTicketsForActiveTab() {
        when (_uiState.value.activeTab) {
            0 -> loadPendingTickets()
            1 -> loadInProgressTickets()
            2 -> loadClosedTickets()
        }
    }
    
    private fun loadPendingTickets() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val pendingTickets = repository.getTicketsByStatus(AssistanceTicketStatus.PENDING)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        pendingTickets = pendingTickets,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar tickets pendientes"
                    )
                }
            }
        }
    }
    
    private fun loadInProgressTickets() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val inProgressTickets = repository.getTicketsByStatus(AssistanceTicketStatus.ASSIGNED)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        inProgressTickets = inProgressTickets,
                        error = null
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar tickets en progreso"
                    )
                }
            }
        }
    }
    
    private fun loadClosedTickets() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val firstDayOfMonth = Calendar.getInstance().firstDayFromCurrentMonth()
                val lastDayOfMonth = Calendar.getInstance().lastDayFromCurrentMonth()
                val closedTickets = repository.getTicketsByDateRange(
                    AssistanceTicketStatus.CLOSED,
                    firstDayOfMonth,
                    lastDayOfMonth
                )
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        closedTickets = closedTickets,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar tickets cerrados"
                    )
                }
            }
        }
    }
    
    fun takeTicket(ticketId: Int) {
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        pendingTicketsLoading = it.pendingTicketsLoading.toMutableMap().apply {
                            put(ticketId, true)
                        }
                    )
                }
                
                val user = _uiState.value.user ?: return@launch
                val updatedTicket = runCatching {
                    repository.assignSupportTicketToUser(
                        ticketId,
                        AssistanceTicketStatus.ASSIGNED,
                        user.id!!
                    )
                }.getOrThrow()
                
                // Actualizar los tickets pendientes y en progreso
                refreshData()
                
                _uiState.update { 
                    it.copy(
                        pendingTicketsLoading = it.pendingTicketsLoading.toMutableMap().apply {
                            remove(ticketId)
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        pendingTicketsLoading = it.pendingTicketsLoading.toMutableMap().apply {
                            remove(ticketId)
                        },
                        error = e.message ?: "Error al tomar el ticket"
                    )
                }
            }
        }
    }
    
    fun closeUnattendedTicket(ticket: AssistanceTicketResponse) {
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        pendingTicketsLoading = it.pendingTicketsLoading.toMutableMap().apply {
                            put(ticket.id, true)
                        }
                    )
                }
                
                val user = _uiState.value.user ?: return@launch
                val updatedTicket = runCatching {
                    repository.closeUnattendedTicket(
                        ticket.id,
                        AssistanceTicketStatus.CANCELLED,
                        user.id!!
                    )
                }.getOrThrow()
                
                refreshData()
                
                _uiState.update { 
                    it.copy(
                        pendingTicketsLoading = it.pendingTicketsLoading.toMutableMap().apply {
                            remove(ticket.id)
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        pendingTicketsLoading = it.pendingTicketsLoading.toMutableMap().apply {
                            remove(ticket.id)
                        },
                        error = e.message ?: "Error al cancelar el ticket"
                    )
                }
            }
        }
    }
    
    fun closeTicket(ticket: AssistanceTicketResponse, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        inProgressTicketsLoading = it.inProgressTicketsLoading.toMutableMap().apply {
                            put(ticket.id, true)
                        }
                    )
                }
                
                val user = _uiState.value.user ?: return@launch
                val file = getFileFromUri(context, imageUri)
                
                if (file != null) {
                    val rotatedAndCompressedImage = runCatching {
                        rotateImageIfNeeded(context, file, imageUri)?.compressImage(50)
                    }.getOrNull()
                    
                    if (rotatedAndCompressedImage != null) {
                        val updatedTicket = runCatching {
                            repository.closeTicket(
                                ticket.id,
                                AssistanceTicketStatus.CLOSED,
                                user.id!!,
                                rotatedAndCompressedImage
                            )
                        }.getOrThrow()
                        
                        refreshData()
                    } else {
                        _uiState.update { 
                            it.copy(
                                error = "Error al procesar la imagen"
                            )
                        }
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            error = "Error al obtener la imagen"
                        )
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        inProgressTicketsLoading = it.inProgressTicketsLoading.toMutableMap().apply {
                            remove(ticket.id)
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        inProgressTicketsLoading = it.inProgressTicketsLoading.toMutableMap().apply {
                            remove(ticket.id)
                        },
                        error = e.message ?: "Error al cerrar el ticket"
                    )
                }
            }
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun getFileFromUri(context: Context, fileUri: Uri): File? {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        val tempFile: File
        try {
            tempFile = File.createTempFile("tempFile", null, context.cacheDir)
            inputStream = context.contentResolver.openInputStream(fileUri)
            outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
        return tempFile
    }
} 