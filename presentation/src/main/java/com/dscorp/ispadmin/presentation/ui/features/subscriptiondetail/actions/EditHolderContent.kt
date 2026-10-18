package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterFieldLabel
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterNote
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterTextField

object EditHolderTestTags {
    const val SUBMIT = "edit_holder_submit"
    fun field(field: HolderField) = "edit_holder_${field.name.lowercase()}"
}

/** a-sus-14 «Editar titular». */
@Composable
fun EditHolderContent(state: EditHolderUiState, onIntent: (EditHolderIntent) -> Unit, onBack: () -> Unit, onRetry: () -> Unit = {}) {
    val colors = IspTheme.colors
    val form = state.form
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ActionHeader("Editar titular", onBack) {
            if (form != null) {
                TextButton(onClick = { onIntent(EditHolderIntent.Submit) }, enabled = state.canSave) { Text("Guardar", fontWeight = FontWeight.SemiBold) }
            }
        }
        Box(Modifier.weight(1f)) {
            when {
                form == null && state.error != null -> ActionLoadError(state.error, onRetry)
                form == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.sharedNote?.let { note ->
                        RegisterNote(colors.infoContainer, colors.info, Icons.Rounded.Group) { Text(note, fontSize = 14.sp) }
                    }
                    Field(state, HolderField.NAME, onIntent)
                    Field(state, HolderField.LAST_NAME, onIntent)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Field(state, HolderField.DNI, onIntent, Modifier.weight(1f), helper = "8 dígitos", digits = true)
                        Field(state, HolderField.PHONE, onIntent, Modifier.weight(1f), helper = "9 dígitos", digits = true)
                    }
                    Field(state, HolderField.EMAIL, onIntent, helper = if (form.email.isEmpty()) "Sin email registrado" else null, email = true)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Field(state, HolderField.ADDRESS, onIntent, helper = "Calle, número y referencia", singleLine = false)
                        Text("${form.address.length}/${HolderForm.MAX_ADDRESS}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End))
                    }
                    PlaceField(state, onIntent)
                }
            }
        }
        if (form != null) {
            ActionFooter(
                label = "Guardar cambios",
                enabled = state.canSave,
                busy = state.saving,
                onClick = { onIntent(EditHolderIntent.Submit) },
                tag = EditHolderTestTags.SUBMIT,
                note = if (!state.changed) "Sin cambios" else null,
                error = state.error,
            )
        }
    }
}

@Composable
private fun Field(
    state: EditHolderUiState,
    field: HolderField,
    onIntent: (EditHolderIntent) -> Unit,
    modifier: Modifier = Modifier,
    helper: String? = null,
    digits: Boolean = false,
    email: Boolean = false,
    singleLine: Boolean = true,
) {
    RegisterTextField(
        label = field.label,
        value = state.form?.value(field).orEmpty(),
        onValueChange = { onIntent(EditHolderIntent.Changed(field, it)) },
        modifier = modifier.fillMaxWidth(),
        fieldModifier = Modifier.testTag(EditHolderTestTags.field(field)),
        error = state.errors[field],
        helper = helper,
        singleLine = singleLine,
        minHeight = if (singleLine) 52.dp else 88.dp,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                digits -> KeyboardType.Number
                email -> KeyboardType.Email
                else -> KeyboardType.Text
            },
        ),
    )
}

@Composable
private fun PlaceField(state: EditHolderUiState, onIntent: (EditHolderIntent) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val error = state.errors[HolderField.PLACE]
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RegisterFieldLabel(HolderField.PLACE.label)
        Box {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .border(1.dp, if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable(role = Role.DropdownList) { open = true }
                    .padding(horizontal = 16.dp)
                    .testTag(EditHolderTestTags.field(HolderField.PLACE)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.form?.placeName.orEmpty().ifBlank { "Elige el lugar" }, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ExpandMore, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                state.places.forEach { (id, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { open = false; onIntent(EditHolderIntent.PlaceSelected(id)) })
                }
            }
        }
        error?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
    }
}
