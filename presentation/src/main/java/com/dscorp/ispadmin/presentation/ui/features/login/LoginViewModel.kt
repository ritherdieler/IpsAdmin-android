package com.dscorp.ispadmin.presentation.ui.features.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.data.extensions.encryptWithSHA384
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Loging
import com.dscorp.ispadmin.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class LoginState {
    object Empty : LoginState()
    object Loading : LoginState()
    data class Error(val message: String) : LoginState()
    data class LoginSuccess(val data: User) : LoginState()
    data class UnverifiedAccount(val user: User) : LoginState()
}

sealed class CheckVersionState {
    object Loading : CheckVersionState()
    data class Error(val message: String) : CheckVersionState()
    data class CheckVersionSuccess(val forceUpdate: Boolean) : CheckVersionState()
}

class LoginViewModel(private val repository: IRepository) : ViewModel() {

    val loginRequestFlow = MutableStateFlow<LoginState>(LoginState.Empty)
    val checkVersionFlow = MutableStateFlow<CheckVersionState>(CheckVersionState.Loading)

    fun checkSessionStatus(): Pair<Boolean, User?> {
        val status = repository.getRememberSessionCheckBoxStatus()
        if (status) {
            repository.getUserSession()?.let {
                return Pair(true, it)
            }
            return Pair(false, null)
        }
        return Pair(false, null)
    }

    fun doLogin(loginData: LoginForm) = viewModelScope.launch {
        try {
            if (!loginData.isValid()) return@launch
            loginRequestFlow.value = LoginState.Loading
            val login = Loging(
                loginData.username,
                loginData.password.encryptWithSHA384(),
                loginData.checkedState
            )
            val response = repository.doLogin(login)
            
            if (!response.verified) {
                loginRequestFlow.value = LoginState.UnverifiedAccount(response)
            } else {
                loginRequestFlow.value = LoginState.LoginSuccess(response)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            loginRequestFlow.value = LoginState.Error(e.message ?: "Error desconocido")
        }
    }

    fun checkAppVersion() = viewModelScope.launch {
        try {
            val response = repository.getRemoteAppVersion()
            if (response.versionCode > BuildConfig.VERSION_CODE) {
                checkVersionFlow.value = CheckVersionState.CheckVersionSuccess(true)
            } else {
                checkVersionFlow.value = CheckVersionState.CheckVersionSuccess(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            checkVersionFlow.value = CheckVersionState.Error(e.message ?: "Error desconocido")
        }
    }

    fun resetLoginState() {
        loginRequestFlow.value = LoginState.Empty
    }

    // Expone errores del prompt biometrico usando el mismo dialogo de errores del login.
    fun showBiometricError(message: String) {
        loginRequestFlow.value = LoginState.Error(message)
    }

    // Inicia sesion con el usuario guardado localmente despues de validar la huella con Android.
    fun loginWithSavedSession() = viewModelScope.launch {
        val savedUser = repository.getBiometricUserSession()

        if (savedUser == null) {
            loginRequestFlow.value = LoginState.Error(
                "Primero inicia sesion con usuario y contrasena o reconocimiento facial."
            )
            return@launch
        }

        if (!savedUser.verified) {
            loginRequestFlow.value = LoginState.UnverifiedAccount(savedUser)
            return@launch
        }

        // Restaura la sesion activa despues de validar la huella.
        // Asi las pantallas internas pueden leer repository.getUserSession().
        repository.saveUserSession(savedUser, true)
        loginRequestFlow.value = LoginState.LoginSuccess(savedUser)
    }

    // Inicia sesion facial enviando la foto capturada al backend.
    fun doFaceLogin(photo: File) = viewModelScope.launch {
        try {
            loginRequestFlow.value = LoginState.Loading

            val user = repository.loginWithFace(photo)

            if (!user.verified){
                loginRequestFlow.value = LoginState.UnverifiedAccount(user)
            } else{
                loginRequestFlow.value = LoginState.LoginSuccess(user)
            }
        } catch (e: Exception){
            e.printStackTrace()
            loginRequestFlow.value = LoginState.Error(
                e.toFaceLoginMessage()
            )
        } finally {
            photo.delete()
        }
    }
}

private fun Exception.toFaceLoginMessage(): String {
    val rawMessage = message.orEmpty()

    return when {
        rawMessage.contains("Failed to connect", ignoreCase = true) ||
            rawMessage.contains("timeout", ignoreCase = true) ||
            rawMessage.contains("Unable to resolve host", ignoreCase = true) ->
            "No se pudo conectar con el servidor. Verifica tu conexion e intenta nuevamente."

        rawMessage.contains("401", ignoreCase = true) ->
            "Rostro no reconocido. Intenta nuevamente."

        rawMessage.contains("404", ignoreCase = true) ->
            "No se encontro un usuario asociado a este rostro."

        rawMessage.isNotBlank() -> rawMessage

        else -> "Error al iniciar sesion con reconocimiento facial."
    }
}
