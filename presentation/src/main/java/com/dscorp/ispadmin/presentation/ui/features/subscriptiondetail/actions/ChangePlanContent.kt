package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge

object ChangePlanTestTags {
    const val SUBMIT = "change_plan_submit"
}

private val CARD = RoundedCornerShape(16.dp)

/** a-sus-09 «Cambiar plan». */
@Composable
fun ChangePlanContent(state: ChangePlanUiState, onIntent: (ChangePlanIntent) -> Unit, onBack: () -> Unit) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val model = state.model
    val change = state.change
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ActionHeader("Cambiar plan", onBack)
        Box(Modifier.weight(1f)) {
            when {
                model == null && state.error != null -> Text(state.error, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                model == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(Modifier.fillMaxWidth().background(colors.selectedContainer, CARD).padding(16.dp)) {
                        Text("Plan actual", fontSize = 13.sp, color = muted)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Text(model.current.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text(model.current.detail, fontSize = 13.sp, color = muted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(model.current.price, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text("al mes", fontSize = 12.sp, color = muted)
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(model.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(model.note, fontSize = 13.sp, color = muted)
                    }
                    Column(Modifier.fillMaxWidth().background(colors.card, CARD)) {
                        model.options.forEachIndexed { index, option ->
                            val selected = option.id == state.selectedId
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                    .selectable(selected = selected, role = Role.RadioButton) { onIntent(ChangePlanIntent.Select(option.id)) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = colors.hero))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(option.name, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                                        if (option.current) HubBadge(StatusBadge("Actual", BadgeTone.NEUTRAL))
                                    }
                                    Text(option.detail, fontSize = 13.sp, color = muted)
                                }
                                Text(option.price, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (index < model.options.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.track)
                        }
                    }
                }
            }
        }
        if (model != null) {
            ActionFooter(
                label = change?.action ?: "Cambiar plan",
                enabled = change != null,
                busy = state.saving,
                onClick = { onIntent(ChangePlanIntent.Submit) },
                tag = ChangePlanTestTags.SUBMIT,
                note = if (change == null) "Elige el plan nuevo" else null,
                error = state.error,
                summary = {
                    change?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Nueva mensualidad", fontSize = 14.sp, color = muted, modifier = Modifier.weight(1f))
                            Text(it.price, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(" · ", color = muted)
                            Text(it.delta, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(" al mes", fontSize = 13.sp, color = muted)
                        }
                    }
                },
            )
        }
    }
}
