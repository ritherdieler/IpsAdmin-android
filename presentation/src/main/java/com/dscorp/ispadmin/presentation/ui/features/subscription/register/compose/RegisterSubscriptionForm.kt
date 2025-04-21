package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyAutoCompleteTextViewCompose
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyIconButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutLinedDropDown
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutlinedTextField
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse

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
    onRegisterClick: () -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(scrollState)
        ) {
            SectionTitle("Datos del Cliente")

            TwoFieldsRow(
                label1 = "Nombres",
                value1 = formState.registerSubscriptionForm.firstName,
                onValueChange1 = onFirstNameChanged,
                hasError1 = formState.registerSubscriptionForm.firstNameError != null,
                label2 = "Apellidos",
                value2 = formState.registerSubscriptionForm.lastName,
                onValueChange2 = onLastNameChanged,
                hasError2 = formState.registerSubscriptionForm.lastNameError != null
            )

            TwoFieldsRow(
                label1 = "DNI",
                value1 = formState.registerSubscriptionForm.dni,
                onValueChange1 = onDniChanged,
                hasError1 = formState.registerSubscriptionForm.dniError != null,
                keyboardType1 = KeyboardType.Number,
                label2 = "Teléfono",
                value2 = formState.registerSubscriptionForm.phone,
                onValueChange2 = onPhoneChanged,
                hasError2 = formState.registerSubscriptionForm.phoneError != null,
                keyboardType2 = KeyboardType.Phone
            )

            SectionTitle("Dirección")
            MyAutoCompleteTextViewCompose(
                modifier = Modifier.fillMaxWidth(),
                items = formState.registerSubscriptionForm.placeList,
                label = "Lugar",
                selectedItem = formState.registerSubscriptionForm.selectedPlace,
                onItemSelected = onPlaceSelected,
                onSelectionCleared = onPLaceSelectionCleared,
                hasError = formState.registerSubscriptionForm.placeError != null,
            )
            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = formState.registerSubscriptionForm.address,
                label = "Dirección",
                onValueChange = onAddressChanged,
                hasError = formState.registerSubscriptionForm.addressError != null,
                singleLine = false,
                maxLines = 4
            )

            SectionTitle("Instalación")
            InstallationTypeSelector(
                installationType = formState.registerSubscriptionForm.installationType,
                onTypeSelected = onInstallationTypeSelected
            )

            MyOutLinedDropDown(
                label = "Plan",
                items = formState.registerSubscriptionForm.planList,
                selected = formState.registerSubscriptionForm.selectedPlan,
                onItemSelected = onPlanSelected,
                hasError = formState.registerSubscriptionForm.planError != null,
                enabled = formState.registerSubscriptionForm.planList.isNotEmpty(),
            )

            if (formState.registerSubscriptionForm.installationType == InstallationType.FIBER) {
                FiberOpticForm(
                    formState = formState,
                    onOnuSelected = onOnuSelected,
                    onNapBoxSelected = onNapBoxSelected,
                    onNapBoxSelectionCleared = onNapBoxSelectionCleared,
                    onRefreshOnuList = onRefreshOnuList
                )
            }

            SectionTitle("Observaciones")
            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = formState.registerSubscriptionForm.note,
                onValueChange = onNoteChanged,
                label = "Nota",
                singleLine = false,
                maxLines = 4,
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
                enabled = formState.registerSubscriptionForm.isValid(),
                isLoading = formState.isLoading
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun TwoFieldsRow(
    label1: String,
    value1: String,
    onValueChange1: (String) -> Unit,
    hasError1: Boolean = false,
    keyboardType1: KeyboardType = KeyboardType.Text,
    label2: String,
    value2: String,
    onValueChange2: (String) -> Unit,
    hasError2: Boolean = false,
    keyboardType2: KeyboardType = KeyboardType.Text
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MyOutlinedTextField(
            modifier = Modifier.weight(1f),
            label = label1,
            value = value1,
            onValueChange = onValueChange1,
            hasError = hasError1,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType1)
        )
        MyOutlinedTextField(
            modifier = Modifier.weight(1f),
            label = label2,
            value = value2,
            onValueChange = onValueChange2,
            hasError = hasError2,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType2)
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
    onRefreshOnuList: () -> Unit
) {
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

    MyAutoCompleteTextViewCompose(
        items = formState.registerSubscriptionForm.napBoxList,
        label = NAP_BOX_LABEL,
        selectedItem = formState.registerSubscriptionForm.selectedNapBox,
        onItemSelected = onNapBoxSelected,
        onSelectionCleared = onNapBoxSelectionCleared,
        enabled = formState.registerSubscriptionForm.selectedPlace != null,
        hasError = formState.registerSubscriptionForm.placeError != null
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

@Composable
fun FormRow(
    modifier: Modifier = Modifier,
    horizontalGap: Dp = 8.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

// Constantes para etiquetas

private const val ONU_LABEL = "Onu"
private const val NAP_BOX_LABEL = "Caja Nap"
const val FIBER_OPTIC = "Fibra óptica"
const val WIRELESS = "Inalámbrico"
const val ONLY_TV = "Solo TV"

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL)
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
