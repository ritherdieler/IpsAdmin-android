package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState

private data class ServiceTypeOption(
    val type: InstallationType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tag: String,
)

private val SERVICE_TYPE_OPTIONS = listOf(
    ServiceTypeOption(
        InstallationType.FIBER, "Fibra con internet", "Empiezas autorizando una ONU",
        Icons.Outlined.Router, RegisterSubscriptionTestTags.SERVICE_TYPE_FIBER,
    ),
    ServiceTypeOption(
        InstallationType.ONLY_TV_FIBER, "Solo TV con receptor CATV", "Splitter, sin ONU ni internet",
        Icons.Outlined.Tv, RegisterSubscriptionTestTags.SERVICE_TYPE_TV,
    ),
    ServiceTypeOption(
        InstallationType.WIRELESS, "Inalámbrico", "Internet por antena, sin ONU",
        Icons.Outlined.Wifi, RegisterSubscriptionTestTags.SERVICE_TYPE_WIRELESS,
    ),
)

/** Name of the service type as the first step shows it. */
internal fun InstallationType.serviceTypeTitle(): String =
    SERVICE_TYPE_OPTIONS.first { it.type == this }.title

/** a-reg-00-tipo (step 0): the service type decides whether the wizard authorizes an ONU first. */
@Composable
internal fun ServiceTypeStepContent(
    formState: RegisterSubscriptionState,
    onIntent: (RegisterSubscriptionIntent) -> Unit,
) {
    val colors = IspTheme.colors
    val enabled = !formState.isLoading
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("¿Qué servicio vas a registrar?", fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Según el tipo, el asistente sigue pasos distintos.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SERVICE_TYPE_OPTIONS.forEach { option ->
                ServiceTypeTile(
                    option = option,
                    selected = formState.serviceTypeChosen && formState.registerSubscriptionForm.installationType == option.type,
                    enabled = enabled,
                    onClick = { onIntent(RegisterSubscriptionIntent.ServiceTypeSelected(option.type)) },
                )
            }
        }
        formState.serviceTypeError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, lineHeight = 16.sp)
        }
        RegisterNote(colors.infoContainer, colors.info, Icons.Outlined.Info) {
            Text(
                "Sin ONU no se autoriza ningún equipo en la OLT. Empiezas por los datos del cliente y son 3 pasos en vez de 4.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun ServiceTypeTile(option: ServiceTypeOption, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = IspTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.hero else MaterialTheme.colorScheme.outline, shape)
            .background(colors.card, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .testTag(option.tag)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = colors.hero))
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(40.dp).background(if (selected) colors.selectedContainer else colors.track, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                option.icon,
                contentDescription = null,
                tint = if (selected) colors.onSelectedContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(option.title, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
            Text(option.subtitle, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
