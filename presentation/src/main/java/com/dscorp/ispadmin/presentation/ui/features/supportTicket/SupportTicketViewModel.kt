package com.dscorp.ispadmin.presentation.ui.features.supportTicket

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import com.dscorp.ispadmin.presentation.extension.firstDayFromCurrentMonth
import com.dscorp.ispadmin.presentation.extension.lastDayFromCurrentMonth
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel
import com.dscorp.ispadmin.presentation.util.compressImage
import com.dscorp.ispadmin.presentation.util.rotateImageIfNeeded
import com.dscorp.ispadmin.domain.model.PlaceResponse
import com.dscorp.ispadmin.domain.model.SubscriptionFastSearchResponse
import com.example.data2.data.apirequestmodel.AssistanceTicketRequest
import com.example.data2.data.repository.IRepository
import com.example.data2.data.response.AssistanceTicketResponse
import com.example.data2.data.response.AssistanceTicketStatus
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Calendar


class SupportTicketViewModel(
    private val repository: IRepository,
    private val context: Context
) : BaseViewModel<SupportTicketState>() {

    val placesFlow = MutableStateFlow<List<PlaceResponse>>(value = emptyList())

    val user = repository.getUserSession()!!

    val categories = listOf(
        "Sin Conexión a Internet",
        "Internet Lento",
        "Migración a fibra óptica",
        "Cambio de Domicilio",
        "Cambio de Contraseña",
        "Ruptura de cable última milla",
        "Alineamiento de antena CPE",
        "Instalación de Tv Cable",
        "Añadir Tv Cable a su plan de internet",
        "No tiene señal de Tv Cable",
        "Cambio de Onu",
        "Cambio de Router",
        "Instalación de repetidor",
        "Evaluar Factibilidad de Servicio",
        "Instalación de Internet",
        "Otros",
    )

    init {
        getPlaces()
    }

    private fun getPlaces() = executeNoProgress {
        val response = repository.getPlaces()
        placesFlow.value = response
    }

    fun getTicket(ticketId: String) = executeWithProgress {
        val response = repository.getTicket(ticketId)
        uiState.postValue(BaseUiState(SupportTicketState.Success(response)))
    }

    suspend fun takeTicket(id: Int) {
        val response =
            repository.assignSupportTicketToUser(id, AssistanceTicketStatus.ASSIGNED, user.id!!)
        uiState.postValue(BaseUiState(SupportTicketState.UpdatedTicket(response)))
    }

    fun getFileFromUri(context: Context, fileUri: Uri): File? {
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

    suspend fun closeTicket(ticket: AssistanceTicketResponse, installationSheetUri: Uri) {
        val file = getFileFromUri(context, installationSheetUri)
        file?.let {
            rotateImageIfNeeded(context, it, installationSheetUri)?.compressImage(50)?.apply {
                val response = repository.closeTicket(
                    id = ticket.id,
                    newStatus = AssistanceTicketStatus.CLOSED,
                    userId = user.id!!,
                    imageBase64 = this
                )
                uiState.postValue(BaseUiState(SupportTicketState.UpdatedTicket(response)))
            }
        }
    }

    suspend fun closeUnattendedTicket(ticket: AssistanceTicketResponse) {

        val response = repository.closeUnattendedTicket(
            id = ticket.id,
            newStatus = AssistanceTicketStatus.CANCELLED,
            userId = user.id!!,
        )

        uiState.postValue(BaseUiState(SupportTicketState.UpdatedTicket(response)))
    }

    fun getClosedTickets() = executeWithProgress {
        val firstDayOfMonth = Calendar.getInstance().firstDayFromCurrentMonth()
        val lastDayOfMonth = Calendar.getInstance().lastDayFromCurrentMonth()
        val response = repository.getTicketsByDateRange(
            AssistanceTicketStatus.CLOSED,
            firstDayOfMonth,
            lastDayOfMonth
        )
        uiState.postValue(BaseUiState(SupportTicketState.TicketList(response)))
    }

    fun getPendingTickets() = executeWithProgress {
        val response = repository.getTicketsByStatus(AssistanceTicketStatus.PENDING)
        uiState.postValue(BaseUiState(SupportTicketState.TicketList(response)))
    }

    fun getTakenTickets() = executeWithProgress {
        val response = repository.getTicketsByStatus(AssistanceTicketStatus.ASSIGNED)
        uiState.postValue(BaseUiState(SupportTicketState.TicketList(response)))
    }

    fun createTicket(supportTicket: AssistanceTicketRequest) = executeWithProgress {
        if (supportTicket.isValid()) {
            repository.createTicket(supportTicket)
            uiState.postValue(BaseUiState(SupportTicketState.TicketCreated))
        } else {
            uiState.postValue(BaseUiState(SupportTicketState.FormError("Los Datos son incorrectos")))
        }
    }

    fun findSubscriptionByNames(names: String) = executeNoProgress {
        val response = repository.findSubscriptionByNames(names)
        uiState.postValue(BaseUiState(SupportTicketState.SearchSubscriptionResult(response)))
    }

}

private fun File.toBase64(context: Context): String {
    val inputStream = context.contentResolver.openInputStream(this.toUri())
    val bytes = inputStream?.readBytes()
    return Base64.encodeToString(bytes, Base64.DEFAULT)
}

sealed class SupportTicketState {
    object Empty : SupportTicketState()
    data class UpdatedTicket(val ticket: AssistanceTicketResponse) : SupportTicketState()

    data class Success(val ticket: AssistanceTicketResponse) : SupportTicketState()

    data class TicketList(val ticketList: List<AssistanceTicketResponse>) : SupportTicketState()
    data class FormError(val error: String) : SupportTicketState()
    data class SearchSubscriptionResult(val response: List<SubscriptionFastSearchResponse>) :
        SupportTicketState()

    object TicketCreated : SupportTicketState()

}

