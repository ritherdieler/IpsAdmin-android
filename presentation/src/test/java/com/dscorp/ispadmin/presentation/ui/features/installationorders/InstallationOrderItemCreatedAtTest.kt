package com.dscorp.ispadmin.presentation.ui.features.installationorders

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.dscorp.ispadmin.domain.model.InstallationOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class InstallationOrderItemCreatedAtTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `hides the created row when the order has no creation date`() {
        setItem(InstallationOrder(id = 7, customerFirstName = "Ana", createdAt = null))

        composeRule.onNodeWithText("Creado", substring = true).assertDoesNotExist()
    }

    @Test
    fun `shows the formatted creation date`() {
        setItem(InstallationOrder(id = 7, customerFirstName = "Ana", createdAt = LocalDateTime.of(2026, 10, 14, 9, 30)))

        composeRule.onNodeWithText("Creado: 14/10/2026 09:30").assertExists()
    }

    private fun setItem(order: InstallationOrder) {
        composeRule.setContent {
            MaterialTheme {
                InstallationOrderItem(
                    order = order,
                    uiState = InstallationOrderListUiState(),
                    onAsignTechinician = {},
                    onTransferOrderClicked = {},
                    onRegisterSubscription = {},
                )
            }
        }
    }
}
