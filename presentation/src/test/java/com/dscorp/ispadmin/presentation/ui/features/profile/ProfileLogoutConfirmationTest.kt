package com.dscorp.ispadmin.presentation.ui.features.profile

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1600dp")
class ProfileLogoutConfirmationTest {
    @get:Rule val compose = createComposeRule()

    private var logouts = 0

    private fun show() = compose.setContent {
        MaterialTheme {
            ProfileContent(
                uiState = MyProfileUiState(user = User(id = 1, name = "Rosa", type = User.UserType.SECRETARY)),
                onLogout = { logouts++ },
                onClearError = {},
            )
        }
    }

    @Test
    fun `logging out asks for confirmation first`() {
        show()

        compose.onNodeWithText("Cerrar sesión").performClick()

        assertEquals(0, logouts)
        compose.onNodeWithText("¿Cerrar sesión?").assertExists()
    }

    @Test
    fun `confirming logs out once`() {
        show()

        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.onAllNodesWithText("Cerrar sesión")[1].performClick()

        assertEquals(1, logouts)
    }

    @Test
    fun `going back keeps the session`() {
        show()

        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.onNodeWithText("Volver").performClick()

        assertEquals(0, logouts)
        compose.onNodeWithText("¿Cerrar sesión?").assertDoesNotExist()
    }
}
