package com.dscorp.ispadmin.presentation.ui.features.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.data.extensions.encryptWithSHA384
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.extensions.isAValidName
import com.dscorp.ispadmin.domain.model.extensions.isValidDni
import com.dscorp.ispadmin.domain.model.extensions.isValidEmail
import com.dscorp.ispadmin.domain.model.extensions.isValidPhone
import com.dscorp.ispadmin.presentation.extension.analytics.AnalyticsConstants
import com.dscorp.ispadmin.presentation.extension.analytics.sendSignUpEvent
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterState(
    val firstName: String = "",
    val firstNameError: Int? = null,
    val lastName: String = "",
    val lastNameError: Int? = null,
    val email: String = "",
    val emailError: Int? = null,
    val phone: String = "",
    val phoneError: Int? = null,
    val dni: String = "",
    val dniError: Int? = null,
    val username: String = "",
    val usernameError: Int? = null,
    val password: String = "",
    val passwordError: Int? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: Int? = null,
    val isLoading: Boolean = false,
    val registeredUser: User? = null,
    val registerError: String? = null,
){
    fun isValid() = firstNameError == null &&
    lastNameError == null &&
    emailError == null &&
    phoneError == null &&
    dniError == null &&
    usernameError == null &&
    passwordError == null &&
    confirmPasswordError == null &&
    firstName.isAValidName() &&
    lastName.isAValidName() &&
    email.isValidEmail() &&
    phone.isValidPhone() &&
    dni.isValidDni() &&
    username.isNotBlank() &&
    password.isNotBlank() &&
    confirmPassword.isNotBlank() &&
    password == confirmPassword
}

sealed class RegisterEvent {
    data class OnFirstNameChange(val value: String) : RegisterEvent()
    data class OnLastNameChange(val value: String) : RegisterEvent()
    data class OnEmailChange(val value: String) : RegisterEvent()
    data class OnPhoneChange(val value: String) : RegisterEvent()
    data class OnDniChange(val value: String) : RegisterEvent()
    data class OnUsernameChange(val value: String) : RegisterEvent()
    data class OnPasswordChange(val value: String) : RegisterEvent()
    data class OnConfirmPasswordChange(val value: String) : RegisterEvent()
    object OnRegister : RegisterEvent()
}

class RegisterViewModel(
    private val repository: IRepository,
    private val firebaseAnalytics: FirebaseAnalytics
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.OnFirstNameChange -> {
                updateField(
                    value = event.value,
                    isValid = { it.isAValidName() },
                    errorMessage = R.string.invalidName,
                    updateField = { currentState, value, error ->
                        currentState.copy(
                            firstName = value,
                            firstNameError = error,
                        )
                    }
                )
            }

            is RegisterEvent.OnLastNameChange -> {
                updateField(
                    value = event.value,
                    isValid = { it.isAValidName() },
                    errorMessage = R.string.invalidLastName,
                    updateField = { currentState, value, error ->
                        currentState.copy(
                            lastName = value,
                            lastNameError = error,
                        )
                    }
                )
            }

            is RegisterEvent.OnEmailChange -> {
                updateField(
                    value = event.value,
                    isValid = { it.isValidEmail() },
                    errorMessage = R.string.invalidEmail,
                    updateField = { currentState, value, error ->
                        currentState.copy(
                            email = value,
                            emailError = error,
                        )
                    }
                )
            }

            is RegisterEvent.OnPhoneChange -> {
                updateField(
                    value = event.value,
                    isValid = { it.isValidPhone() },
                    errorMessage = R.string.invalidPhone,
                    updateField = { currentState, value, error ->
                        currentState.copy(
                            phone = value,
                            phoneError = error,
                        )
                    }
                )
            }

            is RegisterEvent.OnDniChange -> {
                updateField(
                    value = event.value,
                    isValid = { it.isValidDni() },
                    errorMessage = R.string.invalidDNI,
                    updateField = { currentState, value, error ->
                        currentState.copy(
                            dni = value,
                            dniError = error,
                        )
                    }
                )
            }

            is RegisterEvent.OnUsernameChange -> {
                updateField(
                    value = event.value,
                    isValid = { it.isNotBlank() },
                    errorMessage = R.string.mustDigitUserName,
                    updateField = { currentState, value, error ->
                        currentState.copy(
                            username = value,
                            usernameError = error,
                        )
                    }
                )
            }

            is RegisterEvent.OnPasswordChange -> {
                updateField(
                    value = event.value,
                    isValid = { it.isNotBlank() },
                    errorMessage = R.string.mustDigitPassword,
                    updateField = { currentState, value, error ->
                        currentState.copy(
                            password = value,
                            passwordError = error,
                        )
                    }
                )
            }

            is RegisterEvent.OnConfirmPasswordChange -> {
                updateField(
                    value = event.value,
                    isValid = { it == state.value.password },
                    errorMessage = R.string.passwordsMustBeEquals,
                    updateField = { currentState, value, error ->
                        currentState.copy(
                            confirmPassword = value,
                            confirmPasswordError = error,
                        )
                    }
                )
            }

            RegisterEvent.OnRegister -> {
                if (_state.value.isValid()) {
                    registerUser()
                }
            }
        }
    }

    private fun <T> updateField(
        value: T,
        isValid: (T) -> Boolean,
        errorMessage: Int,
        updateField: (RegisterState, T, Int?) -> RegisterState
    ) {
        _state.update { currentState ->
            val error = if (!isValid(value)) errorMessage else null
            updateField(currentState, value, error)
        }
    }

    private fun registerUser() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val user = createUser()
                val registeredUser = repository.registerUser(user)
                _state.update { it.copy(registeredUser = registeredUser, isLoading = false, registerError = null) }
                firebaseAnalytics.sendSignUpEvent(AnalyticsConstants.REGISTER_USER)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, registerError = "Ocurrió un error al registrar el usuario") }
            }
        }
    }

    private fun createUser() = User(
        name = state.value.firstName,
        lastName = state.value.lastName,
        type = User.UserType.CLIENT,
        username = state.value.username,
        password = state.value.password.encryptWithSHA384(),
        verified = false,
        dni = state.value.dni,
        email = state.value.email,
        phone = state.value.phone
    )

    fun clearError() {

        _state.update { it.copy(registerError = null)
    }}

    fun clearRegisterForm() {

        _state.update {
            RegisterState(
                firstName = "",
                lastName = "",
                email = "",
                phone = "",
                dni = "",
                username = "",
                password = "",
                confirmPassword = "",
                registeredUser = null,
                registerError = null
            )
        }
    }
}
