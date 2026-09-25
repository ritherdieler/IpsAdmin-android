package com.dscorp.ispadmin.presentation.navigation

import com.dscorp.ispadmin.navigation.NavRoutes
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvisioningNavigationTest {
    @Test fun `provisioning destination is not classified as home`() {
        val route = NavRoutes.FeatureRoutes.Subscription.Provisioning::class.qualifiedName
        assertTrue(NavRoutes.FeatureRoutes.FromString("$route/{subscriptionId}") is
            NavRoutes.FeatureRoutes.Subscription.Provisioning)
    }
}
