package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FichaCard
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.InfoCard
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.dayMonth
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.TicketAction
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class DayOption(val label: String, val day: LocalDate)
data class HourOption(val label: String, val time: LocalTime)

/** Visit slots offered when rescheduling (a-tic-07). */
val RESCHEDULE_HOURS: List<HourOption> = listOf(9, 11, 14, 16).map { HourOption("%02d:00".format(it), LocalTime.of(it, 0)) }

fun rescheduleDays(today: LocalDate): List<DayOption> = listOf(
    DayOption("Hoy", today),
    DayOption("Mañana ${weekday(today.plusDays(1))} ${today.plusDays(1).dayOfMonth}", today.plusDays(1)),
    today.plusDays(2).let { DayOption("${weekday(it).replaceFirstChar { c -> c.uppercase() }} ${dayMonth(it)}", it) },
)

fun rescheduleLabel(day: LocalDate, time: LocalTime, today: LocalDate): String {
    val whenLabel = when (day) {
        today -> "hoy"
        today.plusDays(1) -> "${weekday(day)} ${day.dayOfMonth}"
        else -> "${weekday(day)} ${dayMonth(day)}"
    }
    return "Reagendar para $whenLabel · %02d:%02d".format(time.hour, time.minute)
}

private fun weekday(day: LocalDate): String = when (day.dayOfWeek) {
    DayOfWeek.MONDAY -> "lun"
    DayOfWeek.TUESDAY -> "mar"
    DayOfWeek.WEDNESDAY -> "mié"
    DayOfWeek.THURSDAY -> "jue"
    DayOfWeek.FRIDAY -> "vie"
    DayOfWeek.SATURDAY -> "sáb"
    DayOfWeek.SUNDAY -> "dom"
}

private val CARD = RoundedCornerShape(16.dp)

/** Ticket detail (a-tic-02 pending, a-tic-03 in progress). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailContent(
    state: TicketDetailUiState,
    onIntent: (TicketDetailIntent) -> Unit,
    onBack: () -> Unit,
    onCharge: () -> Unit,
    onOpenSubscription: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onClose: () -> Unit,
    onOpenTicket: (Int) -> Unit,
) {
    val detail = state.detail
    var reschedule by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(detail?.title.orEmpty(), canCancel = detail?.canCancel == true, canReschedule = detail?.canReschedule == true && detail.primary != TicketAction.CLOSE,
            onBack = onBack, onCancel = { confirmCancel = true }, onReschedule = { reschedule = true })
        when {
            detail == null && state.error != null -> Text(state.error, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            detail == null -> Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() }
            else -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Header(detail)
                    detail.debtNotice?.let { DebtCard(it, onCharge) }
                    Subscriber(detail, onOpenSubscription)
                    Description(detail, onCall, onWhatsApp)
                    state.diagnosis?.let { Diagnosis(it, onIntent) }
                    Assignment(detail, onAssign = { onIntent(TicketDetailIntent.OpenAssign) })
                    if (detail.previous.isNotEmpty()) Previous(detail, onOpenTicket)
                    Spacer(Modifier.size(8.dp))
                }
                BottomActions(detail, busy = state.busy, onTake = { onIntent(TicketDetailIntent.Take) }, onReschedule = { reschedule = true }, onClose = onClose)
            }
        }
    }
    state.technicians?.let { technicians ->
        ModalBottomSheet(onDismissRequest = { onIntent(TicketDetailIntent.CloseAssign) }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            TechnicianList(technicians) { onIntent(TicketDetailIntent.Assign(it)) }
        }
    }
    if (reschedule && detail != null) {
        RescheduleSheet(detail, onDismiss = { reschedule = false }) { millis ->
            reschedule = false
            onIntent(TicketDetailIntent.Reschedule(millis))
        }
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("¿Cancelar el ticket?") },
            text = { Text("El ticket se cierra sin atender.") },
            confirmButton = { TextButton(onClick = { confirmCancel = false; onIntent(TicketDetailIntent.Cancel) }) { Text("Cancelar ticket") } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Volver") } },
        )
    }
}

/** Body of the «Asignar a» sheet. */
@Composable
fun TechnicianList(technicians: List<TechnicianOption>, onAssign: (Int) -> Unit) {
    Column(Modifier.padding(bottom = 24.dp)) {
        Text("Asignar a", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (technicians.isEmpty()) Text("No hay técnicos disponibles.", Modifier.padding(16.dp))
        technicians.forEach { tech ->
            Row(
                Modifier.fillMaxWidth().clickable { onAssign(tech.id) }.heightIn(min = 52.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Person, contentDescription = null)
                Spacer(Modifier.width(16.dp))
                Text(tech.name, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun TopBar(title: String, canCancel: Boolean, canReschedule: Boolean, onBack: () -> Unit, onCancel: () -> Unit, onReschedule: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás") }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (canCancel || canReschedule) {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "Más acciones del ticket") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (canReschedule) DropdownMenuItem(text = { Text("Reagendar") }, onClick = { menu = false; onReschedule() })
                    if (canCancel) DropdownMenuItem(text = { Text("Cancelar ticket", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onCancel() })
                }
            }
        }
    }
}

@Composable
private fun Header(detail: TicketDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { detail.badges.forEach { HubBadge(it) } }
        Text(detail.category, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (detail.meta.startsWith("Tomado")) Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(detail.meta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DebtCard(notice: DebtNotice, onCharge: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(IspTheme.colors.warningContainer, CARD).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = IspTheme.colors.warning)
            Text(notice.text, fontSize = 14.sp, lineHeight = 20.sp)
        }
        Button(
            onClick = onCharge,
            modifier = Modifier.padding(start = 36.dp).heightIn(min = 44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IspTheme.colors.selectedContainer, contentColor = IspTheme.colors.onSelectedContainer),
        ) { Text(notice.chargeLabel, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun Subscriber(detail: TicketDetail, onOpen: () -> Unit) {
    val card = detail.subscriber ?: return
    FichaCard("Abonado") {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(IspTheme.colors.selectedContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(card.initials, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = IspTheme.colors.onSelectedContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(card.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                HubBadge(card.status)
                Text(card.debt, fontSize = 13.sp, color = if (card.owes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Abrir ficha", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Description(detail: TicketDetail, onCall: () -> Unit, onWhatsApp: () -> Unit) {
    FichaCard("Descripción") {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(detail.description, fontSize = 15.sp, lineHeight = 21.sp)
            detail.contact?.let { contact ->
                Text(contact, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Llamar")
                    }
                    OutlinedButton(onClick = onWhatsApp, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("WhatsApp")
                    }
                }
            }
        }
    }
}

@Composable
private fun Diagnosis(diagnosis: TicketDiagnosis, onIntent: (TicketDetailIntent) -> Unit) {
    var confirm by remember { mutableStateOf<TicketDetailIntent?>(null) }
    InfoCard("Diagnóstico rápido", diagnosis.rows)
    if (diagnosis.unavailable) {
        Text("Sin lectura del equipo por ahora.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { confirm = TicketDetailIntent.RebootOnu }, modifier = Modifier.weight(1f).heightIn(min = 44.dp)) { Text("Reiniciar ONU", maxLines = 1) }
        OutlinedButton(onClick = { confirm = TicketDetailIntent.ResendConfig }, modifier = Modifier.weight(1f).heightIn(min = 44.dp)) {
            Text("Reenviar configuración", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    confirm?.let { intent ->
        val reboot = intent == TicketDetailIntent.RebootOnu
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (reboot) "¿Reiniciar la ONU?" else "¿Reenviar la configuración?") },
            text = { Text("El abonado pierde la conexión 1–2 minutos.") },
            confirmButton = { TextButton(onClick = { confirm = null; onIntent(intent) }) { Text(if (reboot) "Reiniciar" else "Reenviar") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Volver") } },
        )
    }
}

@Composable
private fun Assignment(detail: TicketDetail, onAssign: () -> Unit) {
    FichaCard("Asignación", action = if (detail.canAssign) "Asignar" to onAssign else null) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(detail.assignment, fontSize = 15.sp)
        }
    }
}

@Composable
private fun Previous(detail: TicketDetail, onOpenTicket: (Int) -> Unit) {
    FichaCard("Tickets anteriores") {
        detail.previous.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
            Row(Modifier.fillMaxWidth().clickable { onOpenTicket(row.id) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(row.detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HubBadge(row.badge)
            }
        }
    }
}

@Composable
private fun BottomActions(detail: TicketDetail, busy: Boolean, onTake: () -> Unit, onReschedule: () -> Unit, onClose: () -> Unit) {
    val primary = detail.primary ?: return
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        detail.primaryHint?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        val navy = ButtonDefaults.buttonColors(containerColor = IspTheme.colors.hero, contentColor = IspTheme.colors.onHero)
        when (primary) {
            TicketAction.TAKE -> Button(onClick = onTake, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), colors = navy) {
                Text("Tomar ticket", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            TicketAction.CLOSE -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (detail.canReschedule) {
                    OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Reagendar", fontWeight = FontWeight.SemiBold) }
                }
                Button(onClick = onClose, enabled = !busy, modifier = Modifier.weight(1.4f).heightIn(min = 52.dp), colors = navy) {
                    Text("Resolver y cerrar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            else -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RescheduleSheet(detail: TicketDetail, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val today = remember { LocalDate.now(FICHA_ZONE) }
    val days = remember(today) { rescheduleDays(today) }
    var day by remember { mutableStateOf(days[1]) }
    var hour by remember { mutableStateOf(RESCHEDULE_HOURS[1]) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reagendar #${detail.id}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(listOfNotNull(detail.category, detail.subscriber?.name).joinToString(" · "), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Día", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { option -> Chip(option.label, option == day) { day = option } }
            }
            Text("Hora", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RESCHEDULE_HOURS.forEach { option -> Chip(option.label, option == hour) { hour = option } }
            }
            Button(
                onClick = { onConfirm(day.day.atTime(hour.time).atZone(FICHA_ZONE).toInstant().toEpochMilli()) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IspTheme.colors.hero, contentColor = IspTheme.colors.onHero),
            ) { Text(rescheduleLabel(day.day, hour.time, today), fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label, fontSize = 13.sp) }, shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IspTheme.colors.selectedContainer, selectedLabelColor = IspTheme.colors.onSelectedContainer),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
    )
}

