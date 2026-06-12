package com.dscorp.ispadmin.presentation.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.Date

/**
 * Abre la camara para tomar una fotografia.
 *
 * Si Android ya concedio el permiso, abre la camara inmediatamente.
 * Si falta el permiso, lo solicita antes de continuar.
 * Si el usuario lo rechaza, muestra un mensaje claro.
 */
@Composable
fun rememberPhotoTaker(
    context: Context = LocalContext.current,
    onPhotoTaken: (Uri) -> Unit
): Pair<() -> Unit, MutableState<Uri?>> {
    val photoUri = remember { mutableStateOf<Uri?>(null) }

    // Abre la aplicacion de camara y recibe el resultado.
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri.value?.let(onPhotoTaken)
        }
    }

    // Crea un archivo temporal seguro para guardar la foto tomada.
    val takePhoto = {
        val photoFile = File(
            context.cacheDir,
            "${Date().time}.jpg"
        )
        val localPhotoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )

        photoUri.value = localPhotoUri
        takePictureLauncher.launch(localPhotoUri)
    }

    // Solicita el permiso solamente cuando Android aun no lo concedio.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePhoto()
        } else {
            Toast.makeText(
                context,
                "Debes permitir el uso de la camara para tomar una foto.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Abre la camara inmediatamente si ya existe permiso.
    // En caso contrario, solicita autorizacion al usuario.
    val requestCameraAndTakePhoto = {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            takePhoto()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    return Pair(requestCameraAndTakePhoto, photoUri)
}
