package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

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
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.MyAutoCompleteTextViewCompose
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.ispadmin.presentation.ui.components.MyIconButton
import com.dscorp.ispadmin.presentation.ui.components.MyOutLinedDropDown
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter
import androidx.compose.ui.draw.clip

@Composable
fun RegisterSubscriptionForm(
    modifier: Modifier = Modifier,
    formState: RegisterSubscriptionState,
    onFirstNameChanged: (String) -> Unit = {},
    onLastNameChanged: (String) -> Unit = {},
    onDniChanged: (String) -> Unit = {},
    onAddressChanged: (String) -> Unit = {},
    onPhoneChanged: (String) -> Unit = {},
    onPlanSelected: (PlanResponse) -> Unit = {},
    onOnuSelected: (Onu) -> Unit = {},
    onPlaceSelected: (Place) -> Unit = {},
    onNapBoxSelected: (NapBoxResponse) -> Unit = {},
    onPLaceSelectionCleared: () -> Unit = {},
    onNapBoxSelectionCleared: () -> Unit = {},
    onInstallationTypeSelected: (InstallationType) -> Unit = {},
    onRefreshOnuList: () -> Unit = {},
    onNoteChanged: (String) -> Unit = {},
    onFacadePhotoClick: () -> Unit = {},
    onEquipmentConditionChanged: (EquipmentCondition) -> Unit = {},
    onRegisterClick: () -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(scrollState)
        ) {
            SectionTitle("Datos del Cliente")
            Spacer(modifier = Modifier.height(8.dp))

            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = "Nombres (ej: Juan)",
                value = formState.registerSubscriptionForm.firstName,
                errorMessage = formState.registerSubscriptionForm.firstNameError,
                onValueChange = onFirstNameChanged,
                enabled = !formState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = "Apellidos (ej: Pérez)",
                value = formState.registerSubscriptionForm.lastName,
                errorMessage = formState.registerSubscriptionForm.lastNameError,
                onValueChange = onLastNameChanged,
                enabled = !formState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = "DNI (8 dígitos)",
                value = formState.registerSubscriptionForm.dni,
                errorMessage = formState.registerSubscriptionForm.dniError,
                onValueChange = onDniChanged,
                enabled = !formState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = "Teléfono (9 dígitos)",
                value = formState.registerSubscriptionForm.phone,
                errorMessage = formState.registerSubscriptionForm.phoneError,
                onValueChange = onPhoneChanged,
                enabled = !formState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                        "Obteniendo ubicación...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            MyAutoCompleteTextViewCompose(
                modifier = Modifier.fillMaxWidth(),
                items = formState.registerSubscriptionForm.placeList,
                label = "Lugar",
                selectedItem = formState.registerSubscriptionForm.selectedPlace,
                onItemSelected = onPlaceSelected,
                onSelectionCleared = onPLaceSelectionCleared,
                hasError = formState.registerSubscriptionForm.placeError != null,
                enabled = !formState.isLoading,
            )
            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = formState.registerSubscriptionForm.address,
                label = "Dirección completa",
                errorMessage = formState.registerSubscriptionForm.addressError,
                onValueChange = onAddressChanged,
                enabled = !formState.isLoading,
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                supportingText = if (formState.registerSubscriptionForm.address.isEmpty()) {
                    { Text("Ej: Jr. Los Olivos 123, Mz A Lt 5", style = MaterialTheme.typography.bodySmall) }
                } else null
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("Instalación")
            Spacer(modifier = Modifier.height(8.dp))
            
            MyOutLinedDropDown(
                label = "Tipo de Instalación",
                items = listOf(InstallationType.FIBER, InstallationType.WIRELESS, InstallationType.ONLY_TV_FIBER),
                selected = formState.registerSubscriptionForm.installationType,
                onItemSelected = onInstallationTypeSelected,
                enabled = !formState.isLoading,
            )

            MyOutLinedDropDown(
                label = "Plan",
                items = formState.registerSubscriptionForm.planList,
                selected = formState.registerSubscriptionForm.selectedPlan,
                onItemSelected = onPlanSelected,
                hasError = formState.registerSubscriptionForm.planError != null,
                enabled = !formState.isLoading && formState.registerSubscriptionForm.planList.isNotEmpty(),
            )

            AnimatedVisibility(
                visible = formState.registerSubscriptionForm.installationType == InstallationType.FIBER ||
                        formState.registerSubscriptionForm.installationType == InstallationType.ONLY_TV_FIBER,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FiberOpticForm(
                    formState = formState,
                    onOnuSelected = onOnuSelected,
                    onNapBoxSelected = onNapBoxSelected,
                    onNapBoxSelectionCleared = onNapBoxSelectionCleared,
                    onRefreshOnuList = onRefreshOnuList,
                    equipmentCondition = formState.registerSubscriptionForm.equipmentCondition,
                    onEquipmentConditionChanged = onEquipmentConditionChanged
                )
            }
            
            AnimatedVisibility(
                visible = formState.registerSubscriptionForm.installationType == InstallationType.WIRELESS,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle("Equipamiento")
                    Spacer(modifier = Modifier.height(8.dp))
                    EquipmentConditionSelector(
                        equipmentCondition = formState.registerSubscriptionForm.equipmentCondition,
                        onConditionSelected = onEquipmentConditionChanged
                    )
                }
            }

            //foto fachada
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
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionTitle("Observaciones")
            Spacer(modifier = Modifier.height(8.dp))
            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = formState.registerSubscriptionForm.note,
                onValueChange = onNoteChanged,
                label = "Observaciones (opcional)",
                enabled = !formState.isLoading,
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                supportingText = {
                    Text(
                        text = "${formState.registerSubscriptionForm.note.length}/180",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            MyButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Registrar",
                onClick = onRegisterClick,
                enabled = formState.registerSubscriptionForm.isValid() &&
                        formState.registerSubscriptionForm.facadePhotoUri != null,
                isLoading = formState.isLoading
            )
        }
    }
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
    keyboardType2: KeyboardType = KeyboardType.Text
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MyOutlinedTextField(
            modifier = Modifier.weight(1f),
            label = label1,
            value = value1,
            errorMessage = error1,
            onValueChange = onValueChange1,
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
        modifier = modifier
            .clickable(onClick = onClick)
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
    onOnuSelected: (Onu) -> Unit,
    onNapBoxSelected: (NapBoxResponse) -> Unit,
    onNapBoxSelectionCleared: () -> Unit,
    onRefreshOnuList: () -> Unit,
    equipmentCondition: EquipmentCondition,
    onEquipmentConditionChanged: (EquipmentCondition) -> Unit
) {
    val installationType = formState.registerSubscriptionForm.installationType
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
                    items = formState.registerSubscriptionForm.onuList,
                    selected = formState.registerSubscriptionForm.selectedOnu,
                    label = ONU_LABEL,
                    onItemSelected = onOnuSelected,
                    hasError = formState.registerSubscriptionForm.onuError != null
                )

                RefreshIcon(onRefreshOnuList, formState)
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
                    "Buscando cajas NAP cercanas...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        MyAutoCompleteTextViewCompose(
            items = formState.registerSubscriptionForm.napBoxList,
            label = NAP_BOX_LABEL,
            selectedItem = formState.registerSubscriptionForm.selectedNapBox,
            onItemSelected = onNapBoxSelected,
            onSelectionCleared = onNapBoxSelectionCleared,
            enabled = formState.registerSubscriptionForm.selectedPlace != null,
            hasError = formState.registerSubscriptionForm.napBoxError != null
        )

        Spacer(modifier = Modifier.height(8.dp))

        EquipmentConditionSelector(
            equipmentCondition = equipmentCondition,
            onConditionSelected = onEquipmentConditionChanged
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
            contentDescription = "",
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
                error = "leo",
                registeredSubscription = null,
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
