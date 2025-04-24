package com.dscorp.ispadmin.presentation.fcm

import android.content.Context
import android.util.Log
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.UpdateDeviceTokenUseCase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

private const val TAG = "FcmUtils"

/**
 * Obtiene el token FCM actual y lo actualiza en el servidor
 * 
 * @param context Contexto de la aplicación
 * @param user Usuario actual
 * @param updateDeviceTokenUseCase Caso de uso para actualizar el token
 * @param onTokenUpdated Callback invocado al finalizar la actualización
 */
suspend fun updateFcmToken(
    context: Context,
    user: User,
    updateDeviceTokenUseCase: UpdateDeviceTokenUseCase,
    onTokenUpdated: (success: Boolean, message: String?) -> Unit = { _, _ -> }
) {
    try {
        val token = getFcmToken()
        Log.d(TAG, "Token FCM obtenido: $token")
        
        user.id?.let { userId ->
            updateDeviceTokenUseCase(userId, token).fold(
                onSuccess = { 
                    Log.d(TAG, "Token FCM actualizado correctamente en el servidor")
                    onTokenUpdated(true, null)
                },
                onFailure = { error ->
                    Log.e(TAG, "Error al actualizar token FCM: ${error.message}", error)
                    onTokenUpdated(false, error.message)
                }
            )
        } ?: run {
            Log.e(TAG, "No se pudo actualizar el token FCM: ID de usuario nulo")
            onTokenUpdated(false, "ID de usuario nulo")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error al obtener token FCM: ${e.message}", e)
        onTokenUpdated(false, e.message)
    }
}

/**
 * Obtiene el token actual de FCM
 * 
 * @return Token FCM como string
 */
private suspend fun getFcmToken(): String {
    return try {
        FirebaseMessaging.getInstance().token.await()
    } catch (e: Exception) {
        Log.e(TAG, "Error al obtener token FCM: ${e.message}", e)
        throw e
    }
} 