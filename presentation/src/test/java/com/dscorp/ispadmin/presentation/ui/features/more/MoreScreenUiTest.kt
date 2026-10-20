package com.dscorp.ispadmin.presentation.ui.features.more

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.dscorp.ispadmin.presentation.ui.features.profile.LogoutDialogTags
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.navigation.MoreItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val ADMIN_ITEMS = listOf(
    MoreItem.REGISTER_SUBSCRIPTION, MoreItem.CREATE_TICKET, MoreItem.INSTALLATION_ORDERS,
    MoreItem.OUTLAYS, MoreItem.REGISTER_OUTLAY, MoreItem.PLANS, MoreItem.PROFILE,
)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1400dp")
class MoreScreenUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `the header names the user and items fall in their sections with counts`() {
        val menu = MoreMenu.from(User(id = 1, name = "SERGIO CARRILLO DIESTRA", type = User.UserType.ADMIN), ADMIN_ITEMS, pending = 0, activePlans = 11)

        assertEquals(MoreHeader("SC", "Sergio C.", "Administrador"), menu.header)
        assertEquals(listOf("Gestión", "Suscripciones", "Tickets"), menu.sections.map { it.title })
        assertEquals(
            listOf(MoreEntry(MoreItem.PLANS, "11 activos"), MoreEntry(MoreItem.OUTLAYS, null), MoreEntry(MoreItem.REGISTER_OUTLAY, null), MoreEntry(MoreItem.INSTALLATION_ORDERS, null)),
            menu.sections[0].entries,
        )
        assertEquals(listOf("Suscripciones"), MoreMenu.from(User(id = 2, name = "Ana", type = User.UserType.TECHNICIAN),
            listOf(MoreItem.REGISTER_SUBSCRIPTION, MoreItem.PENDING_SUBSCRIPTIONS, MoreItem.PROFILE), pending = null, activePlans = null).sections.map { it.title })
    }

    @Test
    fun `pending subscriptions entry is shown with its count when there are pending`() {
        val menu = MoreMenu.from(User(id = 1, name = "Sergio Carrillo", type = User.UserType.ADMIN), ADMIN_ITEMS + MoreItem.PENDING_SUBSCRIPTIONS, pending = 3, activePlans = 11)

        assertEquals(MoreEntry(MoreItem.PENDING_SUBSCRIPTIONS, "3"), menu.sections[1].entries.last())
    }

    @Test
    fun `profile, items and logout respond`() {
        val menu = MoreMenu.from(User(id = 1, name = "Sergio Carrillo", type = User.UserType.ADMIN), ADMIN_ITEMS + MoreItem.PENDING_SUBSCRIPTIONS, pending = 2, activePlans = 11)
        val events = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                MoreContent(menu, versionLabel = "IspAdmin 2.6.14 (66)", onItemSelected = { events += it.name }, onProfile = { events += "profile" },
                    onLogout = { events += "logout" })
            }
        }

        compose.onNodeWithText("IspAdmin 2.6.14 (66)").assertExists()
        compose.onNodeWithText("Mi perfil").assertDoesNotExist()
        compose.onNodeWithText("Sergio C.").performClick()
        compose.onNodeWithText("Planes").performClick()
        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.onNodeWithText("¿Cerrar sesión?").assertExists()
        compose.onNodeWithText("Dejarás de recibir avisos de tickets en este teléfono hasta que vuelvas a ingresar.").assertExists()
        compose.onNodeWithTag(LogoutDialogTags.CONFIRM).performClick()

        assertEquals(listOf("profile", "PLANS", "logout"), events)
    }
}
