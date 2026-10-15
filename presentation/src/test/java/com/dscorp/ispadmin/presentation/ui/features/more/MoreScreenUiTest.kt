package com.dscorp.ispadmin.presentation.ui.features.more

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.navigation.MoreItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MoreScreenUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `lists the role items and the app version`() {
        compose.setContent {
            MaterialTheme {
                MoreContent(items = listOf(MoreItem.PLANS, MoreItem.PROFILE), versionLabel = "IspAdmin 2.6.14 (66)", onItemSelected = {})
            }
        }

        compose.onNodeWithText("Planes").assertExists()
        compose.onNodeWithText("Mi perfil").assertExists()
        compose.onNodeWithText("Registrar egreso").assertDoesNotExist()
        compose.onNodeWithText("IspAdmin 2.6.14 (66)").assertExists()
    }

    @Test
    fun `tapping an item opens it`() {
        var opened: MoreItem? = null
        compose.setContent {
            MaterialTheme {
                MoreContent(items = listOf(MoreItem.PLANS, MoreItem.PROFILE), versionLabel = "", onItemSelected = { opened = it })
            }
        }

        compose.onNodeWithText("Planes").performClick()

        assertEquals(MoreItem.PLANS, opened)
    }
}
