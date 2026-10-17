package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.dscorp.ispadmin.domain.model.CustomerServiceSummary
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.presentation.designsystem.components.pullUp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.SubscriptionHubTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.badge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.planLabel

object FichaTestTags {
    const val BACK = "ficha_back"
    const val MORE = "ficha_more"
    fun action(action: FichaAction) = "ficha_action_${action.name.lowercase()}"
    fun tab(tab: FichaTab) = "ficha_tab_${tab.name.lowercase()}"
}

private val HERO_BOTTOM = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
private val CARD = RoundedCornerShape(16.dp)
private val DEBT_CARD_OVERLAP = 32.dp

/** Navy band of the ficha (a-sus-04). Other tabs use the compact version without the debt card. */
@Composable
fun FichaHeaderBand(header: FichaHeader, compact: Boolean, onBack: () -> Unit, onMore: (() -> Unit)?, onCharge: () -> Unit) {
    val colors = IspTheme.colors
    val showDebt = !compact && header.debt != null
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.hero, HERO_BOTTOM)
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = if (showDebt) 48.dp else 16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag(FichaTestTags.BACK)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = colors.onHero)
                }
                Spacer(Modifier.weight(1f))
                if (onMore != null) {
                    IconButton(onClick = onMore, modifier = Modifier.testTag(FichaTestTags.MORE)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Más acciones", tint = colors.onHero)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(header.name, color = colors.onHero, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(header.subtitle, color = colors.onHeroMuted, fontSize = 13.sp, lineHeight = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    header.badges.forEach { HubBadge(it) }
                }
            }
        }
        if (showDebt) {
            val debt = header.debt!!
            Surface(
                shape = CARD,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp).pullUp(DEBT_CARD_OVERLAP).fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Deuda pendiente", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(debt.amount, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                        debt.detail?.let {
                            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = onCharge,
                        modifier = Modifier.heightIn(min = 48.dp).testTag(SubscriptionHubTestTags.CHARGE),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.hero, contentColor = colors.onHero),
                    ) { Text("Cobrar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                }
            }
        }
    }
}

@Composable
fun FichaQuickActions(actions: List<FichaAction>, onAction: (FichaAction) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
        actions.forEach { action ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { onAction(action) }
                    .testTag(FichaTestTags.action(action))
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(action.icon(), contentDescription = null, modifier = Modifier.size(22.dp)) }
                Text(action.label, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

private fun FichaAction.icon(): ImageVector = when (this) {
    FichaAction.CALL -> Icons.Outlined.Phone
    FichaAction.WHATSAPP -> Icons.AutoMirrored.Outlined.Chat
    FichaAction.RESTORE -> Icons.Outlined.PowerSettingsNew
    FichaAction.TICKET -> Icons.Outlined.ConfirmationNumber
    FichaAction.MAP -> Icons.Outlined.LocationOn
}

@Composable
fun FichaTabs(selected: FichaTab, onSelect: (FichaTab) -> Unit, ticketsCount: Int? = null) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
            FichaTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val label = if (tab == FichaTab.TICKETS && ticketsCount != null && ticketsCount > 0) "${tab.label} $ticketsCount" else tab.label
                Column(
                    modifier = Modifier
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .testTag(FichaTestTags.tab(tab))
                        .padding(horizontal = 12.dp)
                        .width(IntrinsicSize.Max),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Box(
                        Modifier
                            .height(3.dp)
                            .fillMaxWidth()
                            .background(if (isSelected) IspTheme.colors.hero else Color.Transparent, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
    }
}

@Composable
fun FichaSummary(
    restoreHint: String?,
    service: List<InfoRow>,
    holder: List<InfoRow>,
    network: List<InfoRow>,
    facadePhotoUrl: String?,
    otherServices: List<CustomerServiceSummary>,
    onSeeNetwork: () -> Unit,
    onUpdatePhoto: () -> Unit,
    onOpenService: (Int) -> Unit,
    onEditHolder: (() -> Unit)? = null,
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        restoreHint?.let { FichaWarning(it) }
        InfoCard("Servicio", service)
        InfoCard("Titular", holder, action = onEditHolder?.let { "Editar titular" to it })
        InfoCard("Red", network, action = "Ver red" to onSeeNetwork)
        FichaCard(title = "Foto de fachada", action = "Actualizar foto" to onUpdatePhoto) {
            if (facadePhotoUrl.isNullOrBlank()) {
                Text(
                    "Sin foto de fachada",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            } else {
                AsyncImage(
                    model = facadePhotoUrl,
                    contentDescription = "Foto de fachada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(16.dp).fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
        }
        if (otherServices.isNotEmpty()) {
            FichaCard(title = "Otros servicios del titular") {
                otherServices.forEachIndexed { index, other ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                    OtherServiceRow(other) { onOpenService(other.id) }
                }
            }
        }
    }
}

@Composable
fun FichaWarning(text: String) {
    Row(
        Modifier.fillMaxWidth().background(IspTheme.colors.warningContainer, CARD).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = IspTheme.colors.warning)
        Text(text, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun InfoCard(title: String, rows: List<InfoRow>, action: Pair<String, () -> Unit>? = null) {
    if (rows.isEmpty() && action == null) return
    FichaCard(title, action) { InfoRows(rows) }
}

@Composable
private fun InfoRows(rows: List<InfoRow>) {
    rows.forEach { row ->
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceContainer)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(row.label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(112.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                row.badge?.let { HubBadge(it) }
                if (row.value.isNotEmpty()) {
                    Text(row.value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                }
                row.note?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
fun FichaCard(title: String, action: Pair<String, () -> Unit>? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, CARD)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            action?.let { (label, onClick) ->
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IspTheme.colors.accentInk,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick).padding(8.dp),
                )
            }
        }
        content()
    }
}

@Composable
private fun OtherServiceRow(service: CustomerServiceSummary, onClick: () -> Unit) {
    val status = runCatching { ServiceStatus.valueOf(service.serviceStatus.orEmpty()) }.getOrDefault(ServiceStatus.UNKNOWN)
    val owes = (service.totalDebt ?: 0.0) > 0.0
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(otherServiceTitle(service), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            service.address?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HubBadge(status.badge())
            Text(
                if (owes) formatSoles(service.totalDebt ?: 0.0) else "Al día",
                fontSize = 13.sp,
                fontWeight = if (owes) FontWeight.SemiBold else FontWeight.Normal,
                color = if (owes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun otherServiceTitle(service: CustomerServiceSummary): String {
    val type = when (service.installationType) {
        "FIBER" -> "Fibra"
        "WIRELESS" -> "Inalámbrico"
        "ONLY_TV_FIBER" -> "Solo TV"
        else -> null
    }
    return listOfNotNull(type, service.planName?.takeIf { it.isNotBlank() }?.let(::planLabel)).joinToString(" · ")
}


@Composable
fun FichaPaymentsTab(payments: FichaPayments, onCharge: () -> Unit, onSeeHistory: () -> Unit) {
    val colors = IspTheme.colors
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, CARD).padding(16.dp)) {
            payments.pending?.let { pending ->
                Text("Pendiente", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(pending.amount, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                pending.detail?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Button(
                    onClick = onCharge,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.hero, contentColor = colors.onHero),
                ) { Text(pending.chargeLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceContainer)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Último pago", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(payments.lastPayment ?: "Sin pagos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Recibos", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            payments.dueRule?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (payments.receipts.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, CARD)) {
                payments.receipts.forEachIndexed { index, receipt ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(receipt.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(receipt.detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            HubBadge(receipt.badge)
                            Text(
                                receipt.amount,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (receipt.overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        Text(
            "Ver pagos anteriores",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.accentInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onSeeHistory).padding(12.dp),
        )
    }
}

@Composable
fun FichaNetworkTab(network: FichaNetwork, isFiber: Boolean, onReboot: () -> Unit, onResend: () -> Unit, onSeeRegistration: () -> Unit) {
    var confirm by remember { mutableStateOf<EquipmentAction?>(null) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        network.cutNotice?.let { FichaWarning(it) }
        if (isFiber) {
            FichaCard("Equipo") {
                InfoRows(network.equipment)
                if (network.equipmentUnavailable) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceContainer)
                    Text(
                        "Sin lectura del equipo por ahora.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        InfoCard("Acceso a internet", network.access)
        if (network.fiber.isNotEmpty()) InfoCard("Fibra", network.fiber)
        if (network.wifi.isNotEmpty()) InfoCard("Wi-Fi", network.wifi)
        if (isFiber) {
            FichaCard("Acciones del equipo") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { confirm = EquipmentAction.REBOOT },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text("Reiniciar ONU", fontWeight = FontWeight.SemiBold, maxLines = 1) }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { confirm = EquipmentAction.RESEND },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text("Reenviar configuración", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Text(
                    "Pide confirmación; el abonado pierde la conexión 1–2 minutos.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Text(
                "Ver registro de la ONU",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = IspTheme.colors.accentInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onSeeRegistration).padding(12.dp),
            )
        }
    }
    confirm?.let { action ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(action.question) },
            text = { Text("El abonado pierde la conexión 1–2 minutos.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirm = null
                    if (action == EquipmentAction.REBOOT) onReboot() else onResend()
                }) { Text(action.confirm) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirm = null }) { Text("Volver") } },
        )
    }
}

private enum class EquipmentAction(val question: String, val confirm: String) {
    REBOOT("¿Reiniciar la ONU?", "Reiniciar"),
    RESEND("¿Reenviar la configuración?", "Reenviar"),
}

@Composable
fun FichaTicketsTab(tickets: FichaTickets?, onNewTicket: () -> Unit, onOpenTicket: (Int) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        androidx.compose.material3.OutlinedButton(
            onClick = onNewTicket,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text("Nuevo ticket", fontWeight = FontWeight.SemiBold) }
        when {
            tickets == null -> EmptyNote("No se pudieron cargar los tickets.")
            tickets.rows.isEmpty() -> EmptyNote("Esta suscripción no tiene tickets.")
            else -> Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, CARD)) {
                tickets.rows.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenTicket(row.id) }.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(row.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(row.detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(12.dp))
                        HubBadge(row.badge)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    )
}

/** Body of the «Más acciones» sheet (a-sus-08). */
@Composable
fun FichaMoreActionsContent(name: String, subtitle: String, sections: List<MoreSection>, onAction: (MoreAction) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(IspTheme.colors.selectedContainer, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() },
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = IspTheme.colors.onSelectedContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
        sections.forEach { section ->
            if (section.title == null) {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceContainer)
            } else {
                Text(
                    section.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            section.actions.forEach { action ->
                val destructive = action == MoreAction.CANCEL
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { onAction(action) }.heightIn(min = 52.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        action.icon(), contentDescription = null, modifier = Modifier.size(22.dp),
                        tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        action.label, fontSize = 15.sp, modifier = Modifier.weight(1f),
                        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (destructive) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (!destructive) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun MoreAction.icon(): ImageVector = when (this) {
    MoreAction.CHANGE_PLAN -> Icons.Outlined.Sync
    MoreAction.CHANGE_NAP -> Icons.Outlined.GridView
    MoreAction.MIGRATE_TO_FIBER -> Icons.Outlined.SwapHoriz
    MoreAction.UPDATE_LOCATION -> Icons.Outlined.LocationOn
    MoreAction.NEW_SERVICE -> Icons.Outlined.Add
    MoreAction.DEFER_CUT -> Icons.Outlined.Schedule
    MoreAction.EDIT_HOLDER -> Icons.Outlined.Person
    MoreAction.UPDATE_PHOTO -> Icons.Outlined.PhotoCamera
    MoreAction.CANCEL -> Icons.Outlined.Delete
    MoreAction.REACTIVATE -> Icons.Outlined.PowerSettingsNew
}
