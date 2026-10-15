package com.dscorp.ispadmin.presentation.ui.features.splash.compose

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.presentation.ui.features.login.CheckVersionState
import com.dscorp.ispadmin.presentation.ui.features.login.LoginViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SplashForceUpdateTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val openedUris = mutableListOf<String>()
    private var finishCount = 0

    @Test
    fun `accept opens the download url and closes the app`() {
        setSplash(CheckVersionState.CheckVersionSuccess(forceUpdate = true, downloadUrl = DOWNLOAD_URL))

        composeRule.onNodeWithText("Aceptar").performClick()

        assertEquals(listOf(DOWNLOAD_URL), openedUris)
        assertEquals(1, finishCount)
    }

    @Test
    fun `accept without download url only closes the app`() {
        setSplash(CheckVersionState.CheckVersionSuccess(forceUpdate = true, downloadUrl = " "))

        composeRule.onNodeWithText("Aceptar").performClick()

        assertEquals(emptyList<String>(), openedUris)
        assertEquals(1, finishCount)
    }

    @Test
    fun `closing the dialog closes the app instead of leaving the user stuck`() {
        setSplash(CheckVersionState.CheckVersionSuccess(forceUpdate = true, downloadUrl = DOWNLOAD_URL))

        composeRule.onNodeWithContentDescription("Cerrar diálogo").performClick()

        assertEquals(emptyList<String>(), openedUris)
        assertEquals(1, finishCount)
    }

    private fun setSplash(state: CheckVersionState) {
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        every { viewModel.checkVersionFlow } returns MutableStateFlow(state)
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) {
                openedUris += uri
            }
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                MaterialTheme {
                    SplashScreen(
                        onNavigateToMain = {},
                        onNavigateToLogin = {},
                        onFinishApp = { finishCount++ },
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    private companion object {
        const val DOWNLOAD_URL = "https://appdistribution.firebase.dev/i/example"
    }
}
