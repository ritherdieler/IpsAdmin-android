package com.dscorp.ispadmin.presentation.ui.features.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.data.extensions.encryptWithSHA384
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Loging
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.observability.ObsBreadcrumbCategory
import com.dscorp.ispadmin.observability.ObservabilityClient
import com.dscorp.ispadmin.observability.obsTags
import com.dscorp.ispadmin.observability.runTracked
import com.dscorp.ispadmin.observability.runWorkflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class LoginState {
    object Empty : LoginState()
    object Loading : LoginState()
    data class Error(val message: String) : LoginState()
    data class LoginSuccess(val data: User) : LoginState()
    data class UnverifiedAccount(val user: User) : LoginState()
    object FaceEnrollmentOffer : LoginState()
}

sealed class CheckVersionState {
    object Loading : CheckVersionState()
    data class Error(val message: String) : CheckVersionState()
    data class CheckVersionSuccess(val forceUpdate: Boolean, val downloadUrl: String? = null) : CheckVersionState()
}

class LoginViewModel(
    private val repository: IRepository,
    private val observabilityClient: ObservabilityClient
) : ViewModel() {

    private companion object {
        const val FEATURE = "login"
        const val SCREEN = "login"
    }

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
        if (!loginData.isValid()) return@launch
        loginRequestFlow.value = LoginState.Loading
        runWorkflow(
            client = observabilityClient,
            name = "login",
            category = "auth",
            context = mapOf(
                "username" to loginData.username,
                "rememberSession" to loginData.checkedState
            )
        ) {
            runTracked(
                client = observabilityClient,
                feature = FEATURE,
                screen = SCREEN,
                action = "submit",
                extra = mapOf(
                    "username" to loginData.username,
                    "rememberSession" to loginData.checkedState
                ),
                errorMessage = "Fallo al iniciar sesión"
            ) {
                val login = Loging(
                    loginData.username,
                    loginData.password.encryptWithSHA384(),
                    loginData.checkedState
                )
                repository.doLogin(login)
            }.getOrThrow()
        }.fold(
            onSuccess = { response ->
                loginRequestFlow.value = if (!response.verified) {
                    LoginState.UnverifiedAccount(response)
                } else {
                    LoginState.LoginSuccess(response)
                }
            },
            onFailure = { e ->
                loginRequestFlow.value = LoginState.Error(e.message ?: "Error desconocido")
            }
        )
    }

    fun checkAppVersion() = viewModelScope.launch {
        runTracked(
            client = observabilityClient,
            feature = FEATURE,
            screen = SCREEN,
            action = "check_version",
            errorMessage = "Fallo al verificar la versión de la app"
        ) {
            repository.getRemoteAppVersion()
        }.fold(
            onSuccess = { response ->
                checkVersionFlow.value = CheckVersionState.CheckVersionSuccess(
                    forceUpdate = response.versionCode > BuildConfig.VERSION_CODE,
                    downloadUrl = response.downloadUrl
                )
            },
            onFailure = { e ->
                checkVersionFlow.value = CheckVersionState.Error(e.message ?: "Error desconocido")
            }
        )
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
        runTracked(
            client = observabilityClient,
            feature = FEATURE,
            screen = SCREEN,
            action = "biometric",
            errorMessage = "Fallo al restaurar la sesión guardada"
        ) {
            val savedUser = repository.getBiometricUserSession()
            when {
                savedUser == null -> loginRequestFlow.value = LoginState.Error(
                    "Primero inicia sesion con usuario y contrasena o reconocimiento facial."
                )

                !savedUser.verified ->
                    loginRequestFlow.value = LoginState.UnverifiedAccount(savedUser)

                else -> {
                    repository.saveUserSession(savedUser, true)
                    loginRequestFlow.value = LoginState.LoginSuccess(savedUser)
                }
            }
        }.onFailure { e ->
            loginRequestFlow.value = LoginState.Error(
                e.message ?: "No se pudo restaurar la sesión guardada."
            )
        }
    }

    // Inicia sesion facial enviando la foto capturada al backend.
    fun doFaceLogin(photo: File) = viewModelScope.launch {
        loginRequestFlow.value = LoginState.Loading
        observabilityClient.addBreadcrumb(
            category = ObsBreadcrumbCategory.USER_ACTION,
            message = "$FEATURE.face_login",
            data = obsTags(FEATURE, SCREEN, "face_login")
        )
        try {
            val user = repository.loginWithFace(photo)

            if (!user.verified) {
                loginRequestFlow.value = LoginState.UnverifiedAccount(user)
            } else {
                loginRequestFlow.value = LoginState.LoginSuccess(user)
            }
        } catch (e: Exception) {
            if (e.message == "No se reconocio el rostro") {
                loginRequestFlow.value = LoginState.FaceEnrollmentOffer
            } else {
                observabilityClient.reportError(
                    throwable = e,
                    message = "Fallo al iniciar sesión facial",
                    tags = obsTags(FEATURE, SCREEN, "face_login")
                )
                loginRequestFlow.value = LoginState.Error(e.toFaceLoginMessage())
            }
        } finally {
            photo.delete()
        }
    }

    // Registra el rostro del usuario que inicio sesion normalmente.
    // La sesion ya fue guardada por Repository.doLogin, por eso no necesitamos
    // volver a enviar usuario ni contrasena durante el registro facial.
    fun registerFaceForLoggedUser(photo: File) = viewModelScope.launch {
        loginRequestFlow.value = LoginState.Loading
        runTracked(
            client = observabilityClient,
            feature = FEATURE,
            screen = SCREEN,
            action = "face_enroll",
            errorMessage = "Fallo al registrar el rostro"
        ) {
            val user = repository.getUserSession()
                ?: throw Exception("No se encontro una sesion activa para registrar el rostro.")

            repository.enrollFaceFromPhoto(
                username = user.username,
                password = user.password,
                photo = photo
            )
            user
        }.fold(
            onSuccess = { user ->
                loginRequestFlow.value = LoginState.LoginSuccess(user)
            },
            onFailure = { e ->
                loginRequestFlow.value = LoginState.Error(
                    e.message ?: "No se pudo registrar el rostro."
                )
            }
        )
        photo.delete()
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
