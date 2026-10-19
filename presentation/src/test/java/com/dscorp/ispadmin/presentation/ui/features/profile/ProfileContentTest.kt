package com.dscorp.ispadmin.presentation.ui.features.profile

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.User
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1600dp")
class ProfileContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `the profile shows who is signed in, their data and the app version`() {
        compose.setContent {
            MaterialTheme {
                ProfileContent(
                    uiState = MyProfileUiState(
                        user = User(id = 1, name = "Sergio", lastName = "Carrillo", type = User.UserType.ADMIN, username = "sergio.c",
                            dni = "70123456", email = "", phone = "999111222"),
                    ),
                    onLogout = {}, onClearError = {}, versionLabel = "2.6.14 (66)",
                )
            }
        }

        compose.onNodeWithText("Sergio C.").assertExists()
        compose.onNodeWithText("Administrador").assertExists()
        compose.onNodeWithText("sergio.c").assertExists()
        compose.onNodeWithText("70123456").assertExists()
        compose.onNodeWithText("Sin email").assertExists()
        compose.onNodeWithText("999 111 222").assertExists()
        compose.onNodeWithText("2.6.14 (66)").assertExists()
    }

    @Test
    fun `the theme is chosen from the profile`() {
        var chosen: com.dscorp.ispadmin.presentation.theme.ThemeMode? = null
        compose.setContent {
            MaterialTheme {
                ProfileContent(
                    uiState = MyProfileUiState(user = User(id = 1, name = "Rosa", type = User.UserType.SECRETARY)),
                    onLogout = {}, onClearError = {}, themeMode = com.dscorp.ispadmin.presentation.theme.ThemeMode.SYSTEM,
                    onThemeSelected = { chosen = it },
                )
            }
        }

        compose.onNodeWithText("Según el sistema").performClick()
        compose.onNodeWithText("Oscuro").performClick()
        org.junit.Assert.assertEquals(com.dscorp.ispadmin.presentation.theme.ThemeMode.DARK, chosen)
    }
}
