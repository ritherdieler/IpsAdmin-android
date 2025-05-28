package com.dscorp.ispadmin.presentation.ui.features.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.ispadmin.presentation.ui.components.MyOutlinedTextField
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

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
            state = state,
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
            onRegisterClick = { viewModel.onEvent(RegisterEvent.OnRegister) }
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
    onRegisterClick: () -> Unit
) {
    // Patrones de validación
    val emailRegex = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()
    val phoneRegex = "^[0-9]{9,10}$".toRegex()
    val dniRegex = "^[0-9]{8}[A-Za-z]$".toRegex()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MyOutlinedTextField(
            value = state.firstName,
            onValueChange = onFirstNameChange,
            label = stringResource(id = R.string.firstName),
            errorMessage = state.firstNameError?.let { stringResource(id = it) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true,
            maxLength = 50
        )

        MyOutlinedTextField(
            value = state.lastName,
            onValueChange = onLastNameChange,
            label = stringResource(id = R.string.lastName),
            errorMessage = state.lastNameError?.let { stringResource(id = it) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true,
            maxLength = 50
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
            regex = emailRegex,
            singleLine = true,
            maxLength = 100
        )

        MyOutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = stringResource(id = R.string.phoneNumer),
            errorMessage = state.phoneError?.let { stringResource(id = it) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            regex = phoneRegex,
            singleLine = true,
            maxLength = 10
        )

        MyOutlinedTextField(
            value = state.dni,
            onValueChange = onDniChange,
            label = stringResource(id = R.string.dni),
            errorMessage = state.dniError?.let { stringResource(id = it) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            regex = dniRegex,
            singleLine = true,
            maxLength = 9
        )

        MyOutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = stringResource(id = R.string.username),
            errorMessage = state.usernameError?.let { stringResource(id = it) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true,
            maxLength = 30
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
            maxLength = 30
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
            maxLength = 30
        )

        MyButton (
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            isLoading = state.isLoading,
            text = stringResource(R.string.register)
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
                confirmPassword = "password"
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
            onRegisterClick = {}
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
                lastName = "",
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
                confirmPasswordError = R.string.passwordsMustBeEquals
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
            onRegisterClick = {}
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
                isLoading = true
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
            onRegisterClick = {}
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
                    confirmPassword = "password"
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
                onRegisterClick = {}
            )
        }
    }
}
