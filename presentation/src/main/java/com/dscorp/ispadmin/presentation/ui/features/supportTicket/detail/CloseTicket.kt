package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

interface CloseTicketGateway {
    suspend fun ticket(ticketId: Int): AssistanceTicketResponse
    suspend fun close(ticketId: Int, userId: Int, photo: File)
    fun currentUser(): User?
}

data class CloseTicketUiState(
    val ticketId: Int = 0,
    val subtitle: String = "",
    val photo: File? = null,
    val photoTakenAt: String? = null,
    val submitting: Boolean = false,
    val error: String? = null,
)

sealed interface CloseTicketIntent {
    data class Load(val ticketId: Int) : CloseTicketIntent
    data class PhotoTaken(val photo: File) : CloseTicketIntent
    data object Submit : CloseTicketIntent
}

sealed interface CloseTicketEvent {
    data class Closed(val message: String) : CloseTicketEvent
}

/** Close a ticket (a-tic-04). Today the Core stores only the signed order photo; it is required. */
class CloseTicketViewModel(
    private val gateway: CloseTicketGateway,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val now: () -> Instant = { Instant.now() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(CloseTicketUiState())
    val uiState: StateFlow<CloseTicketUiState> = _uiState
    private val _events = MutableSharedFlow<CloseTicketEvent>()
    val events: SharedFlow<CloseTicketEvent> = _events

    fun onIntent(intent: CloseTicketIntent) {
        when (intent) {
            is CloseTicketIntent.Load -> load(intent.ticketId)
            is CloseTicketIntent.PhotoTaken -> _uiState.update {
                it.copy(photo = intent.photo, photoTakenAt = now().atZone(FICHA_ZONE).let { t -> "%02d:%02d".format(t.hour, t.minute) }, error = null)
            }
            CloseTicketIntent.Submit -> submit()
        }
    }

    private fun load(ticketId: Int) {
        _uiState.update { it.copy(ticketId = ticketId) }
        viewModelScope.launch(mainImmediate) {
            val ticket = try {
                gateway.ticket(ticketId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@launch
            }
            _uiState.update {
                it.copy(subtitle = listOfNotNull(
                    sentenceCase(ticket.category),
                    ticket.name.takeIf { name -> name.isNotBlank() }?.toTitleCase(),
                    ticket.place?.takeIf { place -> place.isNotBlank() }?.toTitleCase(),
                ).joinToString(" · "))
            }
        }
    }

    private fun submit() {
        val state = _uiState.value
        val photo = state.photo ?: return
        val userId = gateway.currentUser()?.id ?: return
        if (state.submitting) return
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(submitting = true, error = null) }
            try {
                gateway.close(state.ticketId, userId, photo)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(submitting = false, error = "No se pudo cerrar el ticket. Revisa tu conexión y vuelve a intentar.") }
                return@launch
            }
            _uiState.update { it.copy(submitting = false) }
            _events.emit(CloseTicketEvent.Closed("Ticket #${state.ticketId} cerrado"))
        }
    }
}

@Composable
fun CloseTicketContent(state: CloseTicketUiState, onIntent: (CloseTicketIntent) -> Unit, onTakePhoto: () -> Unit, onBack: () -> Unit) {
    val colors = IspTheme.colors
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = "Cerrar sin guardar") }
            Text("Cerrar ticket #${state.ticketId}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.subtitle.isNotBlank()) Text(state.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Orden firmada *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(96.dp).background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(16.dp))
                if (state.photo == null) {
                    OutlinedButton(onClick = onTakePhoto, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Tomar foto")
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Foto lista", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.success)
                        }
                        state.photoTakenAt?.let { Text("Tomada $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        OutlinedButton(onClick = onTakePhoto) {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Repetir")
                        }
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        }
        Button(
            onClick = { onIntent(CloseTicketIntent.Submit) },
            enabled = state.photo != null && !state.submitting,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.hero, contentColor = colors.onHero),
        ) { Text("Cerrar ticket", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
    }
}

