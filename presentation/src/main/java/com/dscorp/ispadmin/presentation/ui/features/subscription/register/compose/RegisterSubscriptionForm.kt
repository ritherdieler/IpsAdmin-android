package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import androidx.compose.runtime.saveable.rememberSaveable
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
            if (missingFields == 1) "Completa 1 dato pendiente para registrar." else "Completa los $missingFields datos pendientes para registrar."
        else -> if (missingFields == 1) "Completa 1 campo obligatorio para continuar." else "Completa los $missingFields campos obligatorios para continuar."
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
        var napPickerOpen by rememberSaveable { mutableStateOf(false) }
        Box(Modifier.fillMaxSize()) {
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
                            // Placeholder until the service type selector is built.
                            RegisterSubscriptionWizardStep.SERVICE_TYPE -> Unit
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

                            RegisterSubscriptionWizardStep.INSTALLATION -> ServiceStepContent(
                                formState = formState,
                                onIntent = onIntent,
                                onOpenNapPicker = { napPickerOpen = true },
                                onFacadePhotoClick = onFacadePhotoClick,
                                hostDeviceField = {
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
                                },
                                legacyFiberFields = { LegacyFiberOnuFields(formState, onIntent) },
                            )

                            RegisterSubscriptionWizardStep.CONFIRMATION -> ReviewStepContent(formState, onIntent)
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
            if (napPickerOpen) {
                NapPickerSheet(
                    form = form,
                    onSelect = { onIntent(RegisterSubscriptionIntent.NapBoxSelected(it)); napPickerOpen = false },
                    onDismiss = { napPickerOpen = false },
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
fun InstallationTypeSelector(
    installationType: InstallationType,
    onTypeSelected: (InstallationType) -> Unit,
    enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RegisterFieldLabel("Tipo de instalación")
        Segmented(
            options = listOf(InstallationType.FIBER, InstallationType.WIRELESS, InstallationType.ONLY_TV_FIBER),
            selected = installationType,
            label = {
                when (it) {
                    InstallationType.FIBER -> "Fibra"
                    InstallationType.WIRELESS -> "Inalámbrico"
                    InstallationType.ONLY_TV_FIBER -> "Solo TV"
                }
            },
            containerTag = RegisterSubscriptionTestTags.INSTALLATION_TYPE,
            itemTag = { index, _ -> RegisterSubscriptionTestTags.installationTypeItem(index) },
            enabled = enabled,
            onSelect = onTypeSelected,
        )
    }
}

@Composable
fun WanAccessModeSelector(
    selected: AccessMode,
    onSelected: (AccessMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RegisterFieldLabel("Acceso a internet")
        Segmented(
            options = AccessMode.registerChoices(),
            selected = AccessMode.registerChoices().firstOrNull { it == selected || (it == AccessMode.PPPOE_DYNAMIC && selected.usesPppoe()) },
            label = { it.registerLabel() },
            containerTag = RegisterSubscriptionTestTags.ACCESS_MODE,
            itemTag = { _, mode ->
                if (mode == AccessMode.STATIC_IP) RegisterSubscriptionTestTags.ACCESS_MODE_STATIC_IP else RegisterSubscriptionTestTags.ACCESS_MODE_PPPOE
            },
            enabled = enabled,
            onSelect = onSelected,
        )
    }
}

@Composable
fun EquipmentConditionSelector(
    equipmentCondition: EquipmentCondition,
    onConditionSelected: (EquipmentCondition) -> Unit,
    enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RegisterFieldLabel("Equipo")
        Segmented(
            options = listOf(EquipmentCondition.LOAN, EquipmentCondition.SOLD),
            selected = equipmentCondition,
            label = { if (it == EquipmentCondition.LOAN) "En préstamo" else "Vendido" },
            enabled = enabled,
            onSelect = onConditionSelected,
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

/** ONU and VLAN chosen inside the form, only in registrations without ONU preauthorization. */
@Composable
private fun LegacyFiberOnuFields(formState: RegisterSubscriptionState, onIntent: (RegisterSubscriptionIntent) -> Unit) {
    val form = formState.registerSubscriptionForm
    val showOnuSelector = form.installationType == InstallationType.FIBER && !formState.preauthorizationEnabled
    if (showOnuSelector) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RegisterFieldLabel("ONU y VLAN")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            MyOutLinedDropDown(
                modifier = Modifier.weight(1f).testTag(RegisterSubscriptionTestTags.ONU),
                items = form.onuList,
                selected = form.selectedOnu,
                label = ONU_LABEL,
                onItemSelected = { onIntent(RegisterSubscriptionIntent.OnuSelected(it)) },
                hasError = form.onuError != null,
                itemTestTag = { index, _ -> RegisterSubscriptionTestTags.onuItem(index) },
            )
            RefreshIcon(onRefreshOnuList = { onIntent(RegisterSubscriptionIntent.RefreshOnuList) }, formState = formState)
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
