package com.dscorp.ispadmin.presentation.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthManager {

    // Define que solo se acepta biometria fuerte registrada en Android.
    // La app no recibe la huella; Android solo devuelve exito o error.
    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

    // Revisa si Android permite usar una huella/biometria fuerte en este dispositivo.
    fun getAvailabilityMessage(context: Context): String? {
        return when (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "Este dispositivo no tiene sensor biometrico disponible."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "El sensor biometrico no esta disponible en este momento."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "Registra una huella en los ajustes del telefono para usar esta opcion."
            else -> "No se puede usar huella digital en este dispositivo."
        }
    }

    // Indica si el equipo soporta biometria pero aun no tiene huella registrada.
    // Se usa para mostrar el dialogo que lleva a los ajustes del telefono.
    fun needsBiometricEnrollment(context: Context): Boolean {
        return BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    // Abre los ajustes de Android para que el usuario registre su huella en el sistema.
    fun openBiometricEnrollment(activity: FragmentActivity) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, AUTHENTICATORS)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

        activity.startActivity(intent)
    }

    // Muestra el prompt nativo de Android; la app nunca lee ni guarda la huella.
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // El executor asegura que el resultado del prompt vuelva al hilo principal de Android.
        val executor = ContextCompat.getMainExecutor(activity)

        // Callback que recibe el resultado del sistema operativo.
        // Si la huella es correcta, solo se ejecuta onSuccess; no se expone ningun dato biometrico.
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Huella no reconocida. Intenta nuevamente.")
                }
            }
        )

        // Configura el texto visible del prompt nativo que muestra Android.
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Iniciar sesion con huella digital")
            .setSubtitle("Usa tu huella para ingresar a la app")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }

}
