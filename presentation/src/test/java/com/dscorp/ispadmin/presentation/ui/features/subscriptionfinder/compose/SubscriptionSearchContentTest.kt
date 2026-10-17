package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1600dp")
class SubscriptionSearchContentTest {
    @get:Rule val compose = createComposeRule()

    private var quickAccess: ServiceStatus? = null
    private var openedRecent: Int? = null
    private var cleared = false
    private var opened: SubscriptionResume? = null
    private var chip: ResultFilter? = null

    private fun show(state: SubscriptionFinderUiState) = compose.setContent {
        MaterialTheme {
            SubscriptionSearchContent(
                state = state,
                onQueryChange = {},
                onFilterSelected = { chip = it },
                onQuickAccess = { quickAccess = it },
                onOpenSubscription = { opened = it },
                onOpenRecent = { openedRecent = it },
                onClearRecents = { cleared = true },
                onSearchByDate = {},
                onLoadMore = {},
            )
        }
    }

    @Test
    fun `start screen offers quick accesses with real totals and recents`() {
        show(
            SubscriptionFinderUiState(
                quickAccess = listOf(QuickAccess(ServiceStatus.CUT_OFF, "Cortados", 490), QuickAccess(ServiceStatus.CANCELLED, "Cancelados", 449)),
                recents = listOf(RecentSubscription(1842, "Jorge Ramos Villanueva", "Basico · La Villa", ServiceStatus.CUT_OFF)),
            )
        )

        compose.onNodeWithText("Reconoce nombre, DNI, IP o código").assertExists()
        compose.onNodeWithText("490").assertExists()
        compose.onNodeWithTag(SubscriptionFinderTestTags.quickAccess(ServiceStatus.CUT_OFF)).performClick()
        assertEquals(ServiceStatus.CUT_OFF, quickAccess)

        compose.onNodeWithText("Jorge Ramos Villanueva").performClick()
        assertEquals(1842, openedRecent)
        compose.onNodeWithTag(SubscriptionFinderTestTags.CLEAR_RECENTS).performClick()
        assertEquals(true, cleared)
    }

    @Test
    fun `results are compact rows with state and debt and open the subscription`() {
        val owing = row(id = 1)
        val upToDate = row(id = 2, name = "ANA RAMOS PAREDES", plan = "Duo basico 80", place = "9 de octubre",
            status = ServiceStatus.ACTIVE, type = InstallationType.FIBER, debt = 0.0)
        show(SubscriptionFinderUiState(
            subscriptions = mapOf(ServiceStatus.ACTIVE to listOf(owing, upToDate)),
            smartQuery = "ram",
            lastUsedFilter = SubscriptionFilter.BY_NAME("RAM", ""),
            searchPerformed = true,
        ))

        compose.onNodeWithText("2 resultados").assertExists()
        compose.onNodeWithText("Jorge Ramos Villanueva").assertExists()
        compose.onNodeWithText("Basico · La Villa").assertExists()
        compose.onNodeWithText("S/ 100.00").assertExists()
        compose.onNodeWithText("Al día").assertExists()
        compose.onNodeWithText("Cortado").assertExists()

        compose.onNodeWithTag(SubscriptionFinderTestTags.resultFilter(ResultFilter.WITH_DEBT)).performClick()
        assertEquals(ResultFilter.WITH_DEBT, chip)

        compose.onNodeWithTag(SubscriptionFinderTestTags.resultItem(1)).performClick()
        assertEquals(1, opened?.id)
    }

    @Test
    fun `the box says what kind of search it understood`() {
        show(SubscriptionFinderUiState(smartQuery = "45781236", lastUsedFilter = SubscriptionFilter.BY_DOCUMENT("45781236"), searchPerformed = true))

        compose.onNodeWithText("Buscando por DNI o RUC").assertExists()
        compose.onNodeWithText("Sin resultados para «45781236»").assertExists()
    }
}
