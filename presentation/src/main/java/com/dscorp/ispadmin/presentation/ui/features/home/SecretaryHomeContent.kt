package com.dscorp.ispadmin.presentation.ui.features.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme

sealed interface HomeAction {
    data object Search : HomeAction
    data object Profile : HomeAction
    data object CutCandidates : HomeAction
    data object Owing : HomeAction
    data object Tickets : HomeAction
    data class OpenSubscription(val id: Int) : HomeAction
    data class Charge(val id: Int) : HomeAction
    data object NewCharge : HomeAction
    data object NewTicket : HomeAction
    data object NewOutlay : HomeAction
    data object NewInstallationOrder : HomeAction
}

object HomeTestTags {
    const val NEW = "home_new"
    const val NEW_SHEET = "home_new_sheet"
}

/** Secretary home (a-ini-02) with the "Nuevo" shortcuts sheet (a-ini-06). */
@Composable
fun SecretaryHomeContent(state: HomeUiState, userInitials: String, onAction: (HomeAction) -> Unit, onRetry: () -> Unit) {
    var showNew by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            HomeHero(state.home, userInitials, onAction)
            when {
                state.home == null && state.loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.home == null && state.error != null -> RetryMessage(state.error, onRetry)
                state.home != null -> {
                    state.error?.let { RetryMessage(it, onRetry) }
                    HomeSections(state.home, onAction)
                }
            }
        }
        val colors = IspTheme.colors
        ExtendedFloatingActionButton(
            onClick = { showNew = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(HomeTestTags.NEW),
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            shape = RoundedCornerShape(16.dp),
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Nuevo", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) },
        )
        if (showNew) {
            NewSheet(
                onSelect = { action -> showNew = false; onAction(action) },
                onDismiss = { showNew = false },
            )
        }
    }
}

@Composable
private fun HomeHero(home: SecretaryHome?, userInitials: String, onAction: (HomeAction) -> Unit) {
    val colors = IspTheme.colors
    Column(
        Modifier.fillMaxWidth()
            .background(colors.hero, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Inicio", modifier = Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.onHero)
            Box(
                Modifier.size(36.dp).background(Color(0xFF2A3678), CircleShape).clickable(role = Role.Button) { onAction(HomeAction.Profile) },
                contentAlignment = Alignment.Center,
            ) { Text(userInitials, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onHero) }
        }
        Row(
            Modifier.fillMaxWidth().height(48.dp).background(Color.White.copy(alpha = 0.10f), CircleShape)
                .clickable(role = Role.Button) { onAction(HomeAction.Search) }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.onHeroMuted)
            Spacer(Modifier.width(10.dp))
            Text("Buscar nombre, DNI, IP o código", fontSize = 15.sp, color = colors.onHeroMuted)
        }
        if (home != null) {
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(home.todayTitle, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onHeroMuted)
                Text(home.todayAmount, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, color = colors.onHero)
                Row {
                    Text("${home.todayCount} · ", fontSize = 13.sp, color = colors.selectedContainer)
                    home.delta?.let {
                        Text(
                            "${it.text} ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (it.positive) colors.heroPositive else colors.heroNegative,
                        )
                    }
                    Text(home.deltaVs, fontSize = 13.sp, color = colors.selectedContainer)
                }
            }
        }
    }
}

@Composable
private fun RetryMessage(message: String, onRetry: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
private fun HomeSections(home: SecretaryHome, onAction: (HomeAction) -> Unit) {
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (home.attention.isNotEmpty()) {
            Section("Requiere atención") {
                home.attention.forEachIndexed { index, row ->
                    AttentionItem(row) {
                        onAction(
                            when (row.kind) {
                                AttentionKind.CUT -> HomeAction.CutCandidates
                                AttentionKind.OWING -> HomeAction.Owing
                                AttentionKind.TICKETS -> HomeAction.Tickets
                            },
                        )
                    }
                    if (index < home.attention.lastIndex) HorizontalDivider(color = IspTheme.colors.track)
                }
            }
        }
        if (home.owing.isNotEmpty()) {
            Section(home.owingTitle) {
                home.owing.forEachIndexed { index, row ->
                    OwingItem(row, onOpen = { onAction(HomeAction.OpenSubscription(row.id)) }, onCharge = { onAction(HomeAction.Charge(row.id)) })
                    if (index < home.owing.lastIndex) HorizontalDivider(color = IspTheme.colors.track)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(IspTheme.colors.card, RoundedCornerShape(16.dp))) { content() }
    }
}

@Composable
private fun AttentionItem(row: AttentionRow, onClick: () -> Unit) {
    val colors = IspTheme.colors
    val (container, content, icon) = when (row.kind) {
        AttentionKind.CUT -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, Icons.Outlined.EventBusy)
        AttentionKind.OWING -> Triple(colors.warningContainer, colors.warning, Icons.Outlined.Schedule)
        AttentionKind.TICKETS -> Triple(colors.infoContainer, colors.info, Icons.Outlined.ConfirmationNumber)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).background(container, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.title, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(row.subtitle, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OwingItem(row: OwingRow, onOpen: () -> Unit, onCharge: () -> Unit) {
    val colors = IspTheme.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable(role = Role.Button, onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(colors.selectedContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(row.initials, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSelectedContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.name, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row {
                    if (row.detail.isNotEmpty()) {
                        Text(
                            "${row.detail} · ",
                            modifier = Modifier.weight(1f, fill = false),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(row.debt, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.height(36.dp).background(colors.selectedContainer, CircleShape).clickable(role = Role.Button, onClick = onCharge)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Cobrar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSelectedContainer) }
    }
}

private data class NewOption(val action: HomeAction, val title: String, val subtitle: String, val icon: ImageVector)

private val NEW_OPTIONS = listOf(
    NewOption(HomeAction.NewCharge, "Cobro", "Buscar titular y cobrar", Icons.Outlined.Payments),
    NewOption(HomeAction.NewTicket, "Ticket", "Para un abonado o un no abonado", Icons.Outlined.ConfirmationNumber),
    NewOption(HomeAction.NewOutlay, "Egreso", "Gasto con foto del comprobante", Icons.Outlined.ReceiptLong),
    NewOption(HomeAction.NewInstallationOrder, "Orden de instalación", "Solicitud para un técnico", Icons.Outlined.Engineering),
)

/** In-screen sheet so it shares the window with the home (no popup). */
@Composable
private fun NewSheet(onSelect: (HomeAction) -> Unit, onDismiss: () -> Unit) {
    val colors = IspTheme.colors
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color(0x8510142B))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(colors.card, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .testTag(HomeTestTags.NEW_SHEET)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 32.dp, height = 4.dp).background(MaterialTheme.colorScheme.outline, CircleShape))
            }
            Text("Nuevo", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            NEW_OPTIONS.forEachIndexed { index, option ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button) { onSelect(option.action) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(40.dp).background(colors.selectedContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(option.icon, contentDescription = null, tint = colors.onSelectedContainer, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(option.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(option.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (index < NEW_OPTIONS.lastIndex) HorizontalDivider(color = colors.track)
            }
            Text(
                "Registrar suscripción está disponible para Administrador, Contador y Técnico.",
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
