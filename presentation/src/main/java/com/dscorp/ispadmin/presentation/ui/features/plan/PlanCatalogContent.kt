package com.dscorp.ispadmin.presentation.ui.features.plan

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterFieldLabel
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterTextField

object PlanCatalogTestTags {
    const val SAVE = "plan_editor_save"
    const val NEW = "plan_catalog_new"
}

private val CARD = RoundedCornerShape(16.dp)

/** a-mas-03 «Planes». */
@Composable
fun PlanCatalogContent(state: PlanCatalogUiState, onIntent: (PlanCatalogIntent) -> Unit) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val catalog = state.catalog
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f)) {
            when {
                catalog == null && state.error != null -> Column(Modifier.padding(16.dp)) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    TextButton(onClick = { onIntent(PlanCatalogIntent.Load) }) { Text("Reintentar") }
                }
                catalog == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(Modifier.fillMaxWidth().background(colors.track, RoundedCornerShape(12.dp)).padding(4.dp)) {
                        listOf(false to "Activos · ${catalog.activeCount}", true to "Retirados · ${catalog.retiredCount}").forEach { (retired, label) ->
                            val selected = state.retiredTab == retired
                            Box(
                                Modifier.weight(1f).heightIn(min = 40.dp)
                                    .background(if (selected) colors.card else Color.Transparent, RoundedCornerShape(10.dp))
                                    .selectable(selected, role = Role.Tab) { onIntent(PlanCatalogIntent.TabSelected(retired)) },
                                contentAlignment = Alignment.Center,
                            ) { Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) }
                        }
                    }
                    catalog.groups(state.retiredTab).forEach { group ->
                        Text(group.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = muted, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                        Column(Modifier.fillMaxWidth().background(colors.card, CARD)) {
                            group.rows.forEachIndexed { index, row ->
                                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.track)
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onIntent(PlanCatalogIntent.Edit(row.id)) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(row.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text(row.detail, fontSize = 13.sp, color = muted)
                                    }
                                    Text(row.price, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = muted)
                                }
                            }
                        }
                    }
                    if (catalog.groups(state.retiredTab).isEmpty()) {
                        Text(if (state.retiredTab) "No hay planes retirados." else "No hay planes activos.", fontSize = 14.sp, color = muted)
                    }
                }
            }
        }
        if (catalog != null) {
            Column(Modifier.fillMaxWidth().background(colors.card).padding(16.dp)) {
                Button(
                    onClick = { onIntent(PlanCatalogIntent.New) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(PlanCatalogTestTags.NEW),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nuevo plan", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** a-mas-04: body of the plan editor sheet. */
@Composable
fun PlanEditorContent(
    editor: PlanEditor,
    saving: Boolean,
    error: String?,
    onChange: ((PlanEditor) -> PlanEditor) -> Unit,
    onSave: () -> Unit,
    onRetire: () -> Unit,
) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (editor.isNew) "Nuevo plan" else "Editar plan", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        RegisterTextField("Nombre *", editor.name, { value -> onChange { it.copy(name = value) } }, helper = editor.preview)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RegisterFieldLabel("Tipo")
            Row(Modifier.fillMaxWidth().background(colors.track, RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf(InstallationType.FIBER, InstallationType.WIRELESS, InstallationType.ONLY_TV_FIBER).forEach { type ->
                    val selected = editor.type == type
                    Box(
                        Modifier.weight(1f).heightIn(min = 40.dp)
                            .background(if (selected) colors.card else Color.Transparent, RoundedCornerShape(10.dp))
                            .selectable(selected, enabled = !editor.typeLocked, role = Role.RadioButton) { onChange { it.copy(type = type) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(type.planTypeLabel(), fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (editor.typeLocked && !selected) muted else Color.Unspecified)
                    }
                }
            }
            editor.typeNote?.let { Text(it, fontSize = 12.sp, color = muted) }
        }
        RegisterTextField("Precio mensual * (S/)", editor.price, { value -> onChange { it.copy(price = value) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        if (editor.type != InstallationType.ONLY_TV_FIBER) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RegisterTextField("Bajada (Mbps)", editor.download, { value -> onChange { it.copy(download = value.filter(Char::isDigit)) } },
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                RegisterTextField("Subida (Mbps)", editor.upload, { value -> onChange { it.copy(upload = value.filter(Char::isDigit)) } },
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        if (!editor.isNew) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Disponible para nuevas suscripciones", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Aparece al elegir plan en el registro", fontSize = 12.sp, color = muted)
                }
                Switch(checked = editor.available, onCheckedChange = { checked -> onChange { it.copy(available = checked) } })
            }
        }
        editor.warning?.let {
            Text(it, fontSize = 13.sp, color = colors.warning, modifier = Modifier.fillMaxWidth()
                .background(colors.warningContainer, RoundedCornerShape(10.dp)).padding(10.dp))
        }
        (error ?: editor.blockingReason)?.let { Text(it, fontSize = 13.sp, color = if (error != null) MaterialTheme.colorScheme.error else muted) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!editor.isNew && editor.available) {
                OutlinedButton(onClick = onRetire, enabled = !saving, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Text("Retirar plan", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = onSave,
                enabled = !saving && editor.changed && editor.blockingReason == null,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag(PlanCatalogTestTags.SAVE),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            ) {
                if (saving) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.padding(2.dp))
                else Text(if (editor.isNew) "Crear" else "Guardar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
