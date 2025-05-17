package com.dscorp.ispadmin.presentation.util

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class PermissionManager(
    private val fragment: Fragment,
    private val onDeny: () -> Unit = {},
    private val onRationale: () -> Unit = {}
) {

    private var onGranted: () -> Unit = {}
    private var singlePermissionLauncher: ActivityResultLauncher<String>? = null
    private var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var permissions = ""

    init {
        init()
    }

    private fun init() {
        singlePermissionLauncher =
            fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                when {
                    granted -> onGranted.invoke()
                    fragment.shouldShowRequestPermissionRationale(permissions) -> onRationale.invoke()
                    else -> onDeny.invoke()
                }
            }
        multiplePermissionLauncher =
            fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
                val allPermissionsGranted = map.entries.all { it.value }

                if (allPermissionsGranted)
                    onGranted.invoke()
                else
                    onDeny.invoke()
            }
    }

    fun requestPermission(permission: String, onGranted: () -> Unit) {
        this.onGranted = onGranted
        this.permissions = permission
        singlePermissionLauncher?.launch(permission)
    }

}