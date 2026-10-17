package com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge

/** Ticket list (a-tic-01): tabs with counts, chips, compact rows that open the ticket, «Nuevo ticket». */
@Composable
fun TicketInboxContent(state: TicketInboxUiState, onIntent: (TicketInboxIntent) -> Unit, onOpen: (Int) -> Unit, onNew: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Tabs(state, onIntent)
            Chips(state, onIntent)
            Text(state.summary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            when {
                state.loading && state.rows.isEmpty() -> Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() }
                state.error != null && state.rows.isEmpty() -> Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onIntent(TicketInboxIntent.Refresh) }) { Text("Reintentar") }
                }
                state.rows.isEmpty() -> Text(
                    "No hay tickets en esta pestaña.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
                else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)) {
                    itemsIndexed(state.rows, key = { _, row -> row.id }) { index, row ->
                        val first = index == 0
                        val last = index == state.rows.lastIndex
                        val shape: Shape = RoundedCornerShape(
                            topStart = if (first) 16.dp else 0.dp, topEnd = if (first) 16.dp else 0.dp,
                            bottomStart = if (last) 16.dp else 0.dp, bottomEnd = if (last) 16.dp else 0.dp,
                        )
                        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape)) {
                            TicketRowItem(row) { onOpen(row.id) }
                            if (!last) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNew,
            containerColor = IspTheme.colors.accent,
            contentColor = IspTheme.colors.onAccent,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Nuevo ticket", fontWeight = FontWeight.SemiBold) },
        )
    }
}

@Composable
private fun Tabs(state: TicketInboxUiState, onIntent: (TicketInboxIntent) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            TicketTab.entries.forEach { tab ->
                val selected = tab == state.tab
                Column(
                    Modifier.clickable(role = Role.Tab) { onIntent(TicketInboxIntent.TabSelected(tab)) }.padding(horizontal = 12.dp).width(IntrinsicSize.Max),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tab.label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        state.counts[tab]?.takeIf { it > 0 }?.let { count ->
                            Text(
                                count.toString(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = if (selected) IspTheme.colors.onHero else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(if (selected) IspTheme.colors.hero else MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 7.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Box(Modifier.height(3.dp).fillMaxWidth().background(if (selected) IspTheme.colors.hero else Color.Transparent, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
    }
}

@Composable
private fun Chips(state: TicketInboxUiState, onIntent: (TicketInboxIntent) -> Unit) {
    val tickets = state.tickets[state.tab].orEmpty()
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleChip("Prioridad alta", state.filters.highPriority) { onIntent(TicketInboxIntent.HighPriorityToggled) }
        ToggleChip("Sin asignar", state.filters.unassigned) { onIntent(TicketInboxIntent.UnassignedToggled) }
        MenuChip("Categoría", state.filters.category, categoryOptions(tickets)) { onIntent(TicketInboxIntent.CategorySelected(it)) }
        MenuChip("Lugar", state.filters.place, placeOptions(tickets)) { onIntent(TicketInboxIntent.PlaceSelected(it)) }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label, fontSize = 13.sp) }, shape = RoundedCornerShape(8.dp),
        leadingIcon = if (selected) { { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = IspTheme.colors.selectedContainer, selectedLabelColor = IspTheme.colors.onSelectedContainer,
            selectedLeadingIconColor = IspTheme.colors.onSelectedContainer,
        ),
        modifier = Modifier.height(32.dp),
    )
}

@Composable
private fun MenuChip(label: String, selected: String?, options: List<String>, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null, onClick = { open = true }, label = { Text(selected ?: label, fontSize = 13.sp, maxLines = 1) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(8.dp),
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IspTheme.colors.selectedContainer, selectedLabelColor = IspTheme.colors.onSelectedContainer),
            modifier = Modifier.height(32.dp),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Todos") }, onClick = { open = false; onSelect(null) })
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onSelect(option) }) }
        }
    }
}

@Composable
private fun TicketRowItem(row: TicketInboxRow, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            row.priority?.let { HubBadge(it); Spacer(Modifier.width(8.dp)) }
            Text(row.number, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(row.time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(row.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false))
            row.tag?.let { HubBadge(it) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(row.assignment, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
