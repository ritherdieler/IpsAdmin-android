package com.dscorp.ispadmin.presentation.ui.features.installationorders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TransferWithinAStation
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.Loader
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyCustomDialog
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyDateTimePickerField
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutLinedDropDown
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Pantalla principal para mostrar la lista paginada de órdenes de instalación.
 */
@Composable
fun InstallationOrderListScreen(
    viewModel: InstallationOrderListViewModel,
    onCreateOrderClicked: () -> Unit,
    onNavigateToRegisterSubscription: (InstallationOrder) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val canCreateOrder = viewModel.canCreateOrder()

    // Detectar cuando la actividad se reanuda
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadInstallationOrders()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadTechnicians()
    }
    InstallationOrderList(
        uiState = uiState,
        onFilterChange = { viewModel.filterByStatus(it) },
        onCreateOrderClicked = onCreateOrderClicked,
        canCreateOrder = canCreateOrder,
        onOrderSelected = viewModel::onOrderSelected,
        onTransferOrderClicked = viewModel::onTransferOrderClicked
    )

    if (uiState.showAssignDialog) {
        AssignTechnicianDialog(
            order = uiState.selectedOrder,
            technicians = uiState.technicians,
            selectedTechnician = uiState.selectedTechnician,
            onTechnicianSelected = viewModel::onTechnicianSelected,
            onScheduledDateSelected = viewModel::onScheduledDateSelected,
            onAssign = viewModel::assignTechnician,
            onDismiss = viewModel::onCloseDialog
        )
    }

    if (uiState.showTransferDialog) {
        TransferOrderDialog(
            order = uiState.selectedOrder,
            technicians = uiState.technicians,
            selectedTechnician = uiState.selectedTechnician,
            onTechnicianSelected = viewModel::onTechnicianSelected,
            onScheduledDateSelected = viewModel::onScheduledDateSelected,
            onTransfer = viewModel::transferOrder,
            onDismiss = viewModel::onCloseTransferDialog
        )
    }
    LaunchedEffect(uiState.navigateToRegisterSubscription) {
        if (uiState.navigateToRegisterSubscription)
            onNavigateToRegisterSubscription(uiState.selectedOrder!!)
    }
}

/**
 * Composable que muestra la lista de órdenes de instalación con filtrado y paginación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationOrderList(
    uiState: InstallationOrderListUiState,
    onFilterChange: (InstallationOrderStatus?) -> Unit,
    onCreateOrderClicked: () -> Unit,
    canCreateOrder: Boolean,
    onOrderSelected: (InstallationOrder) -> Unit,
    onTransferOrderClicked: (InstallationOrder) -> Unit
) {
    var selectedStatus by remember { mutableStateOf<InstallationOrderStatus?>(null) }
    val pagingItems = uiState.installationOrders?.collectAsLazyPagingItems()
    val lazyListState = rememberLazyListState()

    // Para mostrar el botón de scroll al inicio cuando no estamos en la parte superior
    val showScrollToTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 3
        }
    }

    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        floatingActionButton = {
            Column {
                // Botón de scroll al inicio
                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                lazyListState.animateScrollToItem(0)
                            }
                        },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .size(48.dp),
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "Volver al inicio"
                        )
                    }
                }

                // Botón para crear nueva orden - solo visible para usuarios autorizados
                AnimatedVisibility(visible = canCreateOrder) {
                    ExtendedFloatingActionButton(
                        onClick = onCreateOrderClicked,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar"
                            )
                        },
                        text = { Text("Nueva orden") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // Lista de opciones para el dropdown
            val dropDownStatusFilterOptions = listOf(
                StatusOption(null, "Todos"),

                ).toMutableList().apply {

                if (uiState.currentUser!!.type != User.UserType.TECHNICIAN)
                    add(StatusOption(InstallationOrderStatus.SOLICITADO, "Solicitado"))

                add(StatusOption(InstallationOrderStatus.EN_CURSO, "En curso"))

                add(StatusOption(InstallationOrderStatus.CERRADO, "Cerrado"))

                if (uiState.currentUser.type != User.UserType.TECHNICIAN)
                    add(StatusOption(InstallationOrderStatus.CANCELADO, "Cancelado"))

            }.toList()

            if (pagingItems != null) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Filtro de estado con dropdown
                        StatusDropDown(
                            options = dropDownStatusFilterOptions,
                            selectedStatus = selectedStatus,
                            onStatusSelected = { status ->
                                selectedStatus = status
                                onFilterChange(status)
                            }
                        )
                    }

                    items(
                        count = pagingItems.itemCount,
                        key = pagingItems.itemKey { it.id }
                    ) { index ->
                        pagingItems[index]?.let { order ->
                            InstallationOrderItem(
                                order = order,
                                uiState = uiState,
                                onOrderSelected = onOrderSelected,
                                onTransferOrderClicked = onTransferOrderClicked
                            )
                        }
                    }

                    // Manejo de estado de carga
                    pagingItems.apply {
                        when {
                            loadState.refresh is LoadState.Loading -> {
                                item { LoadingItem() }
                            }

                            loadState.append is LoadState.Loading -> {
                                item { LoadingItem() }
                            }

                            loadState.refresh is LoadState.Error -> {
                                val error = loadState.refresh as LoadState.Error
                                item {
                                    ErrorItem(
                                        message = error.error.localizedMessage
                                            ?: "Error desconocido",
                                        onRetry = { retry() }
                                    )
                                }
                            }

                            loadState.append is LoadState.Error -> {
                                val error = loadState.append as LoadState.Error
                                item {
                                    ErrorItem(
                                        message = error.error.localizedMessage
                                            ?: "Error desconocido",
                                        onRetry = { retry() }
                                    )
                                }
                            }

                            itemCount == 0 -> {
                                item {
                                    EmptyStateMessage()
                                }
                            }
                        }
                    }
                }
            } else if (uiState.isLoading) {
                Loader()
            }
        }
    }
}

/**
 * Mensaje para cuando no hay órdenes de instalación
 */
@Composable
fun EmptyStateMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No hay órdenes de instalación",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Crea una nueva orden de instalación con el botón +",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

/**
 * Dropdown para filtrar por estado
 */
// Clase personalizada para manejar la opción "Todos"
data class StatusOption(val status: InstallationOrderStatus?, val displayText: String) {
    override fun toString(): String {
        return displayText
    }
}

@Composable
fun StatusDropDown(
    options: List<StatusOption>,
    selectedStatus: InstallationOrderStatus?,
    onStatusSelected: (InstallationOrderStatus?) -> Unit
) {
    // Encontrar la opción seleccionada actualmente
    val selectedOption = options.find { it.status == selectedStatus } ?: options.first()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Filtrar por estado",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        MyOutLinedDropDown(
            modifier = Modifier.fillMaxWidth(),
            items = options,
            selected = selectedOption.displayText,
            label = "Estado",
            onItemSelected = { selected ->
                onStatusSelected((selected as StatusOption).status)
            }
        )
    }
}

/**
 * Item individual de orden de instalación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationOrderItem(
    order: InstallationOrder,
    uiState: InstallationOrderListUiState,
    onOrderSelected: (InstallationOrder) -> Unit,
    onTransferOrderClicked: (InstallationOrder) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = { onOrderSelected(order) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Orden #${order.id}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(status = order.status)
                    
                    Box {
                        IconButton(
                            onClick = { showMenu = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Más opciones",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(x = 0.dp, y = 4.dp)
                        ) {
                            // Opciones para técnicos
                            if (uiState.currentUser?.type == User.UserType.TECHNICIAN) {
                                if (order.status == InstallationOrderStatus.EN_CURSO) {
                                    DropdownMenuItem(
                                        text = { Text("Transferir a otro técnico") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.TransferWithinAStation,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onTransferOrderClicked(order)
                                        }
                                    )
                                }
                            }
                            
                            // Opciones para administradores, ventas y contadores
                            if (uiState.currentUser?.type in listOf(
                                User.UserType.ADMIN,
                                User.UserType.SALES,
                                User.UserType.ACCOUNTANT
                            )) {
                                if (order.status == InstallationOrderStatus.SOLICITADO) {
                                    DropdownMenuItem(
                                        text = { Text("Asignar técnico") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Engineering,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onOrderSelected(order)
                                        }
                                    )
                                }
                                
                                if (order.status == InstallationOrderStatus.EN_CURSO) {
                                    DropdownMenuItem(
                                        text = { Text("Transferir a otro técnico") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.TransferWithinAStation,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onTransferOrderClicked(order)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Información del cliente con iconos
            InfoRow(
                icon = Icons.Outlined.Person,
                label = "${order.customerFirstName} ${order.customerLastName}",
                contentDescription = "Cliente"
            )

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(
                icon = Icons.Outlined.LocationOn,
                label = order.customerAddress,
                contentDescription = "Dirección"
            )

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(
                icon = Icons.Default.Phone,
                label = order.customerPhone,
                contentDescription = "Teléfono"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fecha de creación
            InfoRow(
                icon = Icons.Outlined.Schedule,
                label = "Creado: ${order.createdAt?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}",
                contentDescription = "Fecha de creación"
            )

            // Mostrar fecha programada solo si está en curso
            if (order.status == InstallationOrderStatus.EN_CURSO && order.scheduledDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    icon = Icons.Outlined.Event,
                    label = "Programado: ${order.scheduledDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}",
                    contentDescription = "Fecha programada"
                )
            }

            // Mostrar lugar si está disponible
            if (order.place != null) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    icon = Icons.Outlined.Place,
                    label = "Lugar: ${order.place}",
                    contentDescription = "Lugar"
                )
            }

            // Mostrar técnico asignado si el usuario no es técnico
            order.technician?.let {
                if ((order.status == InstallationOrderStatus.EN_CURSO || order.status != InstallationOrderStatus.CERRADO) && uiState.currentUser!!.type != User.UserType.TECHNICIAN) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Outlined.Engineering,
                        label = "Técnico: ${order.technician!!.name}",
                        contentDescription = "Técnico asignado"
                    )
                }
            }

        }
    }
}

/**
 * Fila de información con icono
 */
@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    contentDescription: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Chip para mostrar el estado de la orden
 */
@Composable
fun StatusChip(status: InstallationOrderStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        InstallationOrderStatus.SOLICITADO -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary,
            "Solicitado"
        )

        InstallationOrderStatus.EN_CURSO -> Triple(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.tertiary,
            "En Curso"
        )

        InstallationOrderStatus.CERRADO -> Triple(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.secondary,
            "Cerrado"
        )

        InstallationOrderStatus.CANCELADO -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.error,
            "Cancelado"
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Indicador de carga
 */
@Composable
fun LoadingItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * Item de error con opción para reintentar
 */
@Composable
fun ErrorItem(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            MyButton(
                text = "Reintentar",
                onClick = onRetry
            )
        }
    }
}


@Composable
fun AssignTechnicianDialog(
    order: InstallationOrder?,
    technicians: List<User>,
    selectedTechnician: User?,
    onTechnicianSelected: (User) -> Unit,
    onScheduledDateSelected: (LocalDateTime) -> Unit,
    onAssign: () -> Unit,
    onDismiss: () -> Unit
) {
    if (order == null) return

    var dateTimeText by rememberSaveable { mutableStateOf("") }
    val isFormValid = selectedTechnician != null && dateTimeText.isNotEmpty()

    MyCustomDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Asignar Técnico",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Cliente: ${order.customerFirstName} ${order.customerLastName}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "Dirección: ${order.customerAddress}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            MyOutLinedDropDown(
                items = technicians,
                selected = selectedTechnician,
                label = "Técnico",
                onItemSelected = onTechnicianSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            MyDateTimePickerField(
                label = "Fecha y Hora Programada",
                dateTime = dateTimeText,
                onDateTimeSelected = {
                    dateTimeText = it
                    // Convertir a LocalDate para el ViewModel
                    try {
                        // Extraer solo la fecha (ignorar la hora para la conversión a LocalDate)
                        val date =
                            LocalDateTime.parse(it, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                        onScheduledDateSelected(date)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Manejar error de análisis de fecha
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                MyButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                )

                MyButton(
                    text = "Asignar",
                    enabled = isFormValid,
                    onClick = onAssign
                )
            }
        }
    }
}

@Composable
fun TransferOrderDialog(
    order: InstallationOrder?,
    technicians: List<User>,
    selectedTechnician: User?,
    onTechnicianSelected: (User) -> Unit,
    onScheduledDateSelected: (LocalDateTime) -> Unit,
    onTransfer: () -> Unit,
    onDismiss: () -> Unit
) {
    if (order == null) return

    var dateTimeText by rememberSaveable { mutableStateOf("") }
    val isFormValid = selectedTechnician != null && dateTimeText.isNotEmpty()

    MyCustomDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Transferir Orden",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Cliente: ${order.customerFirstName} ${order.customerLastName}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "Dirección: ${order.customerAddress}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            MyOutLinedDropDown(
                items = technicians,
                selected = selectedTechnician,
                label = "Nuevo Técnico",
                onItemSelected = onTechnicianSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            MyDateTimePickerField(
                label = "Nueva Fecha y Hora Programada",
                dateTime = dateTimeText,
                onDateTimeSelected = {
                    dateTimeText = it
                    try {
                        val date = LocalDateTime.parse(it, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                        onScheduledDateSelected(date)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                MyButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                )

                MyButton(
                    text = "Transferir",
                    enabled = isFormValid,
                    onClick = onTransfer
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InstallationOrderListPreview() {
    val mockFlow: Flow<PagingData<InstallationOrder>> = flowOf(
        PagingData.from(
            listOf(
                InstallationOrder(
                    id = 6,
                    customerFirstName = "Sergio",
                    customerLastName = "Martínez",
                    customerAddress = "Calle Principal #123, Colonia Centro",
                    customerPhone = "555-123-4567",
                    status = InstallationOrderStatus.SOLICITADO
                ),
                InstallationOrder(
                    id = 4,
                    customerFirstName = "María",
                    customerLastName = "González",
                    customerAddress = "Av. Reforma #456, Colonia Juárez",
                    customerPhone = "555-987-6543",
                    status = InstallationOrderStatus.EN_CURSO
                ),
                InstallationOrder(
                    id = 5,
                    customerFirstName = "Juan",
                    customerLastName = "Pérez",
                    customerAddress = "Calle 5 de Mayo #789, Colonia Obrera",
                    customerPhone = "555-456-7890",
                    status = InstallationOrderStatus.CERRADO
                ),
                InstallationOrder(
                    id = 18,
                    customerFirstName = "Ana",
                    customerLastName = "Rodríguez",
                    customerAddress = "Blvd. Insurgentes #234, Colonia Roma",
                    customerPhone = "555-321-6789",
                    status = InstallationOrderStatus.CANCELADO
                )
            )
        )
    )

    MyTheme {
        InstallationOrderList(
            uiState = InstallationOrderListUiState(
                installationOrders = mockFlow,
                isLoading = false
            ),
            onFilterChange = {},
            onCreateOrderClicked = {},
            canCreateOrder = true,
            onOrderSelected = {},
            onTransferOrderClicked = {}
        )
    }
} 