package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.dscorp.ispadmin.domain.model.AccessMode
import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.subscription.RegisterSubscriptionFormConstraints
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.google.android.gms.maps.model.LatLng
import java.security.SecureRandom
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_M = 6_371_000.0
private const val WIFI_KEY_LENGTH = 10
// No 0/O, 1/l/I: the key is read aloud to the client.
private const val WIFI_KEY_ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private val random = SecureRandom()

/** "180 m" up to a kilometre (rounded to 10 m), then "1.2 km". */
internal fun distanceLabel(meters: Double): String =
    if (meters < 1_000) "${(meters / 10).roundToInt() * 10} m" else String.format(Locale.US, "%.1f km", meters / 1_000)

/** Straight-line distance from the installation point to a NAP box (haversine), null without coordinates. */
internal fun napDistanceMeters(from: LatLng?, nap: NapBoxResponse): Double? {
    val lat = nap.latitude ?: return null
    val lng = nap.longitude ?: return null
    from ?: return null
    val dLat = Math.toRadians(lat - from.latitude)
    val dLng = Math.toRadians(lng - from.longitude)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(lat)) * sin(dLng / 2) * sin(dLng / 2)
    return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
}

internal fun generateWifiPassword(): String =
    (1..WIFI_KEY_LENGTH).map { WIFI_KEY_ALPHABET[random.nextInt(WIFI_KEY_ALPHABET.length)] }.joinToString("")

private fun InstallationType.label() = when (this) {
    InstallationType.FIBER -> "Fibra"
    InstallationType.WIRELESS -> "Inalámbrico"
    InstallationType.ONLY_TV_FIBER -> "Solo TV"
}

private fun soles(amount: Double?) = amount?.let { String.format(Locale.US, "S/ %,.2f", it) }.orEmpty()

/** a-reg-03/05 (step 3): type, plan, access, NAP, equipment, Wi-Fi, facade photo and notes. */
@Composable
internal fun ServiceStepContent(
    formState: RegisterSubscriptionState,
    onIntent: (RegisterSubscriptionIntent) -> Unit,
    onOpenNapPicker: () -> Unit,
    onFacadePhotoClick: () -> Unit,
    hostDeviceField: @Composable () -> Unit,
    legacyFiberFields: @Composable () -> Unit,
) {
    val form = formState.registerSubscriptionForm
    val enabled = !formState.isLoading
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Servicio y plan", fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
        if (formState.preauthorizationEnabled) {
            InstallationTypeReadOnly(form.installationType)
        } else {
            InstallationTypeSelector(form.installationType, { onIntent(RegisterSubscriptionIntent.InstallationTypeSelected(it)) }, enabled)
        }
        PlanList(form, enabled, onIntent)
        if (form.shouldShowAccessModeSelector()) {
            WanAccessModeSelector(form.accessMode, { onIntent(RegisterSubscriptionIntent.AccessModeSelected(it)) }, enabled = enabled)
        }
        if (form.shouldShowHostDeviceSelector()) hostDeviceField()
        if (form.requiresNapBox()) NapCard(formState, onOpenNapPicker)
        legacyFiberFields()
        EquipmentConditionSelector(form.equipmentCondition, { onIntent(RegisterSubscriptionIntent.EquipmentConditionChanged(it)) }, enabled)
        if (form.installationType == InstallationType.FIBER) WifiSection(form, enabled, onIntent)
        FacadePhoto(formState, onFacadePhotoClick)
        RegisterTextField(
            "Observaciones", form.note, { onIntent(RegisterSubscriptionIntent.NoteChanged(it)) },
            fieldModifier = Modifier.testTag(RegisterSubscriptionTestTags.NOTE),
            error = form.noteError,
            helper = "${form.note.length}/${RegisterSubscriptionFormConstraints.MAX_NOTE_LENGTH}",
            enabled = enabled,
            singleLine = false,
            minHeight = 88.dp,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
    }
}

/** The type is chosen on the first step; here it is only shown. */
@Composable
private fun InstallationTypeReadOnly(type: InstallationType) {
    val colors = IspTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RegisterFieldLabel("Tipo de instalación")
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .background(colors.card, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .testTag(RegisterSubscriptionTestTags.INSTALLATION_TYPE)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (type == InstallationType.ONLY_TV_FIBER) "Solo TV · receptor CATV" else type.label(),
                Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text("Elegido al inicio", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Pill-shaped segmented choice with ✓ on the selected option, as in a-reg and a-tic-05. */
@Composable
internal fun <T> Segmented(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    containerTag: String? = null,
    itemTag: ((Int, T) -> String)? = null,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
) {
    val colors = IspTheme.colors
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier.fillMaxWidth().height(44.dp).clip(CircleShape).border(1.dp, outline, CircleShape).background(colors.card)
            .then(if (containerTag != null) Modifier.testTag(containerTag) else Modifier),
    ) {
        options.forEachIndexed { index, option ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(outline))
            val isSelected = option == selected
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (isSelected) colors.selectedContainer else Color.Transparent)
                    .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) { onSelect(option) }
                    .then(if (itemTag != null) Modifier.testTag(itemTag(index, option)) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isSelected) "✓ ${label(option)}" else label(option),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) colors.onSelectedContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PlanList(form: RegisterSubscriptionFormState, enabled: Boolean, onIntent: (RegisterSubscriptionIntent) -> Unit) {
    val colors = IspTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "Plan · ${form.installationType.label()} (${form.planList.size} activos)",
                Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text("Precio al mes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp)).testTag(RegisterSubscriptionTestTags.PLAN)) {
            if (form.planList.isEmpty()) {
                Text("No hay planes activos para este tipo de instalación.", Modifier.padding(16.dp), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            form.planList.forEachIndexed { index, plan ->
                val isSelected = plan.id == form.selectedPlan?.id
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp)
                        .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) {
                            onIntent(RegisterSubscriptionIntent.PlanSelected(plan))
                        }
                        .testTag(RegisterSubscriptionTestTags.planItem(index))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = colors.hero))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(plan.name.orEmpty(), fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
                        val speeds = listOfNotNull(plan.downloadSpeed, plan.uploadSpeed).filter { it.isNotBlank() }
                        if (speeds.size == 2) {
                            Text("${speeds[0]}/${speeds[1]} Mbps", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(soles(plan.price), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                if (index < form.planList.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.track)
            }
        }
        form.planError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}

@Composable
private fun NapCard(formState: RegisterSubscriptionState, onOpen: () -> Unit) {
    val colors = IspTheme.colors
    val form = formState.registerSubscriptionForm
    val nap = form.selectedNapBox
    val canPick = form.selectedPlace != null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            RegisterFieldLabel("Caja NAP *", Modifier.weight(1f))
            Text("La más cercana a la ubicación", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            Modifier.fillMaxWidth()
                .border(if (nap != null) 2.dp else 1.dp, if (nap != null) colors.hero else MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .background(colors.card, RoundedCornerShape(16.dp))
                .clickable(enabled = canPick && !formState.isLoading, role = Role.Button, onClick = onOpen)
                .testTag(RegisterSubscriptionTestTags.NAP_BOX)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when {
                    formState.isLoadingNearbyNapBoxes -> Row(
                        Modifier.testTag(RegisterSubscriptionTestTags.NEARBY_NAP_LOADING),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Buscando cajas NAP cercanas…", fontSize = 14.sp)
                    }
                    nap != null -> {
                        Text(nap.code, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        if (nap.address.isNotBlank()) Text(nap.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        napDistanceMeters(form.location, nap)?.let {
                            Text("${distanceLabel(it)} de la instalación", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    !canPick -> Text("Elige el lugar en Cliente para ver las cajas cercanas.", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Text("Elige la caja NAP", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (canPick) Text(if (nap != null) "Cambiar" else "Elegir", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.accentInk)
        }
        form.napBoxError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}

/** a-reg-04: in-screen sheet with the place's NAP boxes, nearest first. */
@Composable
internal fun NapPickerSheet(form: RegisterSubscriptionFormState, onSelect: (NapBoxResponse) -> Unit, onDismiss: () -> Unit) {
    val colors = IspTheme.colors
    BackHandler(onBack = onDismiss)
    val boxes = form.napBoxList.sortedBy { napDistanceMeters(form.location, it) ?: Double.MAX_VALUE }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color(0x8510142B))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        Column(
            Modifier.fillMaxSize().padding(top = 64.dp)
                .background(colors.card, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .testTag(RegisterSubscriptionTestTags.NAP_SHEET)
                .padding(horizontal = 16.dp),
        ) {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 32.dp, height = 4.dp).background(MaterialTheme.colorScheme.outline, CircleShape))
            }
            Text("Elegir caja NAP", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(form.address.takeIf { it.isNotBlank() }?.let { "Cercanas a $it" }, form.selectedPlace?.name).joinToString(" · "),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
                if (boxes.isEmpty()) {
                    Text("No hay cajas NAP en este lugar.", Modifier.padding(vertical = 16.dp), fontSize = 14.sp)
                }
                boxes.forEachIndexed { index, nap ->
                    val isSelected = nap.code == form.selectedNapBox?.code
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp)
                            .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(nap) }
                            .testTag(RegisterSubscriptionTestTags.napItem(index))
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = colors.hero))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(nap.code, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            if (nap.address.isNotBlank()) Text(nap.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        napDistanceMeters(form.location, nap)?.let {
                            Text(distanceLabel(it), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (index < boxes.lastIndex) HorizontalDivider(color = colors.track)
                }
            }
        }
    }
}

@Composable
private fun WifiSection(form: RegisterSubscriptionFormState, enabled: Boolean, onIntent: (RegisterSubscriptionIntent) -> Unit) {
    val colors = IspTheme.colors
    var visible by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Wi-Fi", fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
        RegisterTextField(
            if (form.useDifferentWifiNames) "Nombre de red 2.4 GHz *" else "Nombre de red *", form.wifiSsid24,
            { onIntent(RegisterSubscriptionIntent.WifiSsid24Changed(it)) },
            fieldModifier = Modifier.testTag(RegisterSubscriptionTestTags.WIFI_SSID_24),
            error = form.wifiSsid24Error, enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        )
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Nombre distinto para 5 GHz", fontSize = 15.sp)
                if (!form.useDifferentWifiNames && form.wifiSsid24.isNotBlank()) {
                    val derived = form.resolvedWifiSsid5()
                    Text("5 GHz usará $derived", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { contentDescription = "SSID 5 GHz derivado $derived" })
                }
            }
            Switch(
                checked = form.useDifferentWifiNames,
                onCheckedChange = { onIntent(RegisterSubscriptionIntent.UseDifferentWifiNamesChanged(it)) },
                enabled = enabled,
                modifier = Modifier.testTag(RegisterSubscriptionTestTags.WIFI_DIFFERENT_NAMES),
                colors = SwitchDefaults.colors(checkedTrackColor = colors.hero),
            )
        }
        if (form.useDifferentWifiNames) {
            RegisterTextField(
                "Nombre de red 5 GHz *", form.wifiSsid5, { onIntent(RegisterSubscriptionIntent.WifiSsid5Changed(it)) },
                fieldModifier = Modifier.testTag(RegisterSubscriptionTestTags.WIFI_SSID_5),
                error = form.wifiSsid5Error, enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RegisterFieldLabel("Clave *")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = form.wifiPassword24,
                    onValueChange = { onIntent(RegisterSubscriptionIntent.WifiPassword24Changed(it)) },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag(RegisterSubscriptionTestTags.WIFI_PASSWORD_24),
                    enabled = enabled,
                    singleLine = true,
                    isError = form.wifiPassword24Error != null,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = colors.card, unfocusedContainerColor = colors.card),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }, modifier = Modifier.testTag(RegisterSubscriptionTestTags.WIFI_PASSWORD_24_TOGGLE)) {
                            Icon(
                                if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (visible) "Ocultar clave WiFi" else "Mostrar clave WiFi",
                            )
                        }
                    },
                )
                TextButton(onClick = { onIntent(RegisterSubscriptionIntent.WifiPassword24Changed(generateWifiPassword())); visible = true }, enabled = enabled) {
                    Text("Generar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.hero)
                }
            }
            Text(form.wifiPassword24Error ?: "Mínimo 8 caracteres", fontSize = 12.sp,
                color = if (form.wifiPassword24Error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FacadePhoto(formState: RegisterSubscriptionState, onFacadePhotoClick: () -> Unit) {
    val form = formState.registerSubscriptionForm
    val source = form.facadePhotoUrl ?: form.facadePhotoUri?.toString()
    val hasPhoto = source != null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Foto de fachada *", Modifier.weight(1f), fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = onFacadePhotoClick, enabled = !formState.isLoading, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (hasPhoto) "Repetir foto" else "Tomar foto", fontWeight = FontWeight.SemiBold, color = IspTheme.colors.hero)
            }
        }
        Box(
            Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
                .background(IspTheme.colors.track)
                .testTag(RegisterSubscriptionTestTags.FACADE_PHOTO)
                .clickable(enabled = !formState.isLoading, role = Role.Button, onClick = onFacadePhotoClick)
                .semantics { contentDescription = if (hasPhoto) "Cambiar foto de fachada" else "Añadir foto de fachada" },
            contentAlignment = Alignment.Center,
        ) {
            if (hasPhoto) {
                Image(rememberAsyncImagePainter(source), contentDescription = "Foto de fachada", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                    Text("Toma la foto de la fachada", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        form.facadePhotoError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}
