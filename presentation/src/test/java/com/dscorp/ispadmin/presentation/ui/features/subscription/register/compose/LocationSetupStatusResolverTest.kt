package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationSetupStatusResolverTest {

    @Test
    fun `returns NeedsPermission on first visit without grant`() {
        val status = LocationSetupStatusResolver.resolve(
            hasPermission = false,
            permissionRequestedOnce = false,
            shouldShowRationale = false,
            locationSettingsSatisfied = true,
        )

        assertEquals(LocationSetupStatus.NeedsPermission, status)
    }

    @Test
    fun `returns NeedsPermissionRationale after denial with rationale`() {
        val status = LocationSetupStatusResolver.resolve(
            hasPermission = false,
            permissionRequestedOnce = true,
            shouldShowRationale = true,
            locationSettingsSatisfied = true,
        )

        assertEquals(LocationSetupStatus.NeedsPermissionRationale, status)
    }

    @Test
    fun `returns PermissionPermanentlyDenied after denial without rationale`() {
        val status = LocationSetupStatusResolver.resolve(
            hasPermission = false,
            permissionRequestedOnce = true,
            shouldShowRationale = false,
            locationSettingsSatisfied = true,
        )

        assertEquals(LocationSetupStatus.PermissionPermanentlyDenied, status)
    }

    @Test
    fun `returns NeedsLocationEnabled when permission granted but location off`() {
        val status = LocationSetupStatusResolver.resolve(
            hasPermission = true,
            permissionRequestedOnce = true,
            shouldShowRationale = false,
            locationSettingsSatisfied = false,
        )

        assertEquals(LocationSetupStatus.NeedsLocationEnabled, status)
    }

    @Test
    fun `returns Ready when permission and location are satisfied`() {
        val status = LocationSetupStatusResolver.resolve(
            hasPermission = true,
            permissionRequestedOnce = true,
            shouldShowRationale = false,
            locationSettingsSatisfied = true,
        )

        assertEquals(LocationSetupStatus.Ready, status)
    }
}
