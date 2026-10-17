package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.CustomerData
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** In proposal A the subscription hub replaces the card menu as the place to act: a tap opens it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SubscriptionCardOpenDetailsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `tapping the card opens the subscription hub`() {
        var selected: SubscriptionMenu? = null
        compose.setContent {
            MaterialTheme {
                SubscriptionCard(
                    subscriptionResume = SubscriptionResume(
                        id = 1842, planName = "basico", installationType = InstallationType.FIBER,
                        serviceStatus = ServiceStatus.CUT_OFF, antiquity = "19", placeName = "la villa",
                        ipAddress = "192.168.221.35", ics = "0", lastPaymentDate = null, totalDebt = 100.0,
                        pendingInvoicesQuantity = 2, customerName = "JORGE RAMOS VILLANUEVA", qualification = "",
                        customer = CustomerData(1842, "JORGE", "RAMOS VILLANUEVA", "70418293", "la villa", "Jr. Los Pinos 214", "987654321", ""),
                        napBox = null, placeId = "1", location = GeoLocation(),
                    ),
                    onMenuItemSelected = { selected = it },
                    onExpandChange = { _, _ -> },
                    expanded = false,
                )
            }
        }

        compose.onNodeWithText("basico", substring = true, ignoreCase = true).performClick()

        assertEquals(SubscriptionMenu.SEE_DETAILS, selected)
    }
}
