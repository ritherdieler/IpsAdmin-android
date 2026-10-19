package com.dscorp.ispadmin.presentation.ui.features.profile

import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import com.dscorp.ispadmin.presentation.theme.ThemeMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.more.MoreMenu
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge

private val CARD = RoundedCornerShape(16.dp)

/** a-mas-02 «Perfil»: who is signed in, their data, the app version and logout. */
@Composable
fun ProfileContent(
    uiState: MyProfileUiState,
    onLogout: () -> Unit,
    onClearError: () -> Unit,
    versionLabel: String = "",
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeSelected: (ThemeMode) -> Unit = {},
) {
    var choosingTheme by remember { mutableStateOf(false) }
    if (choosingTheme) {
        ThemeDialog(themeMode, onSelect = { choosingTheme = false; onThemeSelected(it) }, onDismiss = { choosingTheme = false })
    }
    val colors = IspTheme.colors
    var confirmLogout by remember { mutableStateOf(false) }
    if (confirmLogout) {
        LogoutDialog(onConfirm = { confirmLogout = false; onLogout() }, onDismiss = { confirmLogout = false })
    }
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = onClearError,
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = onClearError) { Text("Aceptar") } },
        )
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.user?.let { user ->
                val header = MoreMenu.from(user, emptyList(), pending = null, activePlans = null).header
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Box(Modifier.size(64.dp).background(colors.selectedContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Text(header.initials, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.onSelectedContainer)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(header.name, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        if (header.role.isNotBlank()) HubBadge(StatusBadge(header.role, BadgeTone.INFO))
                    }
                }
                Section("Datos")
                InfoCard(profileRows(user))
            }
            Section("App")
            Column(Modifier.fillMaxWidth().background(colors.card, CARD).padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Versión", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(versionLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = colors.track)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { choosingTheme = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tema", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(themeMode.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 52.dp)
                    .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(26.dp))
                    .clickable { confirmLogout = true },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            }
        }
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

/** «Usuario · DNI · Email · Teléfono», with a readable phone and a note when there is no email. */
internal fun profileRows(user: User): List<Pair<String, String>> = listOf(
    "Usuario" to user.username,
    "DNI" to user.dni,
    "Email" to user.email.ifBlank { "Sin email" },
    "Teléfono" to user.phone.let { phone -> if (phone.length == 9 && phone.all(Char::isDigit)) phone.chunked(3).joinToString(" ") else phone },
).filter { it.second.isNotBlank() }

@Composable
private fun Section(title: String) {
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp))
}

@Composable
private fun InfoCard(rows: List<Pair<String, String>>) {
    val colors = IspTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.card, CARD).padding(horizontal = 16.dp)) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) HorizontalDivider(color = colors.track)
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ThemeDialog(current: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = mode == current, role = Role.RadioButton) { onSelect(mode) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(mode.label, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
