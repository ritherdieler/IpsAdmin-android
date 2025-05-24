package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyCustomDialog
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import java.io.File

@Composable
fun SupportTicketListScreen(
    uiState: SupportTicketListUiState,
    onTabChange: (Int) -> Unit,
    onTakeTicket: (Int) -> Unit,
    onCloseUnattendedTicket: (AssistanceTicketResponse) -> Unit,
    onCloseTicket: (AssistanceTicketResponse, Uri) -> Unit,
    onTicketCardClick: (AssistanceTicketResponse) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit
) {
    var selectedTicket by remember { mutableStateOf<AssistanceTicketResponse?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRefresh,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Recargar"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Pestaña seleccionada
                TabRow(
                    selectedTabIndex = uiState.activeTab,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    uiState.tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.activeTab == index,
                            onClick = { onTabChange(index) },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                }

                // Contenido según la pestaña seleccionada
                when (uiState.activeTab) {
                    0 -> PendingTicketsTab(
                        tickets = uiState.pendingTickets,
                        loadingTickets = uiState.pendingTicketsLoading,
                        currentUser = uiState.user!!,
                        onTakeTicket = onTakeTicket,
                        onCloseTicket = onCloseUnattendedTicket,
                        onTicketCardClick = onTicketCardClick,
                        gettingTicketsFromServer = uiState.isLoading
                    )

                    1 -> InProgressTicketsTab(
                        tickets = uiState.inProgressTickets,
                        loadingTickets = uiState.inProgressTicketsLoading,
                        currentUser = uiState.user!!,
                        onCloseTicket = { ticket ->
                            selectedTicket = ticket
                        },
                        onTicketCardClick = onTicketCardClick,
                        gettingTicketsFromServer = uiState.isLoading
                    )

                    2 -> ClosedTicketsTab(
                        tickets = uiState.closedTickets,
                        currentUser = uiState.user!!,
                        onTicketCardClick = onTicketCardClick,
                        gettingTicketsFromServer = uiState.isLoading
                    )
                }
            }

            // Dialog de error
            uiState.error?.let { error ->
                ErrorDialog(
                    message = error,
                    onDismiss = onDismissError
                )
            }

            // Dialog para cerrar ticket
            selectedTicket?.let { ticket ->
                CloseTicketDialog(
                    ticket = ticket,
                    onDismissRequest = { selectedTicket = null },
                    onConfirm = { uri ->
                        onCloseTicket(ticket, uri)
                        selectedTicket = null
                    }
                )
            }
        }
    }
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    MyCustomDialog(
        onDismissRequest = onDismiss,
        content = { columnScope ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                MyButton(
                    onClick = onDismiss,
                    text = "Aceptar",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
fun CloseTicketDialog(
    ticket: AssistanceTicketResponse,
    onDismissRequest: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Crear un archivo temporal para la imagen
    val file = remember {
        File(context.getExternalFilesDir(null), "image_${System.currentTimeMillis()}.jpg").apply {
            createNewFile()
            deleteOnExit()
        }
    }

    // URI para la imagen capturada
    val uri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri = uri
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(uri)
            }
        }
    )

    MyCustomDialog(
        onDismissRequest = onDismissRequest,
        content = { columnScope ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Cerrar Ticket",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Para cerrar el ticket, es necesario tomar una foto de la orden de trabajo firmada por el cliente",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                MyButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    text = "Tomar Foto",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (imageUri != null) {
                    Text(
                        text = "✅ Imagen capturada correctamente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MyButton(
                        onClick = { imageUri?.let { onConfirm(it) } },
                        text = "Cerrar Ticket",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = imageUri != null
                    )
                }
            }
        }
    )
}

@Composable
fun PendingTicketsTab(
    tickets: List<AssistanceTicketResponse>,
    loadingTickets: Map<Int, Boolean>,
    currentUser: com.dscorp.ispadmin.domain.model.User,
    onTakeTicket: (Int) -> Unit,
    onCloseTicket: (AssistanceTicketResponse) -> Unit,
    onTicketCardClick: (AssistanceTicketResponse) -> Unit,
    gettingTicketsFromServer: Boolean = false
) {
    AnimatedTicketList(
        tickets = tickets,
        emptyMessage = "No hay tickets pendientes",
        gettingTicketsFromServer = gettingTicketsFromServer,
        content = { ticket ->
            TicketCard(
                ticket = ticket,
                currentUser = currentUser,
                isLoading = loadingTickets[ticket.id] ?: false,
                onCardClick = { onTicketCardClick(ticket) },
                onTakeTicket = { onTakeTicket(ticket.id) },
                onCloseTicket = { onCloseTicket(ticket) }
            )
        }
    )
}

@Composable
fun InProgressTicketsTab(
    tickets: List<AssistanceTicketResponse>,
    loadingTickets: Map<Int, Boolean>,
    currentUser: com.dscorp.ispadmin.domain.model.User,
    onCloseTicket: (AssistanceTicketResponse) -> Unit,
    onTicketCardClick: (AssistanceTicketResponse) -> Unit,
    gettingTicketsFromServer: Boolean = false
) {
    AnimatedTicketList(
        tickets = tickets,
        emptyMessage = "No hay tickets en progreso",
        content = { ticket ->
            TicketCard(
                ticket = ticket,
                currentUser = currentUser,
                isLoading = loadingTickets[ticket.id] ?: false,
                onCardClick = { onTicketCardClick(ticket) },
                onCloseTicket = { onCloseTicket(ticket) }
            )
        },
        gettingTicketsFromServer = gettingTicketsFromServer
    )
}

@Composable
fun ClosedTicketsTab(
    tickets: List<AssistanceTicketResponse>,
    currentUser: com.dscorp.ispadmin.domain.model.User,
    onTicketCardClick: (AssistanceTicketResponse) -> Unit,
    gettingTicketsFromServer: Boolean = false
) {
    AnimatedTicketList(
        tickets = tickets,
        emptyMessage = "No hay tickets cerrados en el mes actual",
        content = { ticket ->
            TicketCard(
                ticket = ticket,
                currentUser = currentUser,
                onCardClick = { onTicketCardClick(ticket) }
            )
        },
        gettingTicketsFromServer = gettingTicketsFromServer
    )
}

@Composable
fun AnimatedTicketList(
    tickets: List<AssistanceTicketResponse>,
    emptyMessage: String,
    content: @Composable (AssistanceTicketResponse) -> Unit,
    gettingTicketsFromServer: Boolean
) {
    val listState = rememberLazyListState()
    if (gettingTicketsFromServer) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else if (tickets.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(
                items = tickets,
                key = { it.id }
            ) { ticket ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    content(ticket)
                }
            }
        }
    }
} 