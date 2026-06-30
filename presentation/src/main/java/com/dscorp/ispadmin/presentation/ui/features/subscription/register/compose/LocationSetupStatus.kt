package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

enum class LocationSetupStatus {
    NeedsPermission,
    NeedsPermissionRationale,
    PermissionPermanentlyDenied,
    NeedsLocationEnabled,
    Ready,
}

object LocationSetupStatusResolver {
    fun resolve(
        hasPermission: Boolean,
        permissionRequestedOnce: Boolean,
        shouldShowRationale: Boolean,
        locationSettingsSatisfied: Boolean,
    ): LocationSetupStatus {
        if (!hasPermission) {
            if (!permissionRequestedOnce) return LocationSetupStatus.NeedsPermission
            if (shouldShowRationale) return LocationSetupStatus.NeedsPermissionRationale
            return LocationSetupStatus.PermissionPermanentlyDenied
        }
        if (!locationSettingsSatisfied) return LocationSetupStatus.NeedsLocationEnabled
        return LocationSetupStatus.Ready
    }
}
