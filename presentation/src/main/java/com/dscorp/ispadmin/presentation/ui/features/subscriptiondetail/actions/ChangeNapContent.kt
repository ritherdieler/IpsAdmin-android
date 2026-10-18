package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge

object ChangeNapTestTags {
    const val SUBMIT = "change_nap_submit"
    fun port(number: Int) = "change_nap_port_$number"
}

private val CARD = RoundedCornerShape(16.dp)
private const val PORTS_PER_ROW = 8
private const val ROWS_PREVIEW = 8

/** a-sus-10 «Cambiar caja NAP». */
@Composable
fun ChangeNapContent(state: ChangeNapUiState, onIntent: (ChangeNapIntent) -> Unit, onBack: () -> Unit, onRetry: () -> Unit = {}) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val model = state.model
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ActionHeader("Cambiar caja NAP", onBack)
        Box(Modifier.weight(1f)) {
            when {
                model == null && state.error != null -> ActionLoadError(state.error, onRetry)
                model == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(Modifier.fillMaxWidth().background(colors.selectedContainer, CARD).padding(16.dp)) {
                        Text("Actual", fontSize = 13.sp, color = muted)
                        Text(model.current, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        model.currentAddress?.let { Text(it, fontSize = 13.sp, color = muted) }
                    }
                    SortTabs(state.sort) { onIntent(ChangeNapIntent.SortSelected(it)) }
                    Text(model.header, fontSize = 13.sp, color = muted)
                    val all = model.rows(state.sort)
                    // Places have dozens of boxes: the nearest few first so the port grid stays close.
                    var showAll by rememberSaveable(state.sort) { mutableStateOf(false) }
                    val rows = if (showAll) all else all.take(ROWS_PREVIEW)
                    Column(Modifier.fillMaxWidth().background(colors.card, CARD)) {
                        if (rows.isEmpty()) Text("No hay cajas en esta vista.", Modifier.padding(16.dp), fontSize = 14.sp, color = muted)
                        rows.forEachIndexed { index, row ->
                            NapRowItem(row, selected = row.id == state.napId) { onIntent(ChangeNapIntent.NapSelected(row.id)) }
                            if (index < rows.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.track)
                        }
                        if (all.size > rows.size) {
                            TextButton(onClick = { showAll = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Ver las ${all.size} cajas", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    state.napId?.let { napId -> PortGrid(model, napId, state.port) { onIntent(ChangeNapIntent.PortSelected(it)) } }
                }
            }
        }
        if (model != null) {
            ActionFooter(
                label = state.action,
                enabled = state.canSubmit,
                busy = state.saving,
                onClick = { onIntent(ChangeNapIntent.Submit) },
                tag = ChangeNapTestTags.SUBMIT,
                note = if (state.napId == null) "Elige la caja nueva" else model.releaseNote,
                error = state.error,
            )
        }
    }
}

@Composable
private fun SortTabs(sort: NapSort, onSelect: (NapSort) -> Unit) {
    val colors = IspTheme.colors
    Row(Modifier.fillMaxWidth().background(colors.track, RoundedCornerShape(12.dp)).padding(4.dp)) {
        NapSort.entries.forEach { option ->
            val selected = option == sort
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp)
                    .background(if (selected) colors.card else Color.Transparent, RoundedCornerShape(10.dp))
                    .selectable(selected = selected, role = Role.Tab) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(option.label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun NapRowItem(row: NapRow, selected: Boolean, onClick: () -> Unit) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .selectable(selected = selected, enabled = row.enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = row.enabled, colors = RadioButtonDefaults.colors(selectedColor = colors.hero))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.code, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (row.enabled) Color.Unspecified else muted)
                row.distance?.let { Text(it, fontSize = 13.sp, color = muted) }
            }
            row.address?.let { Text(it, fontSize = 13.sp, color = muted) }
        }
        HubBadge(StatusBadge(row.freeLabel, if (row.enabled) BadgeTone.SUCCESS else BadgeTone.NEUTRAL))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortGrid(model: ChangeNap, napId: Int, chosen: Int?, onSelect: (Int) -> Unit) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().background(colors.card, CARD).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Puerto en ${model.code(napId)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(model.freeOf(napId), fontSize = 13.sp, color = muted)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            maxItemsInEachRow = PORTS_PER_ROW,
        ) {
            model.ports(napId, chosen).forEach { cell ->
                val (background, content) = when (cell.state) {
                    PortState.TAKEN -> colors.track to muted
                    PortState.FREE -> colors.card to MaterialTheme.colorScheme.onSurface
                    PortState.CHOSEN -> colors.hero to colors.onHero
                }
                Box(
                    Modifier.weight(1f).aspectRatio(1f)
                        .background(background, RoundedCornerShape(8.dp))
                        .border(1.dp, if (cell.state == PortState.FREE) MaterialTheme.colorScheme.outlineVariant else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable(enabled = cell.state != PortState.TAKEN, role = Role.RadioButton) { onSelect(cell.number) }
                        .testTag(ChangeNapTestTags.port(cell.number)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${cell.number}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = content)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(colors.track, "Ocupado")
            Legend(colors.card, "Libre", border = MaterialTheme.colorScheme.outlineVariant)
            Legend(colors.hero, "Elegido")
        }
    }
}

@Composable
private fun Legend(color: Color, label: String, border: Color = Color.Transparent) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)).border(1.dp, border, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
