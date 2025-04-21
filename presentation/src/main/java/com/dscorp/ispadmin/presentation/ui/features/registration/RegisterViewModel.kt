package com.dscorp.ispadmin.presentation.ui.features.registration

import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.extensions.isValidDni
import com.dscorp.ispadmin.domain.model.extensions.isValidEmail
import com.dscorp.ispadmin.domain.model.extensions.isValidPhone
import com.dscorp.ispadmin.presentation.extension.analytics.AnalyticsConstants
import com.dscorp.ispadmin.presentation.extension.analytics.sendSignUpEvent
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.formvalidation.ReactiveFormField
import com.example.data2.data.extensions.encryptWithSHA384
import com.example.data2.data.repository.IRepository
import com.google.firebase.analytics.FirebaseAnalytics

class RegisterViewModel(
    private val repository: IRepository,
    private val firebaseAnalytics: FirebaseAnalytics
) : BaseViewModel<RegisterUiState>() {

    val firstNameField = ReactiveFormField<String>(
        hintResourceId = R.string.firstName,
        errorResourceId = R.string.mustDigitFirstName
    ) { !it.isNullOrEmpty() }

    val lastNameField = ReactiveFormField<String>(
        hintResourceId = R.string.lastName,
        errorResourceId = R.string.mustDigitLastName
    ) { !it.isNullOrEmpty() }

    val emailField = ReactiveFormField<String>(
        hintResourceId = R.string.email,
        errorResourceId = R.string.mustDigitEmail
    ) { it.isValidEmail() }

    val phoneField = ReactiveFormField<String>(
        hintResourceId = R.string.phoneNumer,
        errorResourceId = R.string.mustDigitPhoneNumber
    ) { it.isValidPhone() }

    val dniField = ReactiveFormField<String>(
        hintResourceId = R.string.dni,
        errorResourceId = R.string.must_digit_dni
    ) { it.isValidDni() }

    val userNameField = ReactiveFormField<String>(
        hintResourceId = R.string.username,
        errorResourceId = R.string.mustDigitUserName
    ) { !it.isNullOrEmpty() }

    val password1Field = ReactiveFormField<String>(
        hintResourceId = R.string.password,
        errorResourceId = R.string.mustDigitPassword
    ) { !it.isNullOrEmpty() }

    val password2Field = ReactiveFormField<String>(
        hintResourceId = R.string.repeat_password,
        errorResourceId = R.string.passwordsMustBeEquals
    ) {
        !it.isNullOrEmpty() && !password1Field.getValue()
            .isNullOrEmpty() && it == password1Field.getValue()!!
    }

    fun registerUser() = executeWithProgress {
        if (!formIsValid()) return@executeWithProgress
        val registerFormRepository = repository.registerUser(createUser())
        uiState.value = BaseUiState(RegisterUiState.OnRegister(registerFormRepository))
        firebaseAnalytics.sendSignUpEvent(AnalyticsConstants.REGISTER_USER)
    }

    fun formIsValid() = listOf(
        firstNameField,
        lastNameField,
        dniField,
        phoneField,
        emailField,
        userNameField,
        password1Field,
        password2Field
    ).all { it.isValid() }

    private fun createUser() = User(
        name = firstNameField.getValue()!!,
        lastName = lastNameField.getValue()!!,
        type = User.UserType.CLIENT,
        username = userNameField.getValue()!!,
        password = password1Field.getValue()!!.encryptWithSHA384(),
        verified = false,
        dni = dniField.getValue()!!,
        email = emailField.getValue()!!,
        phone = phoneField.getValue()!!
    )

}
