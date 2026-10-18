package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Router
import com.dscorp.ispadmin.presentation.theme.IspTheme
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
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
import com.dscorp.ispadmin.domain.model.AccessMode
import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.canOpenRegistrationForm
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
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.canAdvanceWizardStep
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.wizardFieldsFor
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
    val missingFields = when (formState.wizardStep) {
        RegisterSubscriptionWizardStep.ONU_SELECTION,
        RegisterSubscriptionWizardStep.ONU_CONFIRMATION ->
            if (form.selectedOnu == null) 1 else 0
        RegisterSubscriptionWizardStep.WAITING_FOR_ACS -> 0
        RegisterSubscriptionWizardStep.CONFIRMATION ->
            form.blockingFields().count { form.validate(it) != null }
        else -> wizardFieldsFor(formState.wizardStep, form)
            .count { form.validate(it) != null }
    }
    val navigationHint = when {
        formState.wizardStep == RegisterSubscriptionWizardStep.WAITING_FOR_ACS -> null
        missingFields == 0 -> null
        formState.wizardStep == RegisterSubscriptionWizardStep.ONU_SELECTION ->
            "Selecciona una ONU para autorizarla en la OLT."
        formState.wizardStep == RegisterSubscriptionWizardStep.ONU_CONFIRMATION ->
            "Revisa los datos del equipo antes de autorizarlo."
        formState.wizardStep == RegisterSubscriptionWizardStep.CONFIRMATION ->
            "Completa los $missingFields datos pendientes para registrar."
        else -> "Completa los $missingFields campos obligatorios para continuar."
    }
    val scrollState = rememberScrollState()

    LaunchedEffect(formState.wizardStep) {
        scrollState.animateScrollTo(0)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
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
        val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        Column(modifier = Modifier.fillMaxSize()) {
            RegisterWizardHeader(
                currentStep = formState.wizardStep,
                preauthorizationEnabled = formState.preauthorizationEnabled,
                onClose = {
                    // Closing an ONU-first registration goes through the cancel flow, which cleans the OLT authorization.
                    if (formState.preauthorizationEnabled) onIntent(RegisterSubscriptionIntent.CancelOnuRegistration)
                    else backDispatcher?.onBackPressed()
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
                    .verticalScroll(scrollState)
            ) {
                AnimatedContent(
                    targetState = formState.wizardStep,
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(tween(180)) + slideInHorizontally { it / 18 }) togetherWith
                            (androidx.compose.animation.fadeOut(tween(120)) + slideOutHorizontally { -it / 18 })
                    },
                    label = "registration_step_content",
                ) { step ->
                    Column {
                        if (formState.preauthorizationEnabled && step in ACS_STATUS_STEPS) {
                            AcsStatusBanner(formState)
                        }
                        when (step) {
                            RegisterSubscriptionWizardStep.ONU_SELECTION,
                            RegisterSubscriptionWizardStep.ONU_CONFIRMATION ->
                                OnuListBlock(
                                    onus = form.onuList,
                                    selected = form.selectedOnu,
                                    error = form.onuError,
                                    loading = formState.isLoading,
                                    refreshIcon = {
                                        RefreshIcon(onRefreshOnuList = { onIntent(RegisterSubscriptionIntent.RefreshOnuList) }, formState = formState)
                                    },
                                    onSelect = { onIntent(RegisterSubscriptionIntent.OnuSelected(it)) },
                                )
                            RegisterSubscriptionWizardStep.WAITING_FOR_ACS -> AcsWaitingBlock(formState)
                            RegisterSubscriptionWizardStep.CLIENT_LOCATION -> {
                                ClientDataFields(
                                    form = form,
                                    isLoading = formState.isLoading,
                                    identityLocked = formState.identityLocked,
                                    customerBanner = formState.customerServiceNumber?.let { number ->
                                        "Cliente: ${form.firstName} ${form.lastName} · servicio $number"
                                    },
                                    onFirstNameChanged = {
                                        onIntent(RegisterSubscriptionIntent.FirstNameChanged(it))
                                    },
                                    onLastNameChanged = {
                                        onIntent(RegisterSubscriptionIntent.LastNameChanged(it))
                                    },
                                    onDniChanged = { onIntent(RegisterSubscriptionIntent.DniChanged(it)) },
                                    onPhoneChanged = { onIntent(RegisterSubscriptionIntent.PhoneChanged(it)) },
                                    dniWarning = formState.dniWarning,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
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
                                FacadePhotoSection(
                                    formState = formState,
                                    onFacadePhotoClick = onFacadePhotoClick
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                ObservationsField(
                                    form = form,
                                    isLoading = formState.isLoading,
                                    onNoteChanged = { onIntent(RegisterSubscriptionIntent.NoteChanged(it)) }
                                )
                            }

                            RegisterSubscriptionWizardStep.CONFIRMATION -> {
                                SectionTitle("Revisa la suscripción")
                                Text(
                                    text = "Verifica los datos antes de registrar. Puedes volver atrás para editarlos.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SubscriptionSummary(
                                    form = form,
                                    isOfflineMode = formState.isOfflineMode && !formState.preauthorizationEnabled,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .background(IspTheme.colors.card)
            ) {
                HorizontalDivider(color = IspTheme.colors.cardBorder)
                val selectedOnu = form.selectedOnu
                when {
                    formState.wizardStep in ONU_STEPS && selectedOnu != null -> RegisterWizardFooterLine(selectedOnu.chosenLabel())
                    navigationHint != null -> RegisterWizardFooterLine(navigationHint)
                    formState.wizardStep == RegisterSubscriptionWizardStep.CONFIRMATION && formState.submitBlockedReason == null ->
                        RegisterWizardFooterLine("Los ${registrationProgressPhases(formState.preauthorizationEnabled).size} pasos están completos", ok = true)
                    formState.wizardStep != RegisterSubscriptionWizardStep.WAITING_FOR_ACS && formState.wizardStep !in ONU_STEPS ->
                        RegisterWizardFooterLine("Todo listo para continuar", ok = true)
                }
                formState.submitBlockedReason?.let { reason ->
                    RegisterWizardFooterLine(reason, error = true, modifier = Modifier.testTag(RegisterSubscriptionTestTags.SUBMIT_BLOCKED_REASON))
                }
                WizardNavigationBar(
                    step = formState.wizardStep,
                    isLoading = formState.isLoading,
                    canContinue = if (formState.wizardStep == RegisterSubscriptionWizardStep.ONU_SELECTION ||
                        formState.wizardStep == RegisterSubscriptionWizardStep.ONU_CONFIRMATION
                    ) form.selectedOnu != null else canAdvanceWizardStep(formState.wizardStep, form),
                    canRegister = form.isValid() && if (formState.preauthorizationOperation != null) {
                        !form.facadePhotoUrl.isNullOrBlank()
                    } else {
                        form.facadePhotoUri != null || !form.facadePhotoUrl.isNullOrBlank()
                    },
                    onBack = { onIntent(RegisterSubscriptionIntent.WizardBackClicked) },
                    onContinue = { onIntent(RegisterSubscriptionIntent.WizardContinueClicked) },
                    onRegister = { onIntent(RegisterSubscriptionIntent.RegisterClick()) },
                    retryLabel = when (formState.preauthorizationOperation?.state) {
                        "CANCEL_FAILED" -> "Reintentar limpieza"
                        "CANCEL_REQUESTED", "CANCELLING" -> "Consultar limpieza"
                        else -> "Reintentar"
                    },
                    onRetryAcs = {
                        if (formState.preauthorizationOperation?.state in setOf("CANCEL_FAILED", "CANCEL_REQUESTED", "CANCELLING")) {
                            onIntent(RegisterSubscriptionIntent.CancelOnuRegistration)
                        } else onIntent(RegisterSubscriptionIntent.RetryOnuRegistration)
                    },
                    onCancelPreauthorization = { onIntent(RegisterSubscriptionIntent.CancelOnuRegistration) },
                )
            }
        }
    }
}

private val ONU_STEPS = setOf(RegisterSubscriptionWizardStep.ONU_SELECTION, RegisterSubscriptionWizardStep.ONU_CONFIRMATION)

private val ACS_STATUS_STEPS = setOf(
    RegisterSubscriptionWizardStep.CLIENT_LOCATION,
    RegisterSubscriptionWizardStep.INSTALLATION,
    RegisterSubscriptionWizardStep.CONFIRMATION,
)

/** ACS state shown on top of every form step: still connecting, or ready to register. */
@Composable
private fun AcsStatusBanner(formState: RegisterSubscriptionState) {
    val operation = formState.preauthorizationOperation ?: return
    when {
        formState.acsPending -> AcsPendingBanner(operation.serial)
        operation.canOpenRegistrationForm() -> EquipmentReadyBanner(formState)
        else -> return
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun AcsPendingBanner(serial: String) {
    val colors = IspTheme.colors
    RegisterNote(colors.infoContainer, colors.info, Icons.Outlined.Router, Modifier.testTag(RegisterSubscriptionTestTags.ACS_PENDING_BANNER)) {
        Text("ONU $serial", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("Autorizada en la OLT ✓ · conectando con la gestión remota…", fontSize = 13.sp, lineHeight = 18.sp)
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(4.dp).clip(RoundedCornerShape(50)),
            color = colors.info,
            trackColor = colors.card,
        )
        Text("Llena los datos mientras tanto; podrás registrar cuando el equipo conecte.", fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun EquipmentReadyBanner(formState: RegisterSubscriptionState) {
    val operation = formState.preauthorizationOperation ?: return
    val colors = IspTheme.colors
    RegisterNote(colors.successContainer, colors.success, Icons.Outlined.Router) {
        Text("Equipo conectado con la gestión remota ✓", fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("ONU ${operation.serial} lista para registrar.", fontSize = 13.sp, lineHeight = 18.sp)
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
    dniWarning: String? = null,
    identityLocked: Boolean = false,
    customerBanner: String? = null,
) {
    val colors = IspTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        customerBanner?.let { banner ->
            RegisterNote(colors.infoContainer, colors.info, Icons.Outlined.Person) {
                Text(banner, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Sus datos de titular vienen de la suscripción existente.", fontSize = 13.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RegisterTextField(
                "Nombres *", form.firstName, onFirstNameChanged, Modifier.weight(1f),
                fieldModifier = Modifier.testTag(RegisterSubscriptionTestTags.FIRST_NAME),
                error = form.firstNameError, enabled = !isLoading && !identityLocked,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            RegisterTextField(
                "Apellidos *", form.lastName, onLastNameChanged, Modifier.weight(1f),
                fieldModifier = Modifier.testTag(RegisterSubscriptionTestTags.LAST_NAME),
                error = form.lastNameError, enabled = !isLoading && !identityLocked,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                RegisterTextField(
                    "DNI *", form.dni, onDniChanged,
                    fieldModifier = Modifier.testTag(RegisterSubscriptionTestTags.DNI),
                    error = form.dniError, enabled = !isLoading && !identityLocked,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
                dniWarning?.let {
                    Text(it, fontSize = 12.sp, color = colors.warning, modifier = Modifier.padding(top = 4.dp).testTag(RegisterSubscriptionTestTags.DNI_WARNING))
                }
            }
            RegisterTextField(
                "Teléfono *", form.phone, onPhoneChanged, Modifier.weight(1f),
                fieldModifier = Modifier.testTag(RegisterSubscriptionTestTags.PHONE),
                error = form.phoneError, enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            )
        }
    }
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RegisterTextField(
            "Dirección *", form.address, onAddressChanged,
            fieldModifier = Modifier.testTag(RegisterSubscriptionTestTags.ADDRESS),
            error = form.addressError,
            helper = "Calle, número y una referencia: «Jr. Los Olivos 123, frente al parque»".takeIf { form.address.isEmpty() },
            enabled = !formState.isLoading,
            singleLine = false,
            minHeight = 88.dp,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        LocationMethodSelector(
            selected = form.locationCaptureMethod,
            location = form.location,
            locationError = form.locationError,
            loading = formState.isLoadingLocation,
            onUseCurrentLocation = onUseCurrentLocation,
            onChooseManualLocation = onChooseManualLocation,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RegisterFieldLabel("Lugar *")
            MyAutoCompleteTextViewCompose(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RegisterSubscriptionTestTags.PLACE),
                items = form.placeList,
                label = "Distrito o localidad *",
                selectedItem = form.selectedPlace,
                onItemSelected = onPlaceSelected,
                onSelectionCleared = onPlaceSelectionCleared,
                hasError = form.placeError != null,
                enabled = !formState.isLoading,
            )
            Text("Escribe para filtrar la lista de lugares.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InstallationBlock(
    formState: RegisterSubscriptionState,
    form: RegisterSubscriptionFormState,
    onIntent: (RegisterSubscriptionIntent) -> Unit,
) {
    SectionTitle("Servicio y equipo")
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
        visible = form.shouldShowAccessModeSelector(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        WanAccessModeSelector(
            selected = form.accessMode,
            onSelected = { onIntent(RegisterSubscriptionIntent.AccessModeSelected(it)) },
            enabled = !formState.isLoading,
        )
    }

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

    SectionTitle("Foto de fachada")
    Text(
        text = "Añade una foto de la fachada para completar el registro.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))

    val facadePhotoUri = formState.registerSubscriptionForm.facadePhotoUri
    val facadePhotoSource = formState.registerSubscriptionForm.facadePhotoUrl ?: facadePhotoUri?.toString()
    val hasFacadePhoto = facadePhotoSource != null
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
            .clickable(enabled = !formState.isLoading, role = Role.Button) {
                onFacadePhotoClick()
            }
            .semantics {
                contentDescription = if (hasFacadePhoto) {
                    "Cambiar foto de fachada"
                } else {
                    "Añadir foto de fachada"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (hasFacadePhoto) {
            Image(
                painter = rememberAsyncImagePainter(facadePhotoSource),
                contentDescription = "Foto de fachada",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "Añadir foto de fachada",
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
private fun AcsWaitingBlock(formState: RegisterSubscriptionState) {
    SectionTitle("Conectando el equipo")
    val operation = formState.preauthorizationOperation
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (operation?.canOpenRegistrationForm() == true) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = when {
                        operation?.state in setOf("CANCEL_REQUESTED", "CANCELLING") -> "Limpiando la autorización"
                        operation?.phase == "OLT_AUTHORIZATION" -> "Comprobando autorización en OLT"
                        operation?.canOpenRegistrationForm() == true -> "Equipo conectado"
                        else -> "Esperando conexión con ACS"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    operation?.phase == "OLT_AUTHORIZATION" ->
                        "La autorización todavía no se confirma. Puedes volver a comprobarla o cancelar y limpiar el intento."
                    operation?.canOpenRegistrationForm() == true ->
                        "La ONU ya respondió a ACS. Ya puedes completar los datos de la suscripción."
                    else ->
                        "La ONU fue autorizada. El formulario estará listo cuando ACS reciba su primer mensaje TR-069."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Serial: ${operation?.serial ?: formState.registerSubscriptionForm.selectedOnu?.sn.orEmpty()}",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
    if (operation?.state == "CANCEL_REQUESTED" || operation?.state == "CANCELLING") {
        Text("Se está limpiando la autorización de la ONU en la OLT…", modifier = Modifier.padding(top = 8.dp))
    }
    val failure = operation?.operationFailure ?: operation?.checkpoints?.firstOrNull { it.failure != null }?.failure
    failure?.let {
        Text("${it.code}: ${it.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        it.technicalDetails?.let { details -> Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    formState.preauthorizationError?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun WizardNavigationBar(
    step: RegisterSubscriptionWizardStep,
    isLoading: Boolean,
    canContinue: Boolean,
    canRegister: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onRegister: () -> Unit,
    retryLabel: String,
    onRetryAcs: () -> Unit,
    onCancelPreauthorization: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (step) {
            RegisterSubscriptionWizardStep.WAITING_FOR_ACS -> {
                // The header close carries the cancel test tag on every step.
                RegisterTextAction("Cancelar", enabled = !isLoading, onClick = onCancelPreauthorization)
                Spacer(Modifier.weight(1f))
                RegisterPrimaryButton(retryLabel, enabled = !isLoading, loading = isLoading, onClick = onRetryAcs)
            }
            RegisterSubscriptionWizardStep.ONU_SELECTION, RegisterSubscriptionWizardStep.ONU_CONFIRMATION -> {
                RegisterTextAction("Cancelar", enabled = !isLoading, onClick = onCancelPreauthorization)
                Spacer(Modifier.weight(1f))
                RegisterPrimaryButton("Autorizar y continuar", enabled = !isLoading && canContinue, loading = isLoading,
                    modifier = Modifier.testTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE), onClick = onContinue)
            }
            RegisterSubscriptionWizardStep.CONFIRMATION -> {
                RegisterTextAction("Atrás", enabled = !isLoading, modifier = Modifier.testTag(RegisterSubscriptionTestTags.WIZARD_BACK), onClick = onBack)
                RegisterPrimaryButton("Registrar suscripción", enabled = !isLoading && canRegister, loading = isLoading,
                    modifier = Modifier.weight(1f).testTag(RegisterSubscriptionTestTags.SUBMIT), onClick = onRegister)
            }
            else -> {
                // The ONU cannot be changed once authorized, so the client step has no way back.
                if (step != RegisterSubscriptionWizardStep.CLIENT_LOCATION) {
                    RegisterTextAction("Atrás", enabled = !isLoading, modifier = Modifier.testTag(RegisterSubscriptionTestTags.WIZARD_BACK), onClick = onBack)
                }
                RegisterPrimaryButton("Continuar", enabled = !isLoading && canContinue, loading = isLoading,
                    modifier = Modifier.weight(1f).padding(start = if (step == RegisterSubscriptionWizardStep.CLIENT_LOCATION) 12.dp else 0.dp)
                        .testTag(RegisterSubscriptionTestTags.WIZARD_CONTINUE), onClick = onContinue)
            }
        }
    }
}

@Composable
private fun SubscriptionSummary(
    form: RegisterSubscriptionFormState,
    isOfflineMode: Boolean,
) {
    SectionTitle("Resumen de la suscripción")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SummaryRow("Cliente", "${form.firstName} ${form.lastName}".trim())
            SummaryRow("DNI", form.dni)
            SummaryRow("Teléfono", form.phone)
            SummaryRow("Dirección", form.address)
            form.location?.let { latLng ->
                SummaryRow(
                    "Ubicación",
                    "${"%.6f".format(latLng.latitude)}, ${"%.6f".format(latLng.longitude)}"
                )
            }
            SummaryRow("Lugar", form.selectedPlace?.name.orEmpty())
            SummaryRow(
                "Tipo de instalación",
                when (form.installationType) {
                    InstallationType.FIBER -> FIBER_OPTIC
                    InstallationType.WIRELESS -> WIRELESS
                    InstallationType.ONLY_TV_FIBER -> ONLY_TV
                }
            )
            SummaryRow("Plan", form.selectedPlan?.name.orEmpty())
            SummaryRow("Observaciones", form.note)
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
                "Condición del equipo",
                if (form.equipmentCondition == EquipmentCondition.LOAN) EQUIPMENT_LOAN else EQUIPMENT_SOLD
            )
            if (isOfflineMode) {
                SummaryRow("IP", form.clientIpAddress)
            }
        }
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

@Composable
fun WanAccessModeSelector(
    selected: AccessMode,
    onSelected: (AccessMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.testTag(RegisterSubscriptionTestTags.ACCESS_MODE)) {
        Text(
            text = "Tipo de WAN",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            AccessMode.registerChoices().forEach { mode ->
                val tag = if (mode == AccessMode.STATIC_IP) {
                    RegisterSubscriptionTestTags.ACCESS_MODE_STATIC_IP
                } else {
                    RegisterSubscriptionTestTags.ACCESS_MODE_PPPOE
                }
                val isSelected = selected == mode ||
                    (mode == AccessMode.PPPOE_DYNAMIC && selected.usesPppoe())
                RadioButtonWithLabel(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(tag),
                    label = mode.registerLabel(),
                    selected = isSelected,
                    onClick = { if (enabled) onSelected(mode) },
                )
            }
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
    val showOnuSelector =
        installationType == InstallationType.FIBER && !formState.preauthorizationEnabled

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
        }

        if (installationType == InstallationType.FIBER) {
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
    loading: Boolean,
    onUseCurrentLocation: () -> Unit,
    onChooseManualLocation: () -> Unit,
) {
    val colors = IspTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ubicación *", fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).background(if (location != null) colors.accent.copy(alpha = 0.14f) else colors.track, CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = if (location != null) colors.accent else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    when {
                        loading -> "Obteniendo ubicación…"
                        location == null -> "Sin ubicación todavía"
                        selected == LocationCaptureMethod.MANUAL -> "Elegida en el mapa"
                        else -> "Ubicación actual del teléfono"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                location?.let { latLng ->
                    Text(
                        "${"%.6f".format(latLng.latitude)}, ${"%.6f".format(latLng.longitude)}",
                        modifier = Modifier.testTag(RegisterSubscriptionTestTags.LOCATION_COORDINATES),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } ?: Text("Usa tu ubicación en la casa del cliente o márcala en el mapa.", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onUseCurrentLocation,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(RegisterSubscriptionTestTags.LOCATION_METHOD_CURRENT),
                colors = ButtonDefaults.buttonColors(containerColor = colors.selectedContainer, contentColor = colors.onSelectedContainer),
            ) { Text("Usar mi ubicación", fontWeight = FontWeight.SemiBold, maxLines = 1) }
            OutlinedButton(
                onClick = onChooseManualLocation,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(RegisterSubscriptionTestTags.LOCATION_METHOD_MANUAL),
            ) { Text("Ajustar en el mapa", fontWeight = FontWeight.SemiBold, color = colors.hero, maxLines = 1) }
        }
        locationError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
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
