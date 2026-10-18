package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme

object UpdateLocationTestTags {
    const val SUBMIT = "update_location_submit"
}

/** a-sus-11 «Actualizar ubicación»: the map (a slot, so tests run without Google Maps) with a fixed center pin. */
@Composable
fun UpdateLocationContent(
    state: UpdateLocationUiState,
    onIntent: (UpdateLocationIntent) -> Unit,
    onBack: () -> Unit,
    onMyLocation: () -> Unit,
    map: @Composable () -> Unit,
) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ActionHeader("Actualizar ubicación", onBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.loaded) map() else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            Text(
                "Mueve el mapa para ajustar el punto",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                    .background(colors.card, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
            )
            // The pin tip sits on the map center.
            Icon(
                Icons.Rounded.Place,
                contentDescription = null,
                tint = colors.hero,
                modifier = Modifier.align(Alignment.Center).size(44.dp).offset(y = (-22).dp),
            )
            SmallFloatingActionButton(
                onClick = onMyLocation,
                containerColor = colors.card,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) { Icon(Icons.Rounded.MyLocation, contentDescription = "Usar mi ubicación") }
        }
        ActionFooter(
            label = "Guardar ubicación",
            enabled = state.loaded,
            busy = state.saving,
            onClick = { onIntent(UpdateLocationIntent.Submit) },
            tag = UpdateLocationTestTags.SUBMIT,
            error = state.error,
            summary = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("Coordenadas", fontSize = 12.sp, color = muted)
                            Text(state.coordinates, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        state.accuracy?.let { Text(it, fontSize = 12.sp, color = muted) }
                    }
                    state.placeCheck?.let { check ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (check.inside) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = if (check.inside) colors.success else colors.warning,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(check.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    state.napNote?.let { Text(it, fontSize = 13.sp, color = muted) }
                    state.reference?.let {
                        Column {
                            Text("Referencia", fontSize = 12.sp, color = muted)
                            Text(it, fontSize = 14.sp)
                        }
                    }
                }
            },
        )
    }
}
