package com.dscorp.ispadmin.presentation.ui.features.supportTicket.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionFastSearchResponse
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.MyAutoCompleteTextViewCompose
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.ispadmin.presentation.ui.components.MyCustomDialog
import com.dscorp.ispadmin.presentation.ui.components.MyOutLinedDropDown
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.badge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox.priorityBadge

private const val DESCRIPTION_MAX = 300

/** Create ticket (a-tic-05) with the categories sheet (a-tic-06). */
@Composable
fun CreateSupportTicketScreen(
    uiState: CreateSupportTicketUiState,
    onPhoneChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onIsClientChange: (Boolean) -> Unit,
    onPlaceSelected: (Place?) -> Unit,
    onSubscriptionSelected: (SubscriptionFastSearchResponse?) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCreateTicket: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAssign: () -> Unit = {},
    onAssigneeChange: (Int?) -> Unit = {},
    categories: List<String> = TICKET_CATEGORIES,
) {
    val colors = IspTheme.colors
    var showCategories by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Rounded.Close, contentDescription = "Cerrar sin guardar") }
                Text("Nuevo ticket", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FieldLabel("¿Para quién?")
                    ClientToggle(uiState.isClient, onIsClientChange)
                    if (uiState.isClient) {
                        SubscriberField(uiState, onSubscriptionSelected, onSearchTextChange)
                    } else {
                        TextInput(
                            label = "Nombre completo *",
                            value = uiState.customerName,
                            onValueChange = onCustomerNameChange,
                            error = uiState.customerNameError,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FieldLabel("Lugar *")
                            MyOutLinedDropDown(
                                items = uiState.places,
                                selected = uiState.selectedPlace,
                                onItemSelected = { place -> onPlaceSelected(place) },
                                label = "Elige el lugar",
                                hasError = uiState.placeError != null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            uiState.placeError?.let { ErrorText(it) }
                        }
                    }
                }

                TextInput(
                    label = "Teléfono de contacto *",
                    value = uiState.phone,
                    onValueChange = { if (it.length <= 9) onPhoneChange(it) },
                    error = uiState.phoneError,
                    helper = "Del titular. Puedes cambiarlo.".takeIf { uiState.isClient && uiState.hasLoadedClientDetails },
                    keyboardType = KeyboardType.Phone,
                )

                CategoryField(
                    selected = uiState.category,
                    categories = categories,
                    error = uiState.categoryError,
                    onSelect = onCategoryChange,
                    onShowAll = { showCategories = true },
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Descripción *")
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { if (it.length <= DESCRIPTION_MAX) onDescriptionChange(it) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
                        placeholder = { Text("¿Qué pasa y desde cuándo?") },
                        isError = uiState.descriptionError != null,
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 6,
                    )
                    Row(Modifier.fillMaxWidth()) {
                        uiState.descriptionError?.let { ErrorText(it, Modifier.weight(1f)) } ?: Spacer(Modifier.weight(1f))
                        Text("${uiState.description.length}/$DESCRIPTION_MAX", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                AssigneeField(uiState.technicians, uiState.assigneeId, onOpenAssign, onAssigneeChange)
            }
            Column(
                Modifier.fillMaxWidth().background(colors.card).navigationBarsPadding(),
            ) {
                HorizontalDivider(color = colors.cardBorder)
                Button(
                    onClick = onCreateTicket,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp).heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.hero, contentColor = colors.onHero),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = colors.onHero, strokeWidth = 2.dp)
                    } else {
                        Text("Crear ticket", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }

        if (showCategories) {
            CategorySheet(
                selected = uiState.category,
                categories = categories,
                onSelect = { onCategoryChange(it); showCategories = false },
                onDismiss = { showCategories = false },
            )
        }

        if (uiState.isTicketCreated) {
            ResultDialog(
                title = "Ticket creado",
                message = uiState.error ?: "El ticket quedó registrado.",
                onConfirm = onNavigateBack,
            )
        } else if (uiState.error != null) {
            ResultDialog(title = "No se pudo crear", message = uiState.error, onConfirm = onDismissError)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun TextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    helper: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
        when {
            error != null -> ErrorText(error)
            helper != null -> Text(helper, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ClientToggle(isClient: Boolean, onChange: (Boolean) -> Unit) {
    val colors = IspTheme.colors
    val outline = MaterialTheme.colorScheme.outline
    Row(
        Modifier.fillMaxWidth().height(44.dp).border(1.dp, outline, CircleShape).background(colors.card, CircleShape).padding(1.dp),
    ) {
        listOf(true to "Abonado", false to "No abonado").forEachIndexed { index, (value, label) ->
            val selected = isClient == value
            if (index == 1) Box(Modifier.width(1.dp).fillMaxHeight().background(outline))
            Box(
                Modifier.weight(1f).fillMaxSize()
                    .background(if (selected) colors.selectedContainer else Color.Transparent, if (index == 0) RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50) else RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50))
                    .selectable(selected = selected, role = Role.RadioButton) { onChange(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (selected) "✓ $label" else label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) colors.onSelectedContainer else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SubscriberField(
    uiState: CreateSupportTicketUiState,
    onSubscriptionSelected: (SubscriptionFastSearchResponse?) -> Unit,
    onSearchTextChange: (String) -> Unit,
) {
    val selected = uiState.selectedSubscription
    if (selected != null && uiState.hasLoadedClientDetails) {
        SubscriberCard(
            name = uiState.clientName.ifBlank { selected.fullName.orEmpty() }.toTitleCase(),
            summary = listOf(uiState.clientPlan, uiState.clientLocation.toTitleCase()).filter { it.isNotBlank() }.joinToString(" · "),
            status = uiState.clientStatus,
            onChange = { onSubscriptionSelected(null) },
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MyAutoCompleteTextViewCompose(
            items = uiState.subscriptions,
            label = "Buscar abonado por nombre",
            selectedItem = selected,
            onItemSelected = { onSubscriptionSelected(it) },
            onSelectionCleared = { onSubscriptionSelected(null) },
            onTextChanged = onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            hasError = uiState.subscriptionError != null,
        )
        if (uiState.isLoadingSubscription) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Cargando datos del abonado…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        (uiState.subscriptionError ?: uiState.subscriptionLoadError)?.let { ErrorText(it) }
    }
}

@Composable
private fun SubscriberCard(name: String, summary: String, status: ServiceStatus?, onChange: () -> Unit) {
    val colors = IspTheme.colors
    Row(
        Modifier.fillMaxWidth().border(2.dp, colors.hero, RoundedCornerShape(16.dp)).background(colors.card, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(colors.selectedContainer, CircleShape), contentAlignment = Alignment.Center) {
            Text(initials(name), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSelectedContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (summary.isNotBlank()) {
                Text(summary, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            status?.let { HubBadge(it.badge()) }
            Text(
                "Cambiar",
                modifier = Modifier.clickable(role = Role.Button, onClick = onChange).padding(vertical = 2.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accentInk,
            )
        }
    }
}

private fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }

@Composable
private fun CategoryField(
    selected: String,
    categories: List<String>,
    error: String?,
    onSelect: (String) -> Unit,
    onShowAll: () -> Unit,
) {
    val colors = IspTheme.colors
    val shown = categories.take(TOP_CATEGORIES).let { top -> if (selected.isNotEmpty() && selected !in top) top + selected else top }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Categoría *")
        Column(
            Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp))
                .then(if (error != null) Modifier.border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp)) else Modifier)
                .padding(horizontal = 16.dp),
        ) {
            shown.forEachIndexed { index, category ->
                CategoryRow(category, category == selected) { onSelect(category) }
                if (index < shown.lastIndex) HorizontalDivider(color = colors.track)
            }
        }
        error?.let { ErrorText(it) }
        Row(
            Modifier.heightIn(min = 40.dp).clickable(role = Role.Button, onClick = onShowAll),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ver las ${categories.size} categorías", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.accentInk)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.accentInk, modifier = Modifier.size(16.dp))
        }
        if (selected.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text("Prioridad:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                priorityBadge(categoryPriority(selected))?.let { HubBadge(it) }
                Text("· según la categoría", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryRow(category: String, selected: Boolean, onSelect: () -> Unit) {
    val colors = IspTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.hero),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            categoryLabel(category),
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        priorityBadge(categoryPriority(category))?.let { HubBadge(it) }
    }
}

/** In-screen sheet so it shares the window with the form (no popup). */
@Composable
private fun CategorySheet(selected: String, categories: List<String>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = IspTheme.colors
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color(0x8510142B))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        Column(
            Modifier.fillMaxSize().padding(top = 64.dp)
                .background(colors.card, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .testTag(CreateTicketTestTags.CATEGORY_SHEET)
                .padding(horizontal = 16.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 32.dp, height = 4.dp).background(MaterialTheme.colorScheme.outline, CircleShape))
            }
            Spacer(Modifier.height(12.dp))
            Text("Categoría", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Por uso · la prioridad sale de la categoría", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
                categories.forEachIndexed { index, category ->
                    CategoryRow(category, category == selected) { onSelect(category) }
                    if (index < categories.lastIndex) HorizontalDivider(color = colors.track)
                }
            }
        }
    }
}

@Composable
private fun AssigneeField(
    technicians: List<Pair<Int, String>>,
    assigneeId: Int?,
    onOpen: () -> Unit,
    onSelect: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val name = technicians.firstOrNull { it.first == assigneeId }?.second ?: "Sin asignar"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Asignar a")
        Box(Modifier.onSizeChanged { fieldWidth = it.width }) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .background(IspTheme.colors.card, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { onOpen(); expanded = true }
                    .padding(start = 14.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, modifier = Modifier.weight(1f), fontSize = 16.sp)
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(with(density) { fieldWidth.toDp() }),
            ) {
                DropdownMenuItem(text = { Text("Sin asignar") }, onClick = { onSelect(null); expanded = false })
                if (technicians.isEmpty()) {
                    DropdownMenuItem(text = { Text("Cargando técnicos…") }, onClick = {}, enabled = false)
                }
                technicians.forEach { (id, technician) ->
                    DropdownMenuItem(text = { Text(technician) }, onClick = { onSelect(id); expanded = false })
                }
            }
        }
        Text("Opcional", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultDialog(title: String, message: String, onConfirm: () -> Unit) {
    MyCustomDialog(
        onDismissRequest = onConfirm,
        content = {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(message, style = MaterialTheme.typography.bodyMedium)
                MyButton(onClick = onConfirm, text = "Aceptar", modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun CreateSupportTicketScreenPreview() {
    MyTheme {
        CreateSupportTicketScreen(
            uiState = CreateSupportTicketUiState(
                selectedSubscription = SubscriptionFastSearchResponse(id = 10, fullName = "Gloria Ticona Apaza"),
                hasLoadedClientDetails = true,
                clientName = "Gloria Ticona Apaza",
                clientPlan = "Duo basico 80",
                clientLocation = "9 de Octubre",
                clientStatus = ServiceStatus.ACTIVE,
                phone = "976333444",
                category = "No tiene señal de Tv Cable",
                description = "Sin señal en el televisor de la sala desde hoy en la mañana.",
            ),
            onPhoneChange = {}, onCategoryChange = {}, onDescriptionChange = {}, onIsClientChange = {},
            onPlaceSelected = {}, onSubscriptionSelected = {}, onSearchTextChange = {}, onCustomerNameChange = {},
            onCreateTicket = {}, onDismissError = {}, onNavigateBack = {},
        )
    }
}
