package com.dscorp.ispadmin.presentation.ui.features.networkmap

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.outlay.ChoiceChip
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge

private val CARD = RoundedCornerShape(16.dp)

@Composable
internal fun NapLevel.color(): Color = when (this) {
    NapLevel.ROOMY -> IspTheme.colors.success
    NapLevel.FEW -> IspTheme.colors.warning
    NapLevel.FULL -> MaterialTheme.colorScheme.error
}

/** a-mas-05 «Mapa de red»: the map is a slot so tests run without Google Maps. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NetworkMapContent(
    state: NetworkMapUiState,
    onIntent: (NetworkMapIntent) -> Unit,
    onOpenBox: (Int) -> Unit,
    onDirections: (NapDetail) -> Unit,
    map: @Composable () -> Unit,
) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val networkMap = state.map
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            PlaceMenu(networkMap) { onIntent(NetworkMapIntent.PlaceSelected(it)) }
            Spacer(Modifier.width(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapLayer.entries.forEach { layer ->
                    ChoiceChip(layer.label, selected = layer in state.layers) { onIntent(NetworkMapIntent.LayerToggled(layer)) }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                networkMap == null && state.error != null -> Column(Modifier.padding(16.dp)) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    TextButton(onClick = { onIntent(NetworkMapIntent.Load) }) { Text("Reintentar") }
                }
                networkMap == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> map()
            }
            if (state.loading && networkMap != null) CircularProgressIndicator(Modifier.align(Alignment.TopCenter).padding(12.dp))
            Row(
                Modifier.align(Alignment.TopStart).padding(12.dp).background(colors.card, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NapLevel.entries.forEach { level ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(level.color(), CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(level.label, fontSize = 12.sp)
                    }
                }
            }
        }
        if (state.selectedId != null || state.selected != null) {
            val selected = state.selected
            Column(
                Modifier.fillMaxWidth().background(colors.card).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (selected == null) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selected.code, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(selected.freeLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    selected.address?.let { Text(it, fontSize = 14.sp, color = muted) }
                    if (selected.network.isNotBlank()) Text(selected.network, fontSize = 13.sp, color = muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedButton(onClick = { onOpenBox(selected.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(selected.holdersLabel, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { onDirections(selected) },
                            enabled = selected.latitude != null,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.hero, contentColor = colors.onHero),
                        ) {
                            Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cómo llegar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceMenu(map: NetworkMap?, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.heightIn(min = 36.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(enabled = map != null, role = Role.DropdownList) { open = true }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(map?.placeName ?: "Lugar", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            map?.places?.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { open = false; onSelect(id) })
            }
        }
    }
}

/** a-mas-06 «Caja NAP». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NapDetailContent(
    state: NapDetailUiState,
    onOpenSubscription: (Int) -> Unit,
    onDirections: (NapDetail) -> Unit,
    onOpenMap: (NapDetail) -> Unit,
    onRetry: () -> Unit,
) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val detail = state.detail
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            detail == null && state.error != null -> Column(Modifier.padding(16.dp)) {
                Text(state.error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                TextButton(onClick = onRetry) { Text("Reintentar") }
            }
            detail == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(detail.code, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        Text(detail.freeLabel, fontSize = 14.sp, color = muted)
                    }
                    TextButton(onClick = { onOpenMap(detail) }, enabled = detail.latitude != null) {
                        Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir mapa")
                    }
                }
                Column(Modifier.fillMaxWidth().background(colors.card, CARD).padding(horizontal = 16.dp)) {
                    detail.rows.forEachIndexed { index, (label, value) ->
                        if (index > 0) HorizontalDivider(color = colors.track)
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, fontSize = 14.sp, color = muted, modifier = Modifier.weight(1f))
                            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().background(colors.card, CARD).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Puertos", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(detail.freeLabel, fontSize = 13.sp, color = muted)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 8) {
                        detail.ports.forEach { port ->
                            val taken = port.initials != null
                            Box(
                                Modifier.weight(1f).aspectRatio(1f)
                                    .background(if (taken) colors.hero else colors.card, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (taken) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Tight line heights so number and initials fit an eighth of the row.
                                    Text("${port.number}", fontSize = 10.sp, lineHeight = 11.sp, color = if (taken) colors.onHero.copy(alpha = 0.7f) else muted)
                                    port.initials?.let { Text(it, fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onHero) }
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LegendDot(colors.card, "Libre", border = MaterialTheme.colorScheme.outlineVariant)
                        LegendDot(colors.hero, "Ocupado (iniciales del titular)")
                    }
                }
                Text("Abonados en esta caja (${detail.holders.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Column(Modifier.fillMaxWidth().background(colors.card, CARD)) {
                    if (detail.holders.isEmpty()) Text("Nadie conectado a esta caja.", Modifier.padding(16.dp), fontSize = 14.sp, color = muted)
                    detail.holders.forEachIndexed { index, holder ->
                        if (index > 0) HorizontalDivider(Modifier.padding(start = 64.dp), color = colors.track)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onOpenSubscription(holder.subscriptionId) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.size(36.dp).background(colors.selectedContainer, CircleShape), contentAlignment = Alignment.Center) {
                                Text(holder.initials, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSelectedContainer)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(holder.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(holder.detail, fontSize = 13.sp, color = muted)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                HubBadge(holder.status)
                                holder.debt?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = muted)
                        }
                    }
                }
                Button(
                    onClick = { onDirections(detail) },
                    enabled = detail.latitude != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.hero, contentColor = colors.onHero),
                ) {
                    Icon(Icons.Rounded.Directions, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cómo llegar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, border: Color = Color.Transparent) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)).border(1.dp, border, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
