package com.dscorp.ispadmin.presentation.extension

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Environment
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.navigation.NavController
import com.dscorp.ispadmin.CrossDialogFragment
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.formvalidation.ReactiveFormField
import com.dscorp.ispadmin.presentation.util.IDialogFactory
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

fun Uri.getBase64FromUri(installationSheetUri: Uri, context: Context): String? {
    val inputStream = context.contentResolver.openInputStream(installationSheetUri)
    val bytes = inputStream?.readBytes()
    val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
    return base64
}

fun NavController.navigateSafe(destinationId: Int) {
    try {
        navigate(destinationId)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun LatLng.toGeoLocation(): GeoLocation = GeoLocation(latitude, longitude)
fun LatLng.toStringLocation(): String = "$latitude, $longitude"

fun MaterialAutoCompleteTextView.fillWithList(data: List<Any>, onItemSelected: (Any) -> Unit) {
    val adapter = ArrayAdapter(context, R.layout.simple_spinner_item, data)
    setAdapter(adapter)
    onItemClickListener =
        AdapterView.OnItemClickListener { _, _, pos, _ ->
            onItemSelected(data[pos])
        }
}

fun Fragment.showSuccessDialog(
    text: String, onPositiveCallback: (() -> Unit)? = null
) {
    childFragmentManager.executePendingTransactions()
    val dialogFactory: IDialogFactory by inject()
    val successDialog =
        dialogFactory.createSuccessDialog(requireContext(), text, onPositiveCallback)
    successDialog.setCancelable(false)
    successDialog.show()
}

fun Fragment.showSuccessDialog(
    text: Int, onPositiveCallback: (() -> Unit)? = null
) {
    childFragmentManager.executePendingTransactions()
    val dialogFactory: IDialogFactory by inject()
    val successDialog =
        dialogFactory.createSuccessDialog(requireContext(), getString(text), onPositiveCallback)
    successDialog.setCancelable(false)
    successDialog.show()
}

fun Fragment.showErrorDialog(error: String? = "Error desconocido") {
    val dialogFactory: IDialogFactory by inject()
    val errorDialog = dialogFactory.createErrorDialog(requireContext(), error ?: "")
    errorDialog.show()
}

fun Activity.showSuccessDialog(text: String) {

    val dialogFactory: IDialogFactory by inject()
    val successDialog = dialogFactory.createSuccessDialog(this, text)
    successDialog.show()
}

fun Activity.showErrorDialog(error: String = "error desconocido") {
    val dialogFactory: IDialogFactory by inject()
    val errorDialog = dialogFactory.createErrorDialog(this, error)
    errorDialog.show()
}


// Kotlin

fun Activity.getDownloadedFileUri(document: DownloadDocumentResponse): Uri {
    val data = Base64.decode(document.base64, Base64.DEFAULT)
    val directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    val outputFile = File(directory, document.getNameWithExtension())
    val outputStream = FileOutputStream(outputFile)
    outputStream.write(data)
    outputStream.close()

    return FileProvider.getUriForFile(this, "com.dscorp.ispadmin.fileprovider", outputFile)
}


fun <T> MaterialAutoCompleteTextView.populate(data: List<T>, onItemSelected: (T) -> Unit) {
    val adapter = ArrayAdapter(context, R.layout.simple_spinner_item, data)
    setAdapter(adapter)
    onItemClickListener =
        AdapterView.OnItemClickListener { _, _, pos, _ ->
            onItemSelected(data[pos])
        }
}


fun Fragment.showCrossDialog(
    text: Any? = null,
    lottieRes: Int? = null,
    cancelable: Boolean = false,
    closeButtonClickListener: (() -> Unit)? = null,
    positiveButtonClickListener: (() -> Unit)? = null
) {
    CrossDialogFragment(
        text = when (text) {
            is String -> text
            is Int -> getString(text)
            else -> ""
        },
        lottieResource = lottieRes,
        onCloseButtonClick = closeButtonClickListener ?: positiveButtonClickListener,
        onPositiveButtonClick = positiveButtonClickListener
    ).apply { isCancelable = cancelable }.show(
        childFragmentManager,
        CrossDialogFragment::class.simpleName
    )
}

fun AppCompatActivity.showCrossDialog(
    text: Any? = null,
    lottieRes: Int? = null,
    cancelable: Boolean = false,
    closeButtonClickListener: (() -> Unit)? = null,
    positiveButtonClickListener: (() -> Unit)? = null
) {
    CrossDialogFragment(
        text = when (text) {
            is String -> text
            is Int -> getString(text)
            else -> ""
        },
        lottieResource = lottieRes,
        onCloseButtonClick = closeButtonClickListener ?: positiveButtonClickListener,
        onPositiveButtonClick = positiveButtonClickListener
    ).apply { isCancelable = cancelable }.show(
        supportFragmentManager,
        CrossDialogFragment::class.simpleName
    )
}


@SuppressLint("MissingPermission")
fun FusedLocationProviderClient.getCurrentLocation(onLocation: (LatLng) -> Unit) {
    this.requestLocationUpdates(
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .apply {
                setWaitForAccurateLocation(true)
                setMinUpdateIntervalMillis(LocationRequest.Builder.IMPLICIT_MIN_UPDATE_INTERVAL)
                setMaxUpdateDelayMillis(1000)
            }.build(),

        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                location?.let {
                    onLocation(LatLng(it.latitude, location.longitude))
                    removeLocationUpdates(this)
                }
            }
        },
        Looper.getMainLooper()
    )
}

fun Fragment.withGpsEnabled(onGpsEnabled: () -> Unit) {
    if ((requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(
            LocationManager.GPS_PROVIDER
        )
    ) {
        onGpsEnabled.invoke()
    } else {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            })
        val client: SettingsClient =
            LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> =
            client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // GPS is enabled, so you can perform your location-related tasks here
            onGpsEnabled.invoke()
        }.addOnFailureListener { exception ->
            when ((exception as ResolvableApiException).statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    exception.startResolutionForResult(requireActivity(), 1)
                }
            }
        }
    }
}

fun ImageView.animateRotate360InLoop() {
    startAnimation(
        RotateAnimation(
            0f,
            360f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        ).apply {
            duration = 1000
            repeatCount = Animation.INFINITE
        }
    )
}

fun Calendar.isSameMonthAndYear(calendar: Calendar) =
    get(Calendar.YEAR) == calendar.get(Calendar.YEAR) && get(Calendar.MONTH) == calendar.get(
        Calendar.MONTH
    )

fun Calendar.isLastDayOfMonth() =
    get(Calendar.DAY_OF_MONTH) == getActualMaximum(Calendar.DAY_OF_MONTH)

fun Long.asCalendar() = Calendar.getInstance().apply { timeInMillis = this@asCalendar }

fun MutableLiveData<Boolean>.visibilityFromBoolean(): LiveData<Int> =
    map { if (it) View.VISIBLE else View.GONE }

fun Boolean.visibilityFromBoolean(): Int {
    return if (this) View.VISIBLE else View.GONE
}

fun List<ReactiveFormField<*>>.formIsValid(): Boolean {
    forEach { it.isValid() }
    return this.all { it.isValid() }
}


//fun Fragment.openPermissionSettings(isRational: Boolean = false ) {
//    if (!isRational)
//        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//    else {
//        val intent = Intent()
//        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//        val uri = Uri.fromParts("package", requireActivity().packageName, null)
//        intent.data = uri
//        startActivity(intent)
//    }
//}

fun Fragment.openLocationSetting() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

}