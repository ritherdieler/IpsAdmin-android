package com.dscorp.ispadmin.presentation.ui.features.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.components.components.formfields.MyOutlinedTextField
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onRegisterSuccess : () -> Unit
) {
    val uiState by viewModel.state.collectAsState()

    when {
        uiState.registeredUser != null -> {
            AlertDialog(
                properties =  DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                onDismissRequest = { viewModel.clearRegisterForm() },
                title = { Text("Registro Exitoso") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("La cuenta se ha registrado correctamente.")

                        Text(
                            "IMPORTANTE: Su cuenta debe ser verificada por un administrador antes de poder ingresar al sistema.",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Información del usuario:")
                        Text("Nombre: ${uiState.registeredUser?.name} ${uiState.registeredUser?.lastName}")
                        Text("Usuario: ${uiState.registeredUser?.username}")
                        Text("DNI: ${uiState.registeredUser?.dni}")
                        Text("Email: ${uiState.registeredUser?.email}")
                        Text("Teléfono: ${uiState.registeredUser?.phone}")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.clearRegisterForm()
                        onRegisterSuccess()
                    }) {
                        Text("Aceptar")
                    }
                }
            )
        }

        uiState.registerError != null -> {
            AlertDialog(
                properties =  DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = {
                    Text(
                        uiState.registerError ?: "Ha ocurrido un error al registrar el usuario"
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.register)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        RegisterForm(
            state = uiState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            onFirstNameChange = { viewModel.onEvent(RegisterEvent.OnFirstNameChange(it)) },
            onLastNameChange = { viewModel.onEvent(RegisterEvent.OnLastNameChange(it)) },
            onEmailChange = { viewModel.onEvent(RegisterEvent.OnEmailChange(it)) },
            onPhoneChange = { viewModel.onEvent(RegisterEvent.OnPhoneChange(it)) },
            onDniChange = { viewModel.onEvent(RegisterEvent.OnDniChange(it)) },
            onUsernameChange = { viewModel.onEvent(RegisterEvent.OnUsernameChange(it)) },
            onPasswordChange = { viewModel.onEvent(RegisterEvent.OnPasswordChange(it)) },
            onConfirmPasswordChange = { viewModel.onEvent(RegisterEvent.OnConfirmPasswordChange(it)) },
            onRegisterClick = { viewModel.onEvent(RegisterEvent.OnRegister) },
            formIsValid = uiState.isValid()
        )
    }
}

@Composable
fun RegisterForm(
    state: RegisterState,
    modifier: Modifier = Modifier,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDniChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    formIsValid : Boolean

) {


    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nombre y Apellidos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MyOutlinedTextField(
                value = state.firstName,
                onValueChange = onFirstNameChange,
                label = stringResource(id = R.string.firstName),
                errorMessage = state.firstNameError?.let { stringResource(id = it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                maxLength = 50,
                modifier = Modifier.weight(1f)
            )

            MyOutlinedTextField(
                value = state.lastName,
                onValueChange = onLastNameChange,
                label = stringResource(id = R.string.lastName),
                errorMessage = state.lastNameError?.let { stringResource(id = it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                maxLength = 50,
                modifier = Modifier.weight(1f)
            )
        }

        // Email y Teléfono
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MyOutlinedTextField(
                value = state.dni,
                onValueChange = onDniChange,
                label = stringResource(id = R.string.dni),
                errorMessage = state.dniError?.let { stringResource(id = it) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                maxLength = 8,
                modifier = Modifier.weight(1f)
            )

            MyOutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(id = R.string.email),
                errorMessage = state.emailError?.let { stringResource(id = it) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                maxLength = 100,
                modifier = Modifier.weight(1f)
            )

        }

        MyOutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = stringResource(id = R.string.phoneNumer),
            errorMessage = state.phoneError?.let { stringResource(id = it) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            maxLength = 9,
            modifier = Modifier.fillMaxWidth()
        )
            MyOutlinedTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = stringResource(id = R.string.username),
                errorMessage = state.usernameError?.let { stringResource(id = it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                maxLength = 30,
                modifier = Modifier.fillMaxWidth()
            )


            MyOutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(id = R.string.password),
                visualTransformation = PasswordVisualTransformation(),
                errorMessage = state.passwordError?.let { stringResource(id = it) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                maxLength = 30,
                modifier = Modifier.fillMaxWidth()

            )

            MyOutlinedTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = stringResource(id = R.string.repeat_password),
                visualTransformation = PasswordVisualTransformation(),
                errorMessage = state.confirmPasswordError?.let { stringResource(id = it) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                maxLength = 30,
                modifier = Modifier.fillMaxWidth()

            )

        MyButton(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isLoading,
            text = stringResource(R.string.register),
            enabled = formIsValid
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterFormPreview() {
    MyTheme {
        RegisterForm(
            state = RegisterState(
                firstName = "Juan",
                lastName = "Pérez",
                email = "juan.perez@example.com",
                phone = "1234567890",
                dni = "12345678A",
                username = "juanperez",
                password = "password",
                confirmPassword = "password",
                registerError = " "
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            onFirstNameChange = {},
            onLastNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onDniChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onRegisterClick = {},
            formIsValid = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterFormWithErrorsPreview() {
    MyTheme {
        RegisterForm(
            state = RegisterState(
                firstName = "Juan",
                firstNameError = R.string.mustDigitFirstName,
                lastNameError = R.string.mustDigitLastName,
                email = "invalid-email",
                emailError = R.string.invalidEmail,
                phone = "123",
                phoneError = R.string.invalidPhone,
                dni = "12345",
                dniError = R.string.invalidDNI,
                username = "j",
                usernameError = R.string.mustDigiUsername,
                password = "pass",
                passwordError = R.string.mustDigitPassword,
                confirmPassword = "password",
                confirmPasswordError = R.string.passwordsMustBeEquals,
                registerError = " "
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            onFirstNameChange = {},
            onLastNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onDniChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onRegisterClick = {},
            formIsValid = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterFormLoadingPreview() {
    MyTheme {
        RegisterForm(
            state = RegisterState(
                firstName = "Juan",
                lastName = "Pérez",
                email = "juan.perez@example.com",
                phone = "1234567890",
                dni = "12345678A",
                username = "juanperez",
                password = "password",
                confirmPassword = "password",
                isLoading = true,
                registerError = " "
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            onFirstNameChange = {},
            onLastNameChange = {},
            onEmailChange = {},
            onPhoneChange = {},
            onDniChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onRegisterClick = {},
            formIsValid = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    MyTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Registro") },
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            RegisterForm(
                state = RegisterState(
                    firstName = "Juan",
                    lastName = "Pérez",
                    email = "juan.perez@example.com",
                    phone = "1234567890",
                    dni = "12345678A",
                    username = "juanperez",
                    password = "password",
                    confirmPassword = "password",
                    registerError = " "
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                onFirstNameChange = {},
                onLastNameChange = {},
                onEmailChange = {},
                onPhoneChange = {},
                onDniChange = {},
                onUsernameChange = {},
                onPasswordChange = {},
                onConfirmPasswordChange = {},
                onRegisterClick = {},
                formIsValid = true
            )
        }
    }
}
