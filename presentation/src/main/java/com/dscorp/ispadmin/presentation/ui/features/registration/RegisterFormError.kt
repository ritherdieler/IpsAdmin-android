package com.dscorp.ispadmin.presentation.ui.features.registration

/**
 * Created by Sergio Carrillo Diestra on 16/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
sealed class RegisterFormError(val error: String) {
    companion object {
        const val USER_ERROR = "Este campo no puede estar vacio"
        const val FIRST_NAME_ERROR = "Este campo no puede estar vacio"
        const val LAST_NAME_ERROR = "Este campo no puede estar vacio"
        const val PASSWORD_ERROR = "Este campo no puede estar vacio"
        const val VERIFY_PASSWORD_ERROR = "Este campo no puede estar vacio"
        const val FIRST_NAME_INVALID = "Este nombre es invalido"
        const val LAST_NAME_INVALID = "Este nombre es invalido"
        const val DIFFERENT_PASSWORDS = "Las contraseñas no coinciden"
    }

    class OnEtLastNameError : RegisterFormError(LAST_NAME_ERROR)
    class OnEtPassword1Error : RegisterFormError(PASSWORD_ERROR)
}
