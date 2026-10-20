package com.dscorp.ispadmin.presentation.ui.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge

sealed interface FieldAction {
    data class Open(val target: TodayTarget) : FieldAction
    data class Register(val target: TodayTarget) : FieldAction
    data class Directions(val query: String) : FieldAction
    data class Filtered(val filter: AgendaFilter) : FieldAction
    data object Profile : FieldAction
    data object PendingUploads : FieldAction
    data object NewOrder : FieldAction
}

object FieldTestTags {
    const val NEW_ORDER = "field_new_order"
}

/** Technician home «Hoy» (a-ini-03). */
@Composable
fun TechnicianTodayContent(state: FieldUiState, onAction: (FieldAction) -> Unit, onRetry: () -> Unit) {
    val today = state.today
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        FieldHeader("Hoy", today?.subtitle, state.initials) { onAction(FieldAction.Profile) }
        FieldStatus(state, onRetry)
        if (today != null) {
            Column(Modifier.padding(top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                today.uploads?.let { UploadsBanner(it) { onAction(FieldAction.PendingUploads) } }
                today.now?.let { job ->
                    FieldSection("Ahora") { NowCard(job, onAction) }
                }
                if (today.pending.isNotEmpty()) {
                    FieldSection("Pendientes") {
                        FieldCard { today.pending.forEachIndexed { index, row -> TodayRowItem(row, index < today.pending.lastIndex) { onAction(FieldAction.Open(row.target)) } } }
                    }
                }
                DoneToday(today.done, onAction)
            }
        }
    }
}

@Composable
private fun FieldHeader(title: String, subtitle: String?, initials: String, onProfile: () -> Unit) {
    val colors = IspTheme.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onProfile), contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp).background(colors.selectedContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(initials, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSelectedContainer)
            }
        }
    }
}

@Composable
private fun FieldStatus(state: FieldUiState, onRetry: () -> Unit) {
    val empty = state.today == null && state.agenda == null
    when {
        empty && state.loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.error != null -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(state.error, Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun FieldSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, Modifier.padding(horizontal = 16.dp), fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun FieldCard(content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(IspTheme.colors.card, RoundedCornerShape(16.dp))) { content() }
}

@Composable
private fun UploadsBanner(banner: UploadBanner, onClick: () -> Unit) {
    val colors = IspTheme.colors
    Row(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(colors.warningContainer, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).background(Color.White, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("${banner.title} · ${banner.detail}", fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun NowCard(job: NowJob, onAction: (FieldAction) -> Unit) {
    val colors = IspTheme.colors
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            .border(2.dp, colors.hero, RoundedCornerShape(16.dp)).background(colors.card, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) { onAction(FieldAction.Open(job.target)) }
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            job.priority?.let { HubBadge(it) }
            Text(job.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            job.time?.let {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = colors.hero, modifier = Modifier.size(16.dp))
                Text(it, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.hero)
            }
        }
        Text(job.title, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(job.name, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
        if (job.address.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(job.address, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (job.mapsQuery.isNotEmpty()) {
                OutlinedButton(onClick = { onAction(FieldAction.Directions(job.mapsQuery)) }, modifier = Modifier.height(48.dp)) {
                    Text("Cómo llegar", fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = { onAction(if (job.registers) FieldAction.Register(job.target) else FieldAction.Open(job.target)) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            ) { Text(if (job.registers) "Registrar suscripción" else "Abrir ticket", fontWeight = FontWeight.SemiBold, maxLines = 1) }
        }
    }
}

@Composable
private fun TodayRowItem(row: TodayRow, divider: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.priority?.let { HubBadge(it) }
                Text(row.number, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                row.trailingBadge?.let { HubBadge(it) }
                row.trailing?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(row.title, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(row.subtitle, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (divider) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = IspTheme.colors.track)
    }
}

@Composable
private fun DoneToday(done: List<TodayRow>, onAction: (FieldAction) -> Unit) {
    val colors = IspTheme.colors
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp))) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { expanded = !expanded }.padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(32.dp).background(colors.successContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Hechos hoy (${done.size})", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
        }
        if (expanded) {
            if (done.isEmpty()) {
                Text("Aún no cerraste tickets hoy.", Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            done.forEachIndexed { index, row -> TodayRowItem(row, index < done.lastIndex) { onAction(FieldAction.Open(row.target)) } }
        }
    }
}

/** Sales home «Instalaciones» (a-ini-04). */
@Composable
fun SalesAgendaContent(state: FieldUiState, onFilter: (AgendaFilter) -> Unit, onAction: (FieldAction) -> Unit, onRetry: () -> Unit) {
    val agenda = state.agenda
    val colors = IspTheme.colors
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            FieldHeader("Instalaciones", null, state.initials) { onAction(FieldAction.Profile) }
            FieldStatus(state, onRetry)
            if (agenda != null) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipPill("Todas", state.filter == AgendaFilter.ALL) { onFilter(AgendaFilter.ALL) }
                    FilterChipPill(agenda.unassignedLabel, state.filter == AgendaFilter.UNASSIGNED) { onFilter(AgendaFilter.UNASSIGNED) }
                    FilterChipPill(agenda.scheduledLabel, state.filter == AgendaFilter.SCHEDULED) { onFilter(AgendaFilter.SCHEDULED) }
                }
                Text(agenda.summary, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (agenda.items.isNotEmpty()) {
                    FieldCard {
                        agenda.items.forEachIndexed { index, item ->
                            AgendaRow(item, index < agenda.items.lastIndex) { onAction(FieldAction.Open(item.target)) }
                        }
                    }
                }
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp))
                        .heightIn(min = 56.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(32.dp).background(colors.successContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(agenda.doneLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onAction(FieldAction.NewOrder) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(FieldTestTags.NEW_ORDER),
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            shape = RoundedCornerShape(16.dp),
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Nueva orden", fontWeight = FontWeight.SemiBold) },
        )
    }
}

@Composable
private fun FilterChipPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = IspTheme.colors
    Row(
        Modifier.height(36.dp)
            .background(if (selected) colors.selectedContainer else colors.card, RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) colors.selectedContainer else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onSelectedContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (selected) colors.onSelectedContainer else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AgendaRow(item: AgendaItem, divider: Boolean, onClick: () -> Unit) {
    val colors = IspTheme.colors
    Column(Modifier.fillMaxWidth().clickable(enabled = item.target is TodayTarget.Ticket, role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val (container, content) = if (item.scheduled) colors.selectedContainer to colors.onSelectedContainer else colors.warningContainer to colors.warning
                    Row(Modifier.background(container, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (item.scheduled) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(item.whenLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = content)
                    }
                    Text(item.reference, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(item.name, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                if (item.address.isNotEmpty()) Text(item.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.detail.isNotEmpty()) Text(item.detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Orders have no detail screen yet; only tickets open.
            if (item.target is TodayTarget.Ticket) Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (divider) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.track)
    }
}
