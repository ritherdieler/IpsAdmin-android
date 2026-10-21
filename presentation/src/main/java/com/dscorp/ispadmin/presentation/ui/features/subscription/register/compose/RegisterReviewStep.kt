package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.TvCpeKind
import java.util.Locale

/** "987 111 222" for 9-digit mobiles; anything else as typed. */
internal fun phoneGroups(phone: String): String =
    if (phone.length == 9 && phone.all { it.isDigit() }) phone.chunked(3).joinToString(" ") else phone

private data class ReviewRow(val label: String, val value: String, val detail: String? = null, val mono: Boolean = false)

/** a-reg-06 (step 4): one card per step, in the app order; the authorized ONU has no «Editar». */
@Composable
internal fun ReviewStepContent(formState: RegisterSubscriptionState, onIntent: (RegisterSubscriptionIntent) -> Unit) {
    val form = formState.registerSubscriptionForm
    val onu = form.selectedOnu
    val plan = form.selectedPlan
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Revisa antes de registrar", fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
        if (onu != null || formState.preauthorizationOperation != null) {
            ReviewCard("Equipo", trailing = "Autorizada en la OLT".takeIf { formState.usesPreauthorization }, rows = listOfNotNull(
                ReviewRow("ONU", onu?.sn ?: formState.preauthorizationOperation?.serial.orEmpty(), mono = true),
                onu?.let { ReviewRow("Modelo · puerto", "${it.onu_type_name} · OLT ${it.olt_id} · ${it.portLabel()}") },
                ReviewRow("VLAN", form.vlan).takeIf { !formState.usesPreauthorization && form.requiresOnu() },
            ))
        }
        ReviewCard("Cliente", editLabel = "Editar cliente", onEdit = { onIntent(RegisterSubscriptionIntent.EditStepClicked(RegisterSubscriptionWizardStep.CLIENT_LOCATION)) },
            rows = listOf(
                ReviewRow("Titular", "${form.firstName} ${form.lastName}".toTitleCase()),
                ReviewRow("DNI · teléfono", listOf(form.dni, phoneGroups(form.phone)).filter { it.isNotBlank() }.joinToString(" · ")),
                ReviewRow("Dirección", form.address, detail = listOfNotNull(
                    form.selectedPlace?.name?.toTitleCase(),
                    form.location?.let { String.format(Locale.US, "%.5f, %.5f", it.latitude, it.longitude) },
                ).joinToString(" · ")),
            ))
        val typeLabel = when (form.installationType) {
            InstallationType.FIBER -> "Fibra"
            InstallationType.WIRELESS -> "Inalámbrico"
            InstallationType.ONLY_TV_FIBER -> "Solo TV"
        }
        val planDetail = listOfNotNull(
            plan?.price?.let { String.format(Locale.US, "S/ %,.2f al mes", it) },
            plan?.takeIf { !it.downloadSpeed.isNullOrBlank() && !it.uploadSpeed.isNullOrBlank() }?.let { "${it.downloadSpeed}/${it.uploadSpeed} Mbps" },
        ).joinToString(" · ")
        ReviewCard("Servicio", editLabel = "Editar servicio", onEdit = { onIntent(RegisterSubscriptionIntent.EditStepClicked(RegisterSubscriptionWizardStep.INSTALLATION)) },
            rows = listOfNotNull(
                ReviewRow("Plan", "$typeLabel · ${plan?.name.orEmpty()}", detail = planDetail.ifBlank { null }),
                ReviewRow("Acceso a internet", form.accessMode.registerLabel()).takeIf { form.shouldShowAccessModeSelector() },
                ReviewRow("Caja NAP", form.selectedNapBox?.code.orEmpty(), mono = true).takeIf { form.requiresNapBox() },
                ReviewRow("Conexión", "Receptor CATV · sin ONU").takeIf {
                    form.installationType == InstallationType.ONLY_TV_FIBER && form.tvCpeKind == TvCpeKind.OPTICAL_RECEIVER
                },
                ReviewRow("Equipo", if (form.equipmentCondition == EquipmentCondition.LOAN) "En préstamo" else "Vendido"),
                ReviewRow("Observaciones", form.note).takeIf { form.note.isNotBlank() },
            ))
        val wifiRows = if (form.requiresWifiConfig()) listOf(
            ReviewRow("Red", form.wifiSsid24),
            ReviewRow("5 GHz", form.resolvedWifiSsid5()),
            ReviewRow("Clave", "•".repeat(form.wifiPassword24.length.coerceIn(8, 12))),
        ) else emptyList()
        val photoReady = !form.facadePhotoUrl.isNullOrBlank() || form.facadePhotoUri != null
        ReviewCard(if (wifiRows.isEmpty()) "Foto" else "Wi-Fi y foto",
            editLabel = if (wifiRows.isEmpty()) "Editar foto" else "Editar Wi-Fi y foto",
            onEdit = { onIntent(RegisterSubscriptionIntent.EditStepClicked(RegisterSubscriptionWizardStep.INSTALLATION)) },
            rows = wifiRows + ReviewRow("Foto de fachada", if (photoReady) "✓ Lista" else "Falta"))
        Text(
            if (form.installationType == InstallationType.ONLY_TV_FIBER && form.tvCpeKind == TvCpeKind.OPTICAL_RECEIVER) {
                "Al registrar se reserva el puerto en la caja NAP y se activa el servicio de TV. No se autoriza ninguna ONU."
            } else if (form.installationType == InstallationType.WIRELESS) {
                "Al registrar se configura el acceso a internet del cliente."
            } else {
                "Al registrar se configura el acceso a internet y el Wi-Fi. Tarda hasta 2 minutos y puedes salir de esta pantalla."
            },
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReviewCard(
    title: String,
    rows: List<ReviewRow>,
    trailing: String? = null,
    editLabel: String? = null,
    onEdit: (() -> Unit)? = null,
) {
    val colors = IspTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            trailing?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (onEdit != null) {
                TextButton(onClick = onEdit, modifier = Modifier.semantics { contentDescription = editLabel.orEmpty() }) {
                    Text("Editar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.accentInk)
                }
            }
        }
        val shown = rows.filter { it.value.isNotBlank() }
        shown.forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(row.label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(row.value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
                        fontFamily = if (row.mono) FontFamily.Monospace else null, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    row.detail?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                    }
                }
            }
            if (index < shown.lastIndex) HorizontalDivider(color = colors.track)
        }
    }
}
