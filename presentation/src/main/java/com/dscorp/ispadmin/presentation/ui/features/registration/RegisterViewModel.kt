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
    val user: User? = null
)

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
                _state.update { currentState ->
                    currentState.copy(
                        firstName = event.value,
                        firstNameError = if (!event.value.isAValidName()) R.string.invalidName else null
                    )
                }
            }
            is RegisterEvent.OnLastNameChange -> {
                _state.update { currentState ->
                    currentState.copy(
                        lastName = event.value,
                        lastNameError = if (!event.value.isAValidName()) R.string.invalidLastName else null
                    )
                }
            }
            is RegisterEvent.OnEmailChange -> {
                _state.update { currentState ->
                    currentState.copy(
                        email = event.value,
                        emailError = if (!event.value.isValidEmail()) R.string.invalidEmail else null
                    )
                }
            }
            is RegisterEvent.OnPhoneChange -> {
                _state.update { currentState ->
                    currentState.copy(
                        phone = event.value,
                        phoneError = if (!event.value.isValidPhone()) R.string.invalidPhone else null
                    )
                }
            }
            is RegisterEvent.OnDniChange -> {
                _state.update { currentState ->
                    currentState.copy(
                        dni = event.value,
                        dniError = if (!event.value.isValidDni()) R.string.invalidDNI else null
                    )
                }
            }
            is RegisterEvent.OnUsernameChange -> {
                _state.update { currentState ->
                    currentState.copy(
                        username = event.value,
                        usernameError = if (event.value.isBlank()) R.string.mustDigitUserName else null
                    )
                }
            }
            is RegisterEvent.OnPasswordChange -> {
                _state.update { currentState ->
                    currentState.copy(
                        password = event.value,
                        passwordError = if (event.value.isBlank()) R.string.mustDigitPassword else null
                    )
                }
            }
            is RegisterEvent.OnConfirmPasswordChange -> {
                _state.update { currentState ->
                    currentState.copy(
                        confirmPassword = event.value,
                        confirmPasswordError = if (event.value != state.value.password) R.string.passwordsMustBeEquals else null
                    )
                }
            }
            RegisterEvent.OnRegister -> {
                if (isFormValid()) {
                    registerUser()
                }
            }
        }
    }

    private fun isFormValid(): Boolean {
        val currentState = state.value
        return currentState.firstNameError == null &&
                currentState.lastNameError == null &&
                currentState.emailError == null &&
                currentState.phoneError == null &&
                currentState.dniError == null &&
                currentState.usernameError == null &&
                currentState.passwordError == null &&
                currentState.confirmPasswordError == null
    }

    private fun registerUser() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val user = createUser()
                val registeredUser = repository.registerUser(user)
                _state.update { it.copy(user = registeredUser, isLoading = false) }
                firebaseAnalytics.sendSignUpEvent(AnalyticsConstants.REGISTER_USER)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
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
}
