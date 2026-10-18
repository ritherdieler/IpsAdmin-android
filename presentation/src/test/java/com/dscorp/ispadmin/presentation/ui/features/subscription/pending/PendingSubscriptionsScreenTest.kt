package com.dscorp.ispadmin.presentation.ui.features.subscription.pending

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import android.app.Application
import com.dscorp.ispadmin.domain.model.PendingSubscriptionStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PendingSubscriptionsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `sync button exposes testTag btn_sync_pending_subscriptions`() {
        composeRule.setContent {
            MaterialTheme {
                PendingSubscriptionsScreen(
                    uiState = PendingSubscriptionsUiState(),
                    onIntent = {}
                )
            }
        }

        composeRule.onNodeWithTag("btn_sync_pending_subscriptions").assertIsDisplayed()
        composeRule.onNodeWithText("Enviar ahora").assertIsDisplayed()
    }

    @Test
    fun `renders client dni date and status for queued items`() {
        composeRule.setContent {
            MaterialTheme {
                PendingSubscriptionsScreen(
                    uiState = PendingSubscriptionsUiState(
                        items = listOf(
                            PendingSubscriptionListItem(
                                localId = "local-1",
                                clientName = "Ana Perez",
                                dni = "12345678",
                                createdAt = 1_700_000_000_000L,
                                status = PendingSubscriptionStatus.PENDING
                            )
                        )
                    ),
                    onIntent = {}
                )
            }
        }

        composeRule.onNodeWithText("Ana Perez").assertIsDisplayed()
        composeRule.onNodeWithText("DNI 12345678", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Esperando señal").assertIsDisplayed()
        composeRule.onNodeWithText("Por enviar · 1").assertIsDisplayed()
    }

    @Test
    fun `shows success snackbar when ShowSuccessSnackbar event is collected`() {
        val events = kotlinx.coroutines.flow.MutableSharedFlow<PendingSubscriptionsUiEvent>(
            extraBufferCapacity = 1
        )
        composeRule.setContent {
            MaterialTheme {
                PendingSubscriptionsScreen(
                    uiState = PendingSubscriptionsUiState(),
                    onIntent = {},
                    uiEvents = events
                )
            }
        }

        composeRule.runOnIdle {
            events.tryEmit(
                PendingSubscriptionsUiEvent.ShowSuccessSnackbar(
                    "Sincronización completada exitosamente (1 suscripciones enviadas)"
                )
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(
                "Sincronización completada exitosamente (1 suscripciones enviadas)"
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(
            "Sincronización completada exitosamente (1 suscripciones enviadas)"
        ).assertIsDisplayed()
    }
}
