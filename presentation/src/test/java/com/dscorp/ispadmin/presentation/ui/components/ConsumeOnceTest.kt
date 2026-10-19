package com.dscorp.ispadmin.presentation.ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ConsumeOnceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `a message cleared as soon as it is taken is still shown`() {
        var consumed = 0
        compose.setContent {
            MaterialTheme {
                val host = remember { SnackbarHostState() }
                var message by remember { mutableStateOf<String?>("Plan cambiado a Duo basico 80") }
                // Clearing the state right away changes the key, which used to cancel the snackbar.
                ConsumeOnce(message, onConsumed = { consumed++; message = null }) { host.showSnackbar(it) }
                SnackbarHost(host)
            }
        }

        compose.onNodeWithText("Plan cambiado a Duo basico 80").assertExists()
        assertEquals(1, consumed)
    }
}
