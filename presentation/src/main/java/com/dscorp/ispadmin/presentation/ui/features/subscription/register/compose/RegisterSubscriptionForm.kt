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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dscorp.components.components.formfields.MyOutlinedTextField
import com.dscorp.ispadmin.R
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
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.LocationCaptureMethod
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.VLAN_OPTIONS
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
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
    val isFormValid = form.isValid()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (!formState.isLoading) {
                    Modifier.testTag(RegisterSubscriptionTestTags.FORM_READY)
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RegisterSubscriptionWizardStepper(
                currentStep = formState.wizardStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (formState.wizardStep) {
                    RegisterSubscriptionWizardStep.CLIENT_LOCATION -> {
                        ClientDataFields(
                            form = form,
                            isLoading = formState.isLoading,
                            onFirstNameChanged = {
                                onIntent(RegisterSubscriptionIntent.FirstNameChanged(it))
                            },
                            onLastNameChanged = {
                                onIntent(RegisterSubscriptionIntent.LastNameChanged(it))
                            },
                            onDniChanged = { onIntent(RegisterSubscriptionIntent.DniChanged(it)) },
                            onPhoneChanged = { onIntent(RegisterSubscriptionIntent.PhoneChanged(it)) }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        AddressFields(
                            formState = formState,
                            form = form,
                            onPlaceSelected = {
                                onIntent(RegisterSubscriptionIntent.PlaceSelected(it))
                            },
                            onPlaceSelectionCleared = {
                                onIntent(RegisterSubscriptionIntent.PlaceSelectionCleared)
                            },
                            onAddressChanged = {
                                onIntent(RegisterSubscriptionIntent.AddressChanged(it))
                            },
                            onUseCurrentLocation = {
                                onIntent(RegisterSubscriptionIntent.UseCurrentLocationClicked)
                            },
                            onChooseManualLocation = {
                                onIntent(RegisterSubscriptionIntent.ChooseManualLocationClicked)
                            },
                        )
                    }

                    RegisterSubscriptionWizardStep.INSTALLATION -> {
                        InstallationBlock(
                            formState = formState,
                            form = form,
                            onIntent = onIntent
                        )
                    }

                    RegisterSubscriptionWizardStep.CONFIRMATION -> {
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
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SubscriptionSummary(
                            form = form,
                            isOfflineMode = formState.isOfflineMode,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            WizardNavigationBar(
                step = formState.wizardStep,
                isLoading = formState.isLoading,
                isFormValid = isFormValid,
                onBack = { onIntent(RegisterSubscriptionIntent.WizardBackClicked) },
                onContinue = { onIntent(RegisterSubscriptionIntent.WizardContinueClicked) },
                onRegister = { onIntent(RegisterSubscriptionIntent.RegisterClick()) },
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
        testTag1 = RegisterSubscriptionTestTags.FIRST_NAME,
        label2 = "Apellidos (ej: Pérez)",
        value2 = form.lastName,
        error2 = form.lastNameError,
        onValueChange2 = onLastNameChanged,
        keyboardType2 = KeyboardType.Text,
        testTag2 = RegisterSubscriptionTestTags.LAST_NAME,
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(8.dp))

    MyOutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RegisterSubscriptionTestTags.DNI),
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
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RegisterSubscriptionTestTags.PHONE),
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
    onUseCurrentLocation: () -> Unit,
    onChooseManualLocation: () -> Unit,
) {
    SectionTitle("Dirección")
    Spacer(modifier = Modifier.height(8.dp))

    MyOutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RegisterSubscriptionTestTags.ADDRESS),
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

    Spacer(modifier = Modifier.height(16.dp))
    LocationMethodSelector(
        selected = form.locationCaptureMethod,
        location = form.location,
        locationError = form.locationError,
        onUseCurrentLocation = onUseCurrentLocation,
        onChooseManualLocation = onChooseManualLocation,
    )

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
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RegisterSubscriptionTestTags.PLACE),
        items = form.placeList,
        label = "Lugar",
        selectedItem = form.selectedPlace,
        onItemSelected = onPlaceSelected,
        onSelectionCleared = onPlaceSelectionCleared,
        hasError = form.placeError != null,
        enabled = !formState.isLoading,
    )
}

@Composable
private fun InstallationBlock(
    formState: RegisterSubscriptionState,
    form: RegisterSubscriptionFormState,
    onIntent: (RegisterSubscriptionIntent) -> Unit,
) {
    SectionTitle("Servicio")
    Spacer(modifier = Modifier.height(8.dp))

    MyOutLinedDropDown(
        modifier = Modifier.testTag(RegisterSubscriptionTestTags.INSTALLATION_TYPE),
        label = "Tipo de Instalación",
        items = listOf(InstallationType.FIBER, InstallationType.WIRELESS, InstallationType.ONLY_TV_FIBER),
        selected = form.installationType,
        onItemSelected = { onIntent(RegisterSubscriptionIntent.InstallationTypeSelected(it)) },
        enabled = !formState.isLoading,
        itemTestTag = { index, _ -> RegisterSubscriptionTestTags.installationTypeItem(index) },
    )

    AnimatedVisibility(
        visible = form.shouldShowHostDeviceSelector(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        MyOutLinedDropDown(
            modifier = Modifier.testTag(RegisterSubscriptionTestTags.HOST_DEVICE),
            label = stringResource(R.string.host_device),
            items = form.activeCoreDevices(),
            selected = form.selectedHostDevice,
            onItemSelected = { onIntent(RegisterSubscriptionIntent.HostDeviceSelected(it)) },
            hasError = form.hostDeviceError != null,
            enabled = !formState.isLoading,
            itemTestTag = { index, _ -> RegisterSubscriptionTestTags.hostDeviceItem(index) },
        )
    }

    MyOutLinedDropDown(
        modifier = Modifier.testTag(RegisterSubscriptionTestTags.PLAN),
        label = "Plan",
        items = form.planList,
        selected = form.selectedPlan,
        onItemSelected = { onIntent(RegisterSubscriptionIntent.PlanSelected(it)) },
        hasError = form.planError != null,
        enabled = !formState.isLoading && form.planList.isNotEmpty(),
        itemTestTag = { index, _ -> RegisterSubscriptionTestTags.planItem(index) },
    )

    if (form.requiresNapBox()) {
        if (formState.isLoadingNearbyNapBoxes) {
            Row(
                modifier = Modifier
                    .testTag(RegisterSubscriptionTestTags.NEARBY_NAP_LOADING)
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
            modifier = Modifier.testTag(RegisterSubscriptionTestTags.NAP_BOX),
            items = form.napBoxList,
            label = NAP_BOX_LABEL,
            selectedItem = form.selectedNapBox,
            onItemSelected = { onIntent(RegisterSubscriptionIntent.NapBoxSelected(it)) },
            onSelectionCleared = { onIntent(RegisterSubscriptionIntent.NapBoxSelectionCleared) },
            enabled = form.selectedPlace != null,
            hasError = form.napBoxError != null
        )
    }

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

    AnimatedVisibility(
        visible = form.requiresClientIpAddress,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            MyOutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RegisterSubscriptionTestTags.CLIENT_IP),
                label = "IP del cliente",
                value = form.clientIpAddress,
                errorMessage = form.clientIpAddressError,
                onValueChange = { onIntent(RegisterSubscriptionIntent.ClientIpAddressChanged(it)) },
                enabled = !formState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
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
            .testTag(RegisterSubscriptionTestTags.FACADE_PHOTO)
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
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RegisterSubscriptionTestTags.NOTE),
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
private fun RegisterSubscriptionWizardStepper(
    currentStep: RegisterSubscriptionWizardStep,
    modifier: Modifier = Modifier,
) {
    val steps = RegisterSubscriptionWizardStep.entries
    Row(
        modifier = modifier.testTag("wizard_stepper"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, step ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = if (currentStep.ordinal >= step.ordinal) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            }
            val completed = currentStep.ordinal > step.ordinal
            val selected = currentStep == step
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            selected -> MaterialTheme.colorScheme.primary
                            completed -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .testTag("wizard_step_${index + 1}"),
                contentAlignment = Alignment.Center,
            ) {
                if (completed) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Paso ${index + 1} completado",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardNavigationBar(
    step: RegisterSubscriptionWizardStep,
    isLoading: Boolean,
    isFormValid: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onRegister: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (step != RegisterSubscriptionWizardStep.CLIENT_LOCATION) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .testTag(RegisterSubscriptionTestTags.WIZARD_BACK)
            ) {
                Text("Atrás")
            }
        }
        if (step == RegisterSubscriptionWizardStep.CONFIRMATION) {
            MyButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag(RegisterSubscriptionTestTags.SUBMIT),
                text = "Registrar suscripción",
                onClick = onRegister,
                enabled = isFormValid,
                isLoading = isLoading
            )
        } else {
            MyButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE),
                text = "Continuar",
                onClick = onContinue,
                enabled = !isLoading,
                isLoading = isLoading
            )
        }
    }
}

@Composable
private fun SubscriptionSummary(
    form: RegisterSubscriptionFormState,
    isOfflineMode: Boolean,
) {
    SectionTitle("Resumen de la suscripción")
    Spacer(modifier = Modifier.height(8.dp))
    SummaryRow("Cliente", "${form.firstName} ${form.lastName}".trim())
    SummaryRow("DNI", form.dni)
    SummaryRow("Teléfono", form.phone)
    SummaryRow("Dirección", form.address)
    form.location?.let { latLng ->
        SummaryRow(
            "Coordenadas",
            "${"%.6f".format(latLng.latitude)}, ${"%.6f".format(latLng.longitude)}"
        )
    }
    SummaryRow("Lugar", form.selectedPlace?.name.orEmpty())
    SummaryRow(
        "Tipo",
        when (form.installationType) {
            InstallationType.FIBER -> FIBER_OPTIC
            InstallationType.WIRELESS -> WIRELESS
            InstallationType.ONLY_TV_FIBER -> ONLY_TV
        }
    )
    SummaryRow("Plan", form.selectedPlan?.name.orEmpty())
    if (form.requiresNapBox()) {
        SummaryRow("NAP", form.selectedNapBox?.code?.takeIf { it.isNotBlank() }
            ?: form.selectedNapBox?.id.orEmpty())
    }
    if (form.requiresOnu()) {
        SummaryRow("ONU", form.selectedOnu?.sn.orEmpty())
        SummaryRow("VLAN", form.vlan)
    }
    if (form.requiresWifiConfig()) {
        val ssid24Label = if (form.useDifferentWifiNames) "SSID 2.4" else "WiFi"
        SummaryRow(ssid24Label, form.wifiSsid24)
        if (form.useDifferentWifiNames) {
            SummaryRow("SSID 5", form.wifiSsid5)
        } else if (form.wifiSsid24.isNotBlank()) {
            SummaryRow("SSID 5", form.resolvedWifiSsid5())
        }
    }
    SummaryRow(
        "Equipo",
        if (form.equipmentCondition == EquipmentCondition.LOAN) EQUIPMENT_LOAN else EQUIPMENT_SOLD
    )
    if (isOfflineMode) {
        SummaryRow("IP", form.clientIpAddress)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TwoFieldsRow(
    label1: String,
    value1: String,
    error1: String? = null,
    onValueChange1: (String) -> Unit,
    keyboardType1: KeyboardType = KeyboardType.Text,
    testTag1: String? = null,
    label2: String,
    value2: String,
    error2: String? = null,
    onValueChange2: (String) -> Unit,
    keyboardType2: KeyboardType = KeyboardType.Text,
    testTag2: String? = null,
    enabled: Boolean = true,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MyOutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .then(if (testTag1 != null) Modifier.testTag(testTag1) else Modifier),
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
            modifier = Modifier
                .weight(1f)
                .then(if (testTag2 != null) Modifier.testTag(testTag2) else Modifier),
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
    onClick: () -> Unit,
    textStyle: TextStyle? = null,
    labelStartPadding: Dp = 8.dp,
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
        Text(
            text = label,
            modifier = Modifier.padding(start = labelStartPadding),
            style = textStyle ?: LocalTextStyle.current,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                    modifier = Modifier
                        .weight(1f)
                        .testTag(RegisterSubscriptionTestTags.ONU),
                    items = form.onuList,
                    selected = form.selectedOnu,
                    label = ONU_LABEL,
                    onItemSelected = { onIntent(RegisterSubscriptionIntent.OnuSelected(it)) },
                    hasError = form.onuError != null,
                    itemTestTag = { index, _ -> RegisterSubscriptionTestTags.onuItem(index) },
                )

                RefreshIcon(
                    onRefreshOnuList = { onIntent(RegisterSubscriptionIntent.RefreshOnuList) },
                    formState = formState
                )
            }

            MyOutLinedDropDown(
                modifier = Modifier.testTag(RegisterSubscriptionTestTags.VLAN),
                items = VLAN_OPTIONS,
                selected = VLAN_OPTIONS.firstOrNull { it.value == form.vlan },
                label = VLAN_LABEL,
                onItemSelected = { onIntent(RegisterSubscriptionIntent.OnVlanChanged(it.value)) },
                enabled = !formState.isLoading,
                isItemEnabled = { it.selectable },
                itemTestTag = { index, _ -> RegisterSubscriptionTestTags.vlanItem(index) },
            )

            Spacer(modifier = Modifier.height(8.dp))
            WifiFields(
                form = form,
                enabled = !formState.isLoading,
                onIntent = onIntent
            )
        }

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
private fun WifiFields(
    form: RegisterSubscriptionFormState,
    enabled: Boolean,
    onIntent: (RegisterSubscriptionIntent) -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val ssid24Label = if (form.useDifferentWifiNames) "SSID 2.4 GHz" else "Nombre de red"

    Text(
        text = "WiFi ONU",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            modifier = Modifier.testTag(RegisterSubscriptionTestTags.WIFI_DIFFERENT_NAMES),
            checked = form.useDifferentWifiNames,
            onCheckedChange = {
                onIntent(RegisterSubscriptionIntent.UseDifferentWifiNamesChanged(it))
            },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        Text(
            text = "Usar nombres diferentes por frecuencia",
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    MyOutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RegisterSubscriptionTestTags.WIFI_SSID_24),
        label = ssid24Label,
        value = form.wifiSsid24,
        errorMessage = form.wifiSsid24Error,
        onValueChange = { onIntent(RegisterSubscriptionIntent.WifiSsid24Changed(it)) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        )
    )

    if (!form.useDifferentWifiNames && form.wifiSsid24.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        val derivedSsid5 = form.resolvedWifiSsid5()
        Text(
            text = "5 GHz: $derivedSsid5",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = "SSID 5 GHz derivado $derivedSsid5"
            }
        )
    }

    if (form.useDifferentWifiNames) {
        Spacer(modifier = Modifier.height(8.dp))
        MyOutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RegisterSubscriptionTestTags.WIFI_SSID_5),
            label = "SSID 5 GHz",
            value = form.wifiSsid5,
            errorMessage = form.wifiSsid5Error,
            onValueChange = { onIntent(RegisterSubscriptionIntent.WifiSsid5Changed(it)) },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    MyOutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RegisterSubscriptionTestTags.WIFI_PASSWORD_24),
        label = "Clave WiFi",
        value = form.wifiPassword24,
        errorMessage = form.wifiPassword24Error,
        onValueChange = { onIntent(RegisterSubscriptionIntent.WifiPassword24Changed(it)) },
        enabled = enabled,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(
                modifier = Modifier.testTag(RegisterSubscriptionTestTags.WIFI_PASSWORD_24_TOGGLE),
                onClick = { passwordVisible = !passwordVisible }
            ) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = if (passwordVisible) {
                        "Ocultar clave WiFi"
                    } else {
                        "Mostrar clave WiFi"
                    }
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        )
    )
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
        modifier = Modifier
            .padding(start = 8.dp)
            .testTag(RegisterSubscriptionTestTags.REFRESH_ONU),
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
private const val VLAN_LABEL = "VLAN"
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

@Composable
private fun LocationMethodSelector(
    selected: LocationCaptureMethod,
    location: com.google.android.gms.maps.model.LatLng?,
    locationError: String?,
    onUseCurrentLocation: () -> Unit,
    onChooseManualLocation: () -> Unit,
) {
    Text(
        text = "Ubicación del cliente",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RadioButtonWithLabel(
            modifier = Modifier
                .weight(1f)
                .testTag(RegisterSubscriptionTestTags.LOCATION_METHOD_CURRENT),
            label = "Ubi. Actual",
            selected = selected == LocationCaptureMethod.CURRENT,
            onClick = onUseCurrentLocation,
            textStyle = MaterialTheme.typography.bodySmall,
            labelStartPadding = 2.dp,
        )
        RadioButtonWithLabel(
            modifier = Modifier
                .weight(1f)
                .testTag(RegisterSubscriptionTestTags.LOCATION_METHOD_MANUAL),
            label = "Ubi. Manualmente",
            selected = selected == LocationCaptureMethod.MANUAL,
            onClick = onChooseManualLocation,
            textStyle = MaterialTheme.typography.bodySmall,
            labelStartPadding = 2.dp,
        )
    }
    location?.let { latLng ->
        Text(
            text = "Coordenadas: ${"%.6f".format(latLng.latitude)}, ${"%.6f".format(latLng.longitude)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag(RegisterSubscriptionTestTags.LOCATION_COORDINATES)
        )
    }
    locationError?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
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
