package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.components.components.formfields.MyOutlinedTextField
import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.subscription.RegisterSubscriptionFormConstraints
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.MyAutoCompleteTextViewCompose
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.ispadmin.presentation.ui.components.MyIconButton
import com.dscorp.ispadmin.presentation.ui.components.MyOutLinedDropDown
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter

@Composable
fun RegisterSubscriptionForm(
    modifier: Modifier = Modifier,
    formState: RegisterSubscriptionState,
    onIntent: (RegisterSubscriptionIntent) -> Unit = {},
    onFacadePhotoClick: () -> Unit = {},
) {
    val form = formState.registerSubscriptionForm
    val isFormValid by remember {
        derivedStateOf { formState.registerSubscriptionForm.isValid() }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(scrollState)
        ) {
            ClientDataFields(
                form = form,
                isLoading = formState.isLoading,
                onFirstNameChanged = { onIntent(RegisterSubscriptionIntent.FirstNameChanged(it)) },
                onLastNameChanged = { onIntent(RegisterSubscriptionIntent.LastNameChanged(it)) },
                onDniChanged = { onIntent(RegisterSubscriptionIntent.DniChanged(it)) },
                onPhoneChanged = { onIntent(RegisterSubscriptionIntent.PhoneChanged(it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            AddressFields(
                formState = formState,
                form = form,
                onPlaceSelected = { onIntent(RegisterSubscriptionIntent.PlaceSelected(it)) },
                onPlaceSelectionCleared = { onIntent(RegisterSubscriptionIntent.PlaceSelectionCleared) },
                onAddressChanged = { onIntent(RegisterSubscriptionIntent.AddressChanged(it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            InstallationBlock(
                formState = formState,
                form = form,
                onIntent = onIntent
            )

            FacadePhotoSection(
                formState = formState,
                onFacadePhotoClick = onFacadePhotoClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ObservationsField(
                form = form,
                isLoading = formState.isLoading,
                onNoteChanged = { onIntent(RegisterSubscriptionIntent.NoteChanged(it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            MyButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Registrar",
                onClick = { onIntent(RegisterSubscriptionIntent.RegisterClick()) },
                enabled = isFormValid,
                isLoading = formState.isLoading
            )
        }
    }
}

@Composable
private fun ClientDataFields(
    form: RegisterSubscriptionFormState,
    isLoading: Boolean,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onDniChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
) {
    SectionTitle("Datos del Cliente")
    Spacer(modifier = Modifier.height(8.dp))

    TwoFieldsRow(
        label1 = "Nombres (ej: Juan)",
        value1 = form.firstName,
        error1 = form.firstNameError,
        onValueChange1 = onFirstNameChanged,
        keyboardType1 = KeyboardType.Text,
        label2 = "Apellidos (ej: Pérez)",
        value2 = form.lastName,
        error2 = form.lastNameError,
        onValueChange2 = onLastNameChanged,
        keyboardType2 = KeyboardType.Text,
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(8.dp))

    MyOutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        label = "DNI (8 dígitos)",
        value = form.dni,
        errorMessage = form.dniError,
        onValueChange = onDniChanged,
        enabled = !isLoading,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        )
    )

    Spacer(modifier = Modifier.height(8.dp))

    MyOutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        label = "Teléfono (9 dígitos)",
        value = form.phone,
        errorMessage = form.phoneError,
        onValueChange = onPhoneChanged,
        enabled = !isLoading,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        )
    )
}

@Composable
private fun AddressFields(
    formState: RegisterSubscriptionState,
    form: RegisterSubscriptionFormState,
    onPlaceSelected: (Place) -> Unit,
    onPlaceSelectionCleared: () -> Unit,
    onAddressChanged: (String) -> Unit,
) {
    SectionTitle("Dirección")
    Spacer(modifier = Modifier.height(8.dp))

    if (formState.isLoadingLocation) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = "Obteniendo ubicación...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    MyAutoCompleteTextViewCompose(
        modifier = Modifier.fillMaxWidth(),
        items = form.placeList,
        label = "Lugar",
        selectedItem = form.selectedPlace,
        onItemSelected = onPlaceSelected,
        onSelectionCleared = onPlaceSelectionCleared,
        hasError = form.placeError != null,
        enabled = !formState.isLoading,
    )
    MyOutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = form.address,
        label = "Dirección completa",
        errorMessage = form.addressError,
        onValueChange = onAddressChanged,
        enabled = !formState.isLoading,
        singleLine = false,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        supportingText = if (form.address.isEmpty()) {
            { Text("Ej: Jr. Los Olivos 123, Mz A Lt 5", style = MaterialTheme.typography.bodySmall) }
        } else null
    )
}

@Composable
private fun InstallationBlock(
    formState: RegisterSubscriptionState,
    form: RegisterSubscriptionFormState,
    onIntent: (RegisterSubscriptionIntent) -> Unit,
) {
    SectionTitle("Instalación")
    Spacer(modifier = Modifier.height(8.dp))

    MyOutLinedDropDown(
        label = "Tipo de Instalación",
        items = listOf(InstallationType.FIBER, InstallationType.WIRELESS, InstallationType.ONLY_TV_FIBER),
        selected = form.installationType,
        onItemSelected = { onIntent(RegisterSubscriptionIntent.InstallationTypeSelected(it)) },
        enabled = !formState.isLoading,
    )

    MyOutLinedDropDown(
        label = "Plan",
        items = form.planList,
        selected = form.selectedPlan,
        onItemSelected = { onIntent(RegisterSubscriptionIntent.PlanSelected(it)) },
        hasError = form.planError != null,
        enabled = !formState.isLoading && form.planList.isNotEmpty(),
    )

    AnimatedVisibility(
        visible = form.installationType == InstallationType.FIBER ||
            form.installationType == InstallationType.ONLY_TV_FIBER,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        FiberOpticForm(
            formState = formState,
            onIntent = onIntent
        )
    }

    AnimatedVisibility(
        visible = form.installationType == InstallationType.WIRELESS,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("Equipamiento")
            Spacer(modifier = Modifier.height(8.dp))
            EquipmentConditionSelector(
                equipmentCondition = form.equipmentCondition,
                onConditionSelected = {
                    onIntent(RegisterSubscriptionIntent.EquipmentConditionChanged(it))
                }
            )
        }
    }
}

@Composable
private fun FacadePhotoSection(
    formState: RegisterSubscriptionState,
    onFacadePhotoClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    SectionTitle("Foto de Fachada")

    val facadePhotoUri = formState.registerSubscriptionForm.facadePhotoUri
    val hasFacadePhoto = facadePhotoUri != null
    val photoShape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(photoShape)
            .border(
                width = 1.dp,
                color = if (hasFacadePhoto) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = photoShape
            )
            .background(
                color = if (hasFacadePhoto) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = photoShape
            )
            .clickable(enabled = !formState.isLoading) {
                onFacadePhotoClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (hasFacadePhoto) {
            Image(
                painter = rememberAsyncImagePainter(facadePhotoUri.toString()),
                contentDescription = "Foto de fachada",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "Subir foto fachada",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }

    formState.registerSubscriptionForm.facadePhotoError?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ObservationsField(
    form: RegisterSubscriptionFormState,
    isLoading: Boolean,
    onNoteChanged: (String) -> Unit,
) {
    SectionTitle("Observaciones")
    Spacer(modifier = Modifier.height(8.dp))
    MyOutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = form.note,
        onValueChange = onNoteChanged,
        label = "Observaciones (opcional)",
        errorMessage = form.noteError,
        enabled = !isLoading,
        singleLine = false,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        supportingText = {
            Text(
        text = "${form.note.length}/${RegisterSubscriptionFormConstraints.MAX_NOTE_LENGTH}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun TwoFieldsRow(
    label1: String,
    value1: String,
    error1: String? = null,
    onValueChange1: (String) -> Unit,
    keyboardType1: KeyboardType = KeyboardType.Text,
    label2: String,
    value2: String,
    error2: String? = null,
    onValueChange2: (String) -> Unit,
    keyboardType2: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MyOutlinedTextField(
            modifier = Modifier.weight(1f),
            label = label1,
            value = value1,
            errorMessage = error1,
            onValueChange = onValueChange1,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType1,
                imeAction = ImeAction.Next
            )
        )
        MyOutlinedTextField(
            modifier = Modifier.weight(1f),
            label = label2,
            value = value2,
            errorMessage = error2,
            onValueChange = onValueChange2,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType2,
                imeAction = ImeAction.Next
            )
        )
    }
}

@Composable
fun InstallationTypeSelector(
    installationType: InstallationType,
    onTypeSelected: (InstallationType) -> Unit
) {
    Column {
        Row {
            RadioButtonWithLabel(
                modifier = Modifier.weight(1f),
                label = FIBER_OPTIC,
                selected = installationType == InstallationType.FIBER,
                onClick = { onTypeSelected(InstallationType.FIBER) }
            )
            RadioButtonWithLabel(
                modifier = Modifier.weight(1f),
                label = WIRELESS,
                selected = installationType == InstallationType.WIRELESS,
                onClick = { onTypeSelected(InstallationType.WIRELESS) }
            )
        }
        Row {
            RadioButtonWithLabel(
                modifier = Modifier.weight(1f),
                label = ONLY_TV,
                selected = installationType == InstallationType.ONLY_TV_FIBER,
                onClick = { onTypeSelected(InstallationType.ONLY_TV_FIBER) }
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentConditionSelector(
    equipmentCondition: EquipmentCondition,
    onConditionSelected: (EquipmentCondition) -> Unit
) {
    Column {
        Text(
            text = "Condición del Equipo",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = equipmentCondition == EquipmentCondition.LOAN,
                onClick = { onConditionSelected(EquipmentCondition.LOAN) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(EQUIPMENT_LOAN)
            }
            SegmentedButton(
                selected = equipmentCondition == EquipmentCondition.SOLD,
                onClick = { onConditionSelected(EquipmentCondition.SOLD) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(EQUIPMENT_SOLD)
            }
        }
        Text(
            text = if (equipmentCondition == EquipmentCondition.LOAN)
                "El cliente devolverá el equipo al cancelar"
            else
                "El cliente es propietario del equipo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun RadioButtonWithLabel(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = onClick
        )
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun FiberOpticForm(
    formState: RegisterSubscriptionState,
    onIntent: (RegisterSubscriptionIntent) -> Unit,
) {
    val form = formState.registerSubscriptionForm
    val installationType = form.installationType
    val showOnuSelector = installationType == InstallationType.FIBER

    Column {
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle(
            if (showOnuSelector) "Equipamiento de Fibra" else "Equipamiento"
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (showOnuSelector) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MyOutLinedDropDown(
                    modifier = Modifier.weight(1f),
                    items = form.onuList,
                    selected = form.selectedOnu,
                    label = ONU_LABEL,
                    onItemSelected = { onIntent(RegisterSubscriptionIntent.OnuSelected(it)) },
                    hasError = form.onuError != null
                )

                RefreshIcon(
                    onRefreshOnuList = { onIntent(RegisterSubscriptionIntent.RefreshOnuList) },
                    formState = formState
                )
            }
        }

        if (formState.isLoadingNearbyNapBoxes) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "Buscando cajas NAP cercanas...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        MyAutoCompleteTextViewCompose(
            items = form.napBoxList,
            label = NAP_BOX_LABEL,
            selectedItem = form.selectedNapBox,
            onItemSelected = { onIntent(RegisterSubscriptionIntent.NapBoxSelected(it)) },
            onSelectionCleared = { onIntent(RegisterSubscriptionIntent.NapBoxSelectionCleared) },
            enabled = form.selectedPlace != null,
            hasError = form.napBoxError != null
        )

        Spacer(modifier = Modifier.height(8.dp))

        EquipmentConditionSelector(
            equipmentCondition = form.equipmentCondition,
            onConditionSelected = {
                onIntent(RegisterSubscriptionIntent.EquipmentConditionChanged(it))
            }
        )
    }
}

@Composable
private fun RefreshIcon(
    onRefreshOnuList: () -> Unit,
    formState: RegisterSubscriptionState
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnimation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAnimation"
    )

    MyIconButton(
        modifier = Modifier.padding(start = 8.dp),
        onClick = onRefreshOnuList
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Actualizar lista de ONUs",
            modifier = Modifier.rotate(if (formState.isRefreshingOnuList) rotation else 0f)
        )
    }
}

private const val ONU_LABEL = "Onu"
private const val NAP_BOX_LABEL = "Caja Nap"
const val FIBER_OPTIC = "Fibra óptica"
const val WIRELESS = "Inalámbrico"
const val ONLY_TV = "Solo TV"
const val EQUIPMENT_LOAN = "Préstamo"
const val EQUIPMENT_SOLD = "Vendido"

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun RegisterSubscriptionPreview() {
    MyTheme {
        RegisterSubscriptionForm(
            modifier = Modifier.statusBarsPadding(),
            formState = RegisterSubscriptionState(
                isLoading = false,
                registerSubscriptionForm = RegisterSubscriptionFormState(
                    firstName = "",
                    lastName = "",
                    dni = "",
                    address = "",
                    phone = "",
                    price = "",
                    subscriptionDate = 5666,
                    selectedPlace = null,
                    selectedHostDevice = null,
                    location = null,
                    cpeDevice = null,
                    selectedNapBox = null,
                    coupon = "mazim",
                    note = "persequeris"
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InstallationTypeSelectorPreview() {
    MyTheme {
        InstallationTypeSelector(
            installationType = InstallationType.FIBER,
            onTypeSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "EquipmentCondition dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EquipmentConditionSelectorPreviewDark() {
    MyTheme {
        EquipmentConditionSelector(
            equipmentCondition = EquipmentCondition.LOAN,
            onConditionSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FiberOpticFormPreview() {
    MyTheme {
        FiberOpticForm(
            formState = RegisterSubscriptionState(
                registerSubscriptionForm = RegisterSubscriptionFormState(
                    installationType = InstallationType.FIBER,
                    onuList = emptyList(),
                    napBoxList = emptyList()
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RadioButtonWithLabelPreview() {
    MyTheme {
        RadioButtonWithLabel(
            modifier = Modifier.fillMaxWidth(),
            label = FIBER_OPTIC,
            selected = true,
            onClick = {}
        )
    }
}
