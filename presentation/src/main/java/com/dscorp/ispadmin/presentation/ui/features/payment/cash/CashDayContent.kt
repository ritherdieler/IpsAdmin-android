package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.designsystem.components.HERO_OVERLAP
import com.dscorp.ispadmin.presentation.designsystem.components.pullUp
import com.dscorp.ispadmin.presentation.theme.IspTheme

enum class CashPeriod { TODAY, YESTERDAY, PICK }

sealed interface CashAction {
    data object Search : CashAction
    data object NewCharge : CashAction
    data object Retry : CashAction
    data class Period(val period: CashPeriod) : CashAction
    data class OpenSubscription(val id: Int) : CashAction
}

object CashDayTestTags {
    const val NEW_CHARGE = "cash_day_new_charge"
    const val PERIOD = "cash_day_period"
}

private const val ROWS_PREVIEW = 5

/** Caja (a-caj-01). */
@Composable
fun CashDayContent(state: CashDayUiState, onAction: (CashAction) -> Unit) {
    val colors = IspTheme.colors
    val day = state.day
    var showAll by rememberSaveable(day?.date) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            Column(
                Modifier.fillMaxWidth().background(colors.hero, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = if (day?.methods?.isNotEmpty() == true) 56.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Caja", Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.onHero)
                    PeriodChip(isToday = day?.title?.startsWith("Cobrado hoy") != false, onAction = onAction)
                }
                if (day != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(day.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onHeroMuted)
                        Text(day.amount, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, color = colors.onHero)
                        Text(day.subtitle, fontSize = 13.sp, color = colors.selectedContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            when {
                day == null && state.loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.error, Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { onAction(CashAction.Retry) }) { Text("Reintentar") }
                }
            }
            if (day != null) {
                if (day.methods.isNotEmpty()) MethodsCard(day.methods)
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth().height(48.dp).clip(CircleShape)
                        .background(colors.track).clickable(role = Role.Button) { onAction(CashAction.Search) }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    Text("Buscar pago, titular o pagador", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (day.title.startsWith("Cobrado hoy")) "Cobros de hoy" else "Cobros del día", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (day.rows.size > ROWS_PREVIEW) {
                        TextButton(onClick = { showAll = !showAll }) {
                            Text(if (showAll) "Ver menos" else "Ver los ${day.rows.size}", fontWeight = FontWeight.SemiBold, color = colors.accentInk)
                        }
                    }
                }
                if (day.rows.isEmpty()) {
                    Text("Aún no hay cobros este día.", Modifier.padding(horizontal = 16.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val shown = if (showAll) day.rows else day.rows.take(ROWS_PREVIEW)
                    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp))) {
                        shown.forEachIndexed { index, row ->
                            CashRowItem(row) { row.subscriptionId?.let { onAction(CashAction.OpenSubscription(it)) } }
                            if (index < shown.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.track)
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onAction(CashAction.NewCharge) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(CashDayTestTags.NEW_CHARGE),
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            shape = RoundedCornerShape(16.dp),
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Nuevo cobro", fontWeight = FontWeight.SemiBold) },
        )
    }
}

@Composable
private fun PeriodChip(isToday: Boolean, onAction: (CashAction) -> Unit) {
    val colors = IspTheme.colors
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.height(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                .clickable(role = Role.Button) { open = true }.testTag(CashDayTestTags.PERIOD).padding(start = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (isToday) "Hoy" else "Otro día", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onHero)
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = colors.onHero, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Hoy") }, onClick = { open = false; onAction(CashAction.Period(CashPeriod.TODAY)) })
            DropdownMenuItem(text = { Text("Ayer") }, onClick = { open = false; onAction(CashAction.Period(CashPeriod.YESTERDAY)) })
            DropdownMenuItem(text = { Text("Elegir fecha…") }, onClick = { open = false; onAction(CashAction.Period(CashPeriod.PICK)) })
        }
    }
}

@Composable
private fun MethodsCard(methods: List<CashMethod>) {
    val colors = IspTheme.colors
    val palette = colors.chartCategorical
    Column(
        Modifier.pullUp(HERO_OVERLAP + 4.dp).padding(horizontal = 16.dp).fillMaxWidth()
            .background(colors.card, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Por método", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            methods.forEachIndexed { index, method ->
                if (method.share > 0f) Box(Modifier.weight(method.share).height(12.dp).background(palette[index % palette.size]))
            }
        }
        Column {
            methods.forEachIndexed { index, method ->
                Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(palette[index % palette.size], RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(10.dp))
                    Text(method.label, Modifier.weight(1f), fontSize = 14.sp)
                    Text(method.amount, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(method.count, Modifier.width(48.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun CashRowItem(row: CashRow, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(enabled = row.subscriptionId != null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.time, Modifier.width(44.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(row.detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(row.amount, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
