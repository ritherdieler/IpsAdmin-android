package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.home.weekdayDayMonth
import com.dscorp.ispadmin.presentation.ui.features.outlay.ChoiceChip
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import java.time.LocalDate

object DeferCutTestTags {
    const val SUBMIT = "defer_cut_submit"
}

private val CARD = RoundedCornerShape(16.dp)

/** a-sus-12 «Postergar corte». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeferCutContent(state: DeferCutUiState, onIntent: (DeferCutIntent) -> Unit, onBack: () -> Unit, onPickDate: () -> Unit, onRetry: () -> Unit = {}) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val model = state.model
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ActionHeader("Postergar corte", onBack)
        Box(Modifier.weight(1f)) {
            when {
                model == null && state.error != null -> ActionLoadError(state.error, onRetry)
                model == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(Modifier.fillMaxWidth().background(colors.card, CARD).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(model.header.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(model.header.subtitle, fontSize = 13.sp, color = muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HubBadge(model.header.status)
                            model.header.debtBadge?.let { HubBadge(it) }
                        }
                        Text(model.summary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        model.currentNote?.let {
                            Text(it, fontSize = 13.sp, color = colors.info, modifier = Modifier.fillMaxWidth()
                                .background(colors.infoContainer, RoundedCornerShape(10.dp)).padding(10.dp))
                        }
                    }
                    Text("Hasta cuándo", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (model.options.isEmpty() && model.pickRange == null) {
                        Text("Hoy es el último día del mes: la postergación debe quedar dentro del mes en curso.", fontSize = 14.sp, color = muted)
                    }
                    val picked = state.date?.takeIf { date -> model.options.none { it.date == date } }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
                        model.options.forEach { option ->
                            DateTile(option.title, option.hint, selected = option.date == state.date, modifier = Modifier.weight(1f)) {
                                onIntent(DeferCutIntent.DateSelected(option.date))
                            }
                        }
                        if (model.pickRange != null) {
                            DateTile(
                                title = picked?.let { weekdayDayMonth(it).replaceFirstChar { c -> c.uppercase() } } ?: "Elegir fecha",
                                hint = if (picked != null) "elegida" else "del mes en curso",
                                selected = picked != null,
                                icon = true,
                                modifier = Modifier.weight(1f),
                                onClick = onPickDate,
                            )
                        }
                    }
                    Text("Motivo", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DEFER_REASONS.forEach { reason ->
                            ChoiceChip(reason, selected = reason == state.reason) { onIntent(DeferCutIntent.ReasonSelected(reason)) }
                        }
                    }
                    OutlinedTextField(
                        value = state.detail,
                        onValueChange = { onIntent(DeferCutIntent.DetailChanged(it)) },
                        label = { Text("Detalle (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
            }
        }
        if (model != null) {
            val date = state.date
            ActionFooter(
                label = date?.let(model::action) ?: "Postergar",
                enabled = state.canSubmit,
                busy = state.saving,
                onClick = { onIntent(DeferCutIntent.Submit) },
                tag = DeferCutTestTags.SUBMIT,
                note = when {
                    date == null -> "Elige hasta cuándo"
                    state.reason == null -> "Elige el motivo"
                    else -> model.note(date)
                },
                error = state.error,
            )
        }
    }
}

@Composable
private fun DateTile(title: String, hint: String, selected: Boolean, modifier: Modifier = Modifier, icon: Boolean = false, onClick: () -> Unit) {
    val colors = IspTheme.colors
    Row(
        modifier.heightIn(min = 60.dp)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(if (selected) colors.selectedContainer else colors.card, RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (icon) Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
