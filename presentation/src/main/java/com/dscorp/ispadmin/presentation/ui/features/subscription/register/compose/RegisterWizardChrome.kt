package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep

internal data class RegistrationProgressPhase(
    val label: String,
    val steps: Set<RegisterSubscriptionWizardStep>,
)

/** Visible steps, in the app order: the ONU is authorized first so ACS connects while the client is filled in. */
internal fun registrationProgressPhases(usesPreauthorization: Boolean): List<RegistrationProgressPhase> {
    val formPhases = listOf(
        RegistrationProgressPhase("Cliente", setOf(RegisterSubscriptionWizardStep.CLIENT_LOCATION)),
        RegistrationProgressPhase("Servicio", setOf(RegisterSubscriptionWizardStep.INSTALLATION)),
        RegistrationProgressPhase("Revisión", setOf(RegisterSubscriptionWizardStep.CONFIRMATION)),
    )
    if (!usesPreauthorization) return formPhases
    return listOf(
        RegistrationProgressPhase(
            "Equipo",
            setOf(
                RegisterSubscriptionWizardStep.ONU_SELECTION,
                RegisterSubscriptionWizardStep.ONU_CONFIRMATION,
                RegisterSubscriptionWizardStep.WAITING_FOR_ACS,
            ),
        ),
    ) + formPhases
}

/** "1/5" as the OLT names board and port. */
internal fun Onu.portLabel(): String = "$board/$port"

internal fun Onu.chosenLabel(): String = "ONU elegida: $sn · OLT $olt_id · puerto ${portLabel()}"

/** a-reg header: close, title, "N de M", progress bars and step names; the type step shows "Antes de empezar" only. */
@Composable
internal fun RegisterWizardHeader(
    currentStep: RegisterSubscriptionWizardStep,
    usesPreauthorization: Boolean,
    onClose: () -> Unit,
) {
    val colors = IspTheme.colors
    val phases = registrationProgressPhases(usesPreauthorization)
    val choosingType = currentStep == RegisterSubscriptionWizardStep.SERVICE_TYPE
    val current = phases.indexOfFirst { currentStep in it.steps }.coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onClose,
                // Nothing is authorized on the type step, so its close is a plain leave button.
                modifier = if (usesPreauthorization && !choosingType) Modifier.testTag(RegisterSubscriptionTestTags.ONU_REGISTRATION_CANCEL) else Modifier,
            ) { Icon(Icons.Rounded.Close, contentDescription = "Cerrar registro") }
            Text("Nueva suscripción", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                if (choosingType) "Antes de empezar" else "${current + 1} de ${phases.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.hero,
            )
        }
        if (choosingType) return@Column
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("wizard_stepper")
                .semantics { contentDescription = "Paso ${current + 1} de ${phases.size}: ${phases[current].label}" },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                phases.forEachIndexed { index, _ ->
                    Box(
                        Modifier.weight(1f).height(4.dp)
                            .background(if (index <= current) colors.hero else colors.track, CircleShape)
                            .testTag("wizard_step_${index + 1}"),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                phases.forEachIndexed { index, phase ->
                    Text(
                        phase.label,
                        Modifier.weight(1f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = if (index == current) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (index == current) colors.hero else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** a-reg footer: one status line, then a text action on the left and the main pill on the right. */
@Composable
internal fun RegisterWizardFooterLine(text: String, ok: Boolean = false, error: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text = if (ok) "✓ $text" else text,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = when {
            error -> MaterialTheme.colorScheme.error
            ok -> IspTheme.colors.success
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
internal fun RegisterPrimaryButton(text: String, enabled: Boolean, loading: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = IspTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = colors.onHero, strokeWidth = 2.dp)
        } else {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
internal fun RegisterTextAction(text: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = IspTheme.colors.hero)
    }
}

/** a-reg Equipo: unauthorized ONUs as a list with a serial search; the selector keeps the e2e test tags. */
@Composable
internal fun OnuListBlock(
    onus: List<Onu>,
    selected: Onu?,
    error: String?,
    loading: Boolean,
    refreshIcon: @Composable () -> Unit,
    onSelect: (Onu) -> Unit,
) {
    val colors = IspTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val shown = onus.filter { query.isBlank() || it.sn.contains(query.trim(), ignoreCase = true) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("ONU sin autorizar", fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    when (onus.size) {
                        0 -> "Sin equipos por autorizar"
                        1 -> "1 equipo esperando en la OLT"
                        else -> "${onus.size} equipos esperando en la OLT"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            refreshIcon()
        }
        Row(
            Modifier.fillMaxWidth().height(56.dp).background(colors.track, RoundedCornerShape(28.dp)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Buscar por serie", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(colors.hero),
                    modifier = Modifier.fillMaxWidth().testTag(RegisterSubscriptionTestTags.ONU),
                )
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        when {
            shown.isNotEmpty() -> Column(Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp))) {
                shown.forEachIndexed { index, onu ->
                    val isSelected = onu.sn == selected?.sn
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 72.dp)
                            .background(if (isSelected) colors.selectedContainer.copy(alpha = 0.45f) else colors.card,
                                if (index == 0 && shown.size == 1) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp))
                            .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(onu) }
                            .testTag(RegisterSubscriptionTestTags.onuItem(index))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = colors.hero))
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(onu.sn, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${onu.onu_type_name} · puerto ${onu.portLabel()}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (index < shown.lastIndex) HorizontalDivider(color = colors.track)
                }
            }
            onus.isNotEmpty() -> Text("Ninguna ONU coincide con «$query».", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            error == null && !loading -> Text(
                "No hay equipos disponibles. Actualiza la lista o comprueba la conexión.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            Modifier.fillMaxWidth().background(colors.infoContainer, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(Modifier.size(36.dp).background(colors.card, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.info, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Al continuar, la ONU se autoriza en la OLT y empieza a conectarse con la gestión remota. Mientras tanto llenas los datos del cliente.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
internal fun RegisterFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** a-reg field: label above, 12 dp outlined box, error or helper below. The test tag goes on the editable field. */
@Composable
internal fun RegisterTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 52.dp,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RegisterFieldLabel(label)
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier.fillMaxWidth().heightIn(min = minHeight),
            enabled = enabled,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 4,
            isError = error != null,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = keyboardOptions,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = IspTheme.colors.card,
                unfocusedContainerColor = IspTheme.colors.card,
                disabledContainerColor = IspTheme.colors.card,
            ),
        )
        when {
            error != null -> Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            helper != null -> Text(helper, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Tinted note with an icon tile: ACS state, ticket origin, existing customer. */
@Composable
internal fun RegisterNote(
    container: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth().background(container, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(36.dp).background(IspTheme.colors.card, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}
