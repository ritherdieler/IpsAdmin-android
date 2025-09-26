package com.dscorp.ispadmin.presentation.ui.components

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.util.Date

@Composable
fun rememberPhotoTaker(
    context: Context = LocalContext.current,
    onPhotoTaken: (Uri) -> Unit
): Pair<() -> Unit, MutableState<Uri?>> {
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri.value?.let {
                onPhotoTaken(it)
            }
        }
    }

    val takePhoto = {
        val photoFile = File(context.cacheDir, "${Date().time}.jpg")
        val localPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
        photoUri.value = localPhotoUri
        takePictureLauncher.launch(localPhotoUri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePhoto()
        } else {
            // Handle permission denied
        }
    }

    return Pair({ cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }, photoUri)
}