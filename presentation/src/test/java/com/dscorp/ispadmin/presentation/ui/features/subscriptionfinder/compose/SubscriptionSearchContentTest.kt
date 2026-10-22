package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.material3.Text
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1600dp")
class SubscriptionSearchContentTest {
    @get:Rule val compose = createComposeRule()

    private var quickAccess: ServiceStatus? = null
    private var openedRecent: Int? = null
    private var cleared = false
    private var opened: SubscriptionResume? = null
    private var chip: ResultFilter? = null
    private var filtersOpened = false
    private var retried = 0

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
                onOpenFilters = { filtersOpened = true },
                onLoadMore = {},
                onRetry = { retried++ },
                rowMenu = { subscription, expanded, _ -> if (expanded) Text("menu-${subscription.id}") },
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

        // The design keeps one fixed hint under the box.
        compose.onNodeWithText("Reconoce nombre, DNI, IP o código").assertExists()
        compose.onNodeWithText("Buscando por DNI o RUC").assertDoesNotExist()
        compose.onNodeWithText("Sin resultados para «45781236»").assertExists()
    }

    @Test
    fun `the search has its own header with the filters button`() {
        show(SubscriptionFinderUiState())

        compose.onNodeWithText("Suscripciones").assertExists()
        compose.onNodeWithTag(SubscriptionFinderTestTags.FILTERS).performClick()

        assertEquals(true, filtersOpened)
    }

    @Test
    fun `row actions open with a long press, without a visible menu button`() {
        show(SubscriptionFinderUiState(
            subscriptions = mapOf(ServiceStatus.ACTIVE to listOf(row(id = 1))),
            smartQuery = "ram",
            lastUsedFilter = SubscriptionFilter.BY_NAME("RAM", ""),
            searchPerformed = true,
        ))

        compose.onNodeWithTag(SubscriptionFinderTestTags.resultMenu(1)).assertDoesNotExist()
        compose.onNodeWithText("menu-1").assertDoesNotExist()
        compose.onNodeWithTag(SubscriptionFinderTestTags.resultItem(1)).performTouchInput { longClick() }

        compose.onNodeWithText("menu-1").assertExists()
        assertEquals(null, opened)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE) // real text metrics, so the drawn chip height is meaningful
    fun `result filter chips offer a 48dp touch target`() {
        show(SubscriptionFinderUiState(
            subscriptions = mapOf(ServiceStatus.CUT_OFF to listOf(row(id = 1))),
            smartQuery = "ram", searchPerformed = true,
        ))

        ResultFilter.entries.take(VISIBLE_CHIPS).forEach { filter ->
            val node = compose.onNodeWithTag(SubscriptionFinderTestTags.resultFilter(filter)).fetchSemanticsNode()
            with(node.layoutInfo.density) {
                val touchHeight = node.touchBoundsInRoot.height.toDp()
                assertTrue("$filter touch height $touchHeight < 48dp", touchHeight >= 48.dp)
                assertEquals("$filter chip is still drawn 32dp tall", 32f, node.boundsInRoot.height.toDp().value, 0.5f)
            }
        }
    }

    @Test
    fun `a failed search says so and retries`() {
        show(SubscriptionFinderUiState(smartQuery = "ram", searchPerformed = true, searchError = "timeout"))

        compose.onNodeWithText("No se pudo buscar").assertExists()
        compose.onNodeWithText("Revisa tu conexión y vuelve a intentar.").assertExists()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun `a failed refresh keeps the results with a banner`() {
        show(SubscriptionFinderUiState(
            subscriptions = mapOf(ServiceStatus.CUT_OFF to listOf(row(id = 1))),
            smartQuery = "ram", searchPerformed = true, searchError = "timeout",
        ))

        compose.onNodeWithText("Jorge Ramos Villanueva").assertExists()
        compose.onNodeWithText("No se pudo actualizar").assertExists()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retried)
    }

    private companion object {
        /** Chips composed by the lazy row on a 411 dp wide screen; the rest are off screen. */
        const val VISIBLE_CHIPS = 3
    }
}
