package com.dscorp.ispadmin.presentation.ui.features.installationorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.Loader
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyCustomDialog
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutLinedDropDown
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutlinedTextField
import com.dscorp.ispadmin.presentation.utils.FormValidations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInstallationOrderScreen(
    uiState: InstallationOrderUiState,
    onCreateOrderClicked: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDniChange: (String) -> Unit,
    onPlaceChange: (Place) -> Unit,
    onErrorDismissed: () -> Unit,
    onSuccessDismissed: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismissed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            if (uiState.isLoading) {
                Loader()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "Información del Cliente",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    MyOutlinedTextField(
                        value = uiState.form.firstName,
                        onValueChange = onFirstNameChange,
                        label = "Nombre",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        maxLength = FormValidations.Limits.NAME_MAX_LENGTH,
                        regex = FormValidations.NAME_REGEX,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MyOutlinedTextField(
                        value = uiState.form.lastName,
                        onValueChange = onLastNameChange,
                        label = "Apellido",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        maxLength = FormValidations.Limits.NAME_MAX_LENGTH,
                        regex = FormValidations.NAME_REGEX,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MyOutlinedTextField(
                        value = uiState.form.dni,
                        onValueChange = onDniChange,
                        label = "DNI",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        maxLength = FormValidations.Limits.DNI_MAX_LENGTH,
                        regex = FormValidations.DNI_REGEX,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MyOutlinedTextField(
                        value = uiState.form.address,
                        onValueChange = onAddressChange,
                        label = "Dirección",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        maxLength = FormValidations.Limits.ADDRESS_MAX_LENGTH,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MyOutlinedTextField(
                        value = uiState.form.phone,
                        onValueChange = onPhoneChange,
                        label = "Teléfono",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        regex = FormValidations.PHONE_REGEX,
                        maxLength = FormValidations.Limits.PHONE_MAX_LENGTH,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MyOutLinedDropDown(
                        items = uiState.places,
                        selected = uiState.form.place,
                        label = "Lugar",
                        onItemSelected = onPlaceChange,
                        hasError = uiState.form.place == null && uiState.form.firstName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    MyButton(
                        text = "Crear Orden de Instalación",
                        enabled = uiState.isFormValid,
                        onClick = onCreateOrderClicked,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (uiState.orderCreated != null) {
                MyCustomDialog(
                    onDismissRequest = {
                        onSuccessDismissed()
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "¡Éxito!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Orden de instalación creada correctamente.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        MyButton(
                            text = "Aceptar",
                            onClick = {
                                onSuccessDismissed()
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}