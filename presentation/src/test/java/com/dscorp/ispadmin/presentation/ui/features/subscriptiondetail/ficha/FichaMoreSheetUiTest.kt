package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1600dp")
class FichaMoreSheetUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `sheet groups the actions under the holder and reports the choice`() {
        var chosen: MoreAction? = null
        compose.setContent {
            MaterialTheme {
                FichaMoreActionsContent(
                    name = "Jorge Ramos Villanueva",
                    subtitle = "Cód. 1842 · Basico · La Villa",
                    sections = listOf(
                        MoreSection("Servicio", listOf(MoreAction.CHANGE_PLAN)),
                        MoreSection("Cobranza", listOf(MoreAction.DEFER_CUT)),
                        MoreSection(null, listOf(MoreAction.CANCEL)),
                    ),
                    onAction = { chosen = it },
                )
            }
        }

        compose.onNodeWithText("JR").assertExists()
        compose.onNodeWithText("Cód. 1842 · Basico · La Villa").assertExists()
        compose.onNodeWithText("Servicio").assertExists()
        compose.onNodeWithText("Cobranza").assertExists()
        compose.onNodeWithText("Postergar corte").performClick()
        assertEquals(MoreAction.DEFER_CUT, chosen)
        compose.onNodeWithText("Cancelar suscripción").performClick()
        assertEquals(MoreAction.CANCEL, chosen)
    }
}
