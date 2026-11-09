package com.dscorp.ispadmin.presentation.ui.features.outlay

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.domain.model.OutlayType
import com.dscorp.ispadmin.presentation.designsystem.components.BadgeTone
import com.dscorp.ispadmin.presentation.designsystem.components.ProgressTrack
import com.dscorp.ispadmin.presentation.designsystem.components.SegmentedChoice
import com.dscorp.ispadmin.presentation.designsystem.components.StatusBadge
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import kotlin.math.abs
import kotlin.math.roundToInt

private val TYPE_FILTERS = listOf<OutlayType?>(null, OutlayType.VARIABLE, OutlayType.FIXED)
private val TYPE_LABELS = listOf("Todos", "Variables", "Fijos")

/** Egresos (a-caj-08): month, the cap of variable outlays and the fixed total, type filter and the list. */
@Composable
fun OutlaysContent(
    state: OutlaysUiState,
    onNew: () -> Unit,
    onRetry: () -> Unit,
    onFilterType: (OutlayType?) -> Unit = {},
    onOpen: (Int) -> Unit = {},
    snackbar: AnnotatedString? = null,
    onSnackbarDismiss: () -> Unit = {},
    highlightId: Int? = null,
) {
    val colors = IspTheme.colors
    val month = state.month
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            when {
                month == null && state.loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.error, Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Reintentar") }
                }
            }
            if (month != null) {
                Text(
                    month.title,
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 13.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(Modifier.padding(horizontal = 16.dp)) {
                    when {
                        state.summary == null -> TotalCard(month)
                        state.typeFilter == OutlayType.FIXED -> FixedCard(state.summary, month)
                        else -> LimitCard(state.summary, month)
                    }
                }
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    SegmentedChoice(TYPE_LABELS, TYPE_FILTERS.indexOf(state.typeFilter), { onFilterType(TYPE_FILTERS[it]) })
                }
                if (month.rows.isEmpty()) {
                    OutlaysEmpty(
                        fixed = state.typeFilter == OutlayType.FIXED,
                        monthTitle = month.title,
                        showAction = state.typeFilter != OutlayType.FIXED || state.canRegisterFixed,
                        onNew = onNew,
                    )
                } else {
                    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(IspShapes.card).background(colors.card)) {
                        month.rows.forEachIndexed { index, row ->
                            OutlayRowItem(row, highlighted = row.id != null && row.id == highlightId, onClick = { row.id?.let(onOpen) })
                            if (index < month.rows.lastIndex) HorizontalDivider(color = colors.track)
                        }
                    }
                }
            }
        }
        if (snackbar != null) {
            OutlaySnackbar(
                snackbar,
                onSnackbarDismiss,
                Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            )
        } else {
            ExtendedFloatingActionButton(
                onClick = onNew,
                modifier = Modifier.align(Alignment.BottomEnd).padding(start = 16.dp, end = 16.dp, bottom = 24.dp).testTag(OutlayTestTags.NEW),
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                shape = IspShapes.card,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Nuevo egreso", fontWeight = FontWeight.SemiBold) },
            )
        }
    }
}


/** Variable outlays against their monthly cap, plus the fixed total (design screen 1). */
@Composable
private fun LimitCard(summary: OutlaySummary, month: OutlayMonth) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val (badge, tone, bar) = when (summary.level) {
        LimitLevel.InRange -> Triple("En rango", BadgeTone.Neutral, MaterialTheme.colorScheme.primary)
        LimitLevel.NearLimit -> Triple("Cerca del límite", BadgeTone.Warning, colors.warningBar)
        LimitLevel.Reached -> Triple("Límite alcanzado", BadgeTone.Error, MaterialTheme.colorScheme.error)
    }
    Column(Modifier.fillMaxWidth().background(colors.card, IspShapes.card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Variables", style = IspType.label, color = muted)
            StatusBadge(badge, tone)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatSoles(summary.spent), style = IspType.kpi)
            Spacer(Modifier.width(6.dp))
            Text("de ${formatSoles(summary.limit)}", fontSize = 14.sp, lineHeight = 20.sp, color = muted)
        }
        ProgressTrack(summary.usedFraction.toFloat(), bar, height = 8.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${(summary.usedFraction * 100).roundToInt()}% usado · ${count(month.variableCount)}", style = IspType.rowLabel.copy(fontSize = 12.sp, lineHeight = 16.sp), color = muted)
            Text("Disponible ${formatSoles(summary.available)}", style = IspType.rowLabel.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold))
        }
        HorizontalDivider(color = colors.track)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Fijos", style = IspType.label, color = muted)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatSoles(summary.fixedTotal), fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(" · ${count(month.fixedCount)}", fontSize = 12.sp, lineHeight = 22.sp, color = muted)
                }
            }
            StatusBadge("Sin límite", BadgeTone.Info)
        }
    }
}

/** The fixed outlays of the month against the month before (design screen 7). */
@Composable
private fun FixedCard(summary: OutlaySummary, month: OutlayMonth) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().background(colors.card, IspShapes.card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Fijos", style = IspType.label, color = muted)
            StatusBadge("Sin límite", BadgeTone.Info)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatSoles(summary.fixedTotal), style = IspType.kpi)
            Spacer(Modifier.width(6.dp))
            Text("· ${count(month.fixedCount)}", fontSize = 14.sp, lineHeight = 20.sp, color = muted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${month.previousTitle}: ${formatSoles(summary.previousFixedTotal)}", style = IspType.rowLabel.copy(fontSize = 12.sp, lineHeight = 16.sp), color = muted)
            fixedChange(summary)?.let { Text(it, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = muted) }
        }
    }
}

private fun count(amount: Int) = "$amount ${if (amount == 1) "egreso" else "egresos"}"

/** "▲ S/ 50.00 (1.2%)" against last month's fixed total, with no percentage when there was none. */
private fun fixedChange(summary: OutlaySummary): String? {
    val change = summary.fixedChange
    if (change == 0.0) return null
    val arrow = if (change > 0) "▲" else "▼"
    val percent = if (summary.previousFixedTotal > 0.0) " (${"%.1f".format(java.util.Locale.US, abs(change) / summary.previousFixedTotal * 100)}%)" else ""
    return "$arrow ${formatSoles(abs(change))}$percent"
}

/** Fallback when the Core has no summary endpoint: the month total and its split by category. */
@Composable
private fun TotalCard(month: OutlayMonth) {
    Column(
        Modifier.fillMaxWidth().background(IspTheme.colors.card, IspShapes.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(month.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(month.total, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(month.count, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        }
        if (month.breakdown.isNotBlank()) Text(month.breakdown, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OutlayRowItem(row: OutlayRow, highlighted: Boolean, onClick: () -> Unit) {
    val colors = IspTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .background(if (highlighted) colors.selectedContainer.copy(alpha = 0.4f) else colors.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(if (row.isFixed) colors.infoContainer else colors.track, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                categoryIcon(row.category, row.isFixed),
                contentDescription = null,
                tint = if (row.isFixed) colors.info else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.description, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(row.detail, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Text(row.amount, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal fun categoryIcon(category: String?, fixed: Boolean): ImageVector = when (category) {
    "Transporte" -> Icons.Rounded.TwoWheeler
    "Suministros" -> Icons.Rounded.Inventory2
    "Alquiler" -> Icons.Rounded.Home
    "Servicios" -> if (fixed) Icons.Rounded.Wifi else Icons.Rounded.Bolt
    "Mantenimiento" -> Icons.Rounded.Build
    "Equipos" -> Icons.Rounded.Router
    "Planilla" -> Icons.Rounded.Groups
    "Impuestos" -> Icons.Rounded.AccountBalance
    else -> if (fixed) Icons.Rounded.Event else Icons.Rounded.Receipt
}

/** Nothing to list (design screen 9): the action only appears when this profile can register that type. */
@Composable
private fun OutlaysEmpty(fixed: Boolean, monthTitle: String, showAction: Boolean, onNew: () -> Unit) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).background(colors.infoContainer, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Icon(if (fixed) Icons.Rounded.Event else Icons.Rounded.Receipt, contentDescription = null, tint = colors.info, modifier = Modifier.size(30.dp))
        }
        Text(
            if (fixed) "Sin egresos fijos en $monthTitle" else "Sin egresos en $monthTitle",
            fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        )
        Text(
            if (fixed) "Registra aquí el alquiler, el tránsito IP y los demás gastos de cada mes." else "Los egresos que registres aparecerán aquí.",
            modifier = Modifier.widthIn(max = 260.dp),
            fontSize = 14.sp, lineHeight = 20.sp, color = muted, textAlign = TextAlign.Center,
        )
        if (showAction) {
            Box(
                Modifier.padding(top = 8.dp).height(48.dp).clip(IspShapes.pill).background(colors.selectedContainer)
                    .clickable(role = Role.Button, onClick = onNew).padding(horizontal = 20.dp).testTag(OutlayTestTags.EMPTY_ACTION),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (fixed) "Registrar egreso fijo" else "Registrar egreso", color = colors.onSelectedContainer, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
