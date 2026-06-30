package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

data class LocationSetupState(
    val status: LocationSetupStatus,
    val isReady: Boolean,
    val onContinue: () -> Unit,
    val openAppSettings: () -> Unit,
    val openLocationSettings: () -> Unit,
    val fetchCurrentLocation: (onSuccess: (Double, Double) -> Unit) -> Unit,
)

@Composable
fun rememberLocationSetupState(): LocationSetupState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var hasPermission by remember {
        mutableStateOf(hasLocationPermission(context))
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }
    var shouldShowRationale by remember {
        mutableStateOf(shouldShowLocationRationale(activity))
    }
    var locationSettingsSatisfied by remember {
        mutableStateOf(isLocationUsableFallback(context))
    }

    fun recomputeStatus(): LocationSetupStatus {
        return LocationSetupStatusResolver.resolve(
            hasPermission = hasPermission,
            permissionRequestedOnce = permissionRequestedOnce,
            shouldShowRationale = shouldShowRationale,
            locationSettingsSatisfied = locationSettingsSatisfied,
        )
    }

    var status by remember { mutableStateOf(recomputeStatus()) }

    fun updateStatus() {
        status = recomputeStatus()
    }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        locationSettingsSatisfied = result.resultCode == Activity.RESULT_OK ||
            isLocationUsableFallback(context)
        updateStatus()
    }

    fun checkLocationSettings() {
        val hostActivity = activity
        if (hostActivity == null) {
            locationSettingsSatisfied = isLocationUsableFallback(context)
            updateStatus()
            return
        }
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
                    .setMinUpdateIntervalMillis(5_000L)
                    .build()
            )
            .build()

        LocationServices.getSettingsClient(hostActivity)
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                locationSettingsSatisfied = true
                updateStatus()
            }
            .addOnFailureListener { exception ->
                locationSettingsSatisfied = false
                updateStatus()
                if (exception is ResolvableApiException) {
                    locationSettingsLauncher.launch(
                        IntentSenderRequest.Builder(exception.resolution).build()
                    )
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequestedOnce = true
        hasPermission = granted
        shouldShowRationale = shouldShowLocationRationale(activity)
        updateStatus()
        if (granted) {
            checkLocationSettings()
        }
    }

    val openAppSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    val openLocationSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    val onContinue: () -> Unit = {
        when (status) {
            LocationSetupStatus.NeedsPermission,
            LocationSetupStatus.NeedsPermissionRationale -> {
                permissionRequestedOnce = true
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            LocationSetupStatus.PermissionPermanentlyDenied -> openAppSettings()

            LocationSetupStatus.NeedsLocationEnabled -> checkLocationSettings()

            LocationSetupStatus.Ready -> Unit
        }
    }

    val fetchCurrentLocation: (onSuccess: (Double, Double) -> Unit) -> Unit = { onSuccess ->
        if (hasLocationPermission(context)) {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()
            fusedLocationClient.getCurrentLocation(request, null)
                .addOnSuccessListener { location ->
                    location?.let { onSuccess(it.latitude, it.longitude) }
                }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasPermission = hasLocationPermission(context)
            shouldShowRationale = shouldShowLocationRationale(activity)
            if (hasPermission) {
                checkLocationSettings()
            } else {
                locationSettingsSatisfied = isLocationUsableFallback(context)
                updateStatus()
            }
        }
    }

    return LocationSetupState(
        status = status,
        isReady = status == LocationSetupStatus.Ready,
        onContinue = onContinue,
        openAppSettings = openAppSettings,
        openLocationSettings = openLocationSettings,
        fetchCurrentLocation = fetchCurrentLocation,
    )
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun shouldShowLocationRationale(activity: Activity?): Boolean {
    return activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(
            it,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } ?: false
}

private fun isLocationUsableFallback(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
