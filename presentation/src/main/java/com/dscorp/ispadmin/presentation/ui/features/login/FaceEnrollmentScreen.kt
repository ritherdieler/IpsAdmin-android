package com.dscorp.ispadmin.presentation.ui.features.login


import androidx.compose.runtime.Composable
import java.io.File
// Pantalla dedicada al registro facial de un usuario autenticado.
// Reutiliza la camara nativa existente para evitar duplicar la logica de CameraX.
@Composable
fun FaceEnrollmentScreen(
    onFacePhotoCaptured: (File) -> Unit,
    onCancel: () -> Unit,
    retryTrigger: Int = 0
) {
    FaceLoginScreen(
        // Entrega la foto capturada al ViewModel para registrarla en face_data.
        onFacePhotoCaptured = onFacePhotoCaptured,

        // Permite regresar si el usuario decide no registrar su rostro ahora.
        onCancel = onCancel,

        // Reinicia la captura cuando el backend rechaza la foto o sucede un error.
        retryTrigger = retryTrigger
    )
}