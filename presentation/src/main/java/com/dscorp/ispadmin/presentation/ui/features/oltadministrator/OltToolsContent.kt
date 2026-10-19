package com.dscorp.ispadmin.presentation.ui.features.oltadministrator

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.components.ConsumeOnce
import org.koin.androidx.compose.koinViewModel

private val CARD = RoundedCornerShape(16.dp)

/** a-mas-08 «Herramientas OLT». */
@Composable
fun OltToolsContent(state: OltToolsUiState, onIntent: (OltToolsIntent) -> Unit) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var confirming by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().background(colors.warningContainer, CARD).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = colors.warning, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Solo administradores. Los cambios afectan el servicio del abonado.", fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onIntent(OltToolsIntent.QueryChanged(it)) },
                label = { Text("Serial de la ONU") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onIntent(OltToolsIntent.Search) }),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { onIntent(OltToolsIntent.Search) },
                enabled = state.query.isNotBlank() && !state.searching,
                modifier = Modifier.heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.hero, contentColor = colors.onHero),
            ) {
                if (state.searching) CircularProgressIndicator(Modifier.size(18.dp), color = colors.onHero, strokeWidth = 2.dp)
                else Icon(Icons.Rounded.Search, contentDescription = "Buscar")
            }
        }
        state.error?.let { Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.error) }
        state.card?.let { card ->
            Column(Modifier.fillMaxWidth().background(colors.card, CARD).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Router, contentDescription = null, tint = muted)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(card.sn, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        card.model?.let { Text(it, fontSize = 13.sp, color = muted) }
                    }
                }
                card.rows.forEach { (label, value) ->
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = colors.track)
                    Row {
                        Text(label, fontSize = 14.sp, color = muted, modifier = Modifier.weight(1f))
                        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            OutlinedButton(
                onClick = { confirming = true },
                enabled = !state.deleting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (state.deleting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Eliminar de la OLT", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
            if (confirming) {
                AlertDialog(
                    onDismissRequest = { confirming = false },
                    title = { Text("¿Eliminar la ONU ${card.sn}?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            card.linkedWarning?.let { Text(it, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) }
                            Text(card.deleteWarning)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { confirming = false; onIntent(OltToolsIntent.Delete) }) {
                            Text("Eliminar ONU", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = { TextButton(onClick = { confirming = false }) { Text("Volver") } },
                )
            }
        }
    }
}

@Composable
fun OltToolsRoute() {
    val viewModel: OltToolsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    ConsumeOnce(state.message, onConsumed = { viewModel.onIntent(OltToolsIntent.MessageShown) }) { snackbar.showSnackbar(it) }
    Box(Modifier.fillMaxSize()) {
        OltToolsContent(state, viewModel::onIntent)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
