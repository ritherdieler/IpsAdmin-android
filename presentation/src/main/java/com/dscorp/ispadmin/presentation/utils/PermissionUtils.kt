package com.dscorp.ispadmin.presentation.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Clase de utilidad para manejar los permisos de ubicación
 */
object PermissionUtils {
    private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

    /**
     * Verifica si se tienen los permisos de ubicación
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Solicita los permisos de ubicación
     */
    fun requestLocationPermission(
        activity: FragmentActivity,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (hasLocationPermission(activity)) {
            onPermissionGranted()
            return
        }

        // Definir el callback para cuando se obtenga la respuesta de los permisos
        val permissionCallback = object : ActivityCompat.OnRequestPermissionsResultCallback {
            override fun onRequestPermissionsResult(
                requestCode: Int,
                permissions: Array<out String>,
                grantResults: IntArray
            ) {
                if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        onPermissionGranted()
                    } else {
                        onPermissionDenied()
                    }
                    // Eliminar el callback después de su uso
                    activity.removeOnRequestPermissionsResultCallback(this)
                }
            }
        }

        // Registrar el callback
        activity.addOnRequestPermissionsResultCallback(permissionCallback)

        // Solicitar los permisos
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Extensión para agregar un callback de permisos a una actividad
     */
    private fun FragmentActivity.addOnRequestPermissionsResultCallback(callback: ActivityCompat.OnRequestPermissionsResultCallback) {
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment is ActivityCompat.OnRequestPermissionsResultCallback) {
                fragment.onRequestPermissionsResult(
                    LOCATION_PERMISSION_REQUEST_CODE,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    intArrayOf(PackageManager.PERMISSION_GRANTED)
                )
            }
        }
    }

    /**
     * Extensión para eliminar un callback de permisos de una actividad
     */
    private fun FragmentActivity.removeOnRequestPermissionsResultCallback(callback: ActivityCompat.OnRequestPermissionsResultCallback) {
        // La implementación simplificada, ya que no hay un mecanismo directo para esto
        // En una implementación real, se usaría un patrón observer más robusto
    }

    fun requestLocationPermissionFromContext(
        context: Context,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (hasLocationPermission(context)) {
            onPermissionGranted()
            return
        }
        // Si no tiene permisos, simplemente llama a onPermissionDenied (en Compose deberías usar Accompanist Permissions o similar)
        onPermissionDenied()
    }
} 