package com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.extensions.PayerFinderResult
import com.dscorp.ispadmin.observability.ObservabilityClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CashScreenTest {
    @get:Rule val compose = createComposeRule()

    private val result = PayerFinderResult(1842, "JORGE RAMOS VILLANUEVA", "Jorge Ramos V.", "Yape", "12/07/2026", 50.0)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `before searching it explains how to search instead of saying nothing was found`() {
        assertEquals(CashPhase.IDLE, PayerFinderState(searchQuery = "").phase)
        assertEquals(CashPhase.IDLE, PayerFinderState(searchQuery = "ra").phase)
        assertEquals(CashPhase.EMPTY, PayerFinderState(searchQuery = "ramos").phase)
        assertEquals(CashPhase.RESULTS, PayerFinderState(searchQuery = "ramos", electronicPayers = listOf(result)).phase)

        compose.setContent { MaterialTheme { CashContent(PayerFinderState(), {}, {}, {}) } }

        compose.onNodeWithText("Escribe al menos 3 letras del nombre que aparece en Yape o Plin").assertExists()
        compose.onNodeWithText("No se encontraron resultados").assertDoesNotExist()
    }

    @Test
    fun `a payment opens the subscription that received it`() {
        var opened: Int? = null
        compose.setContent {
            MaterialTheme { CashContent(PayerFinderState("ramos", listOf(result)), {}, onOpenSubscription = { opened = it }, onNewCharge = {}) }
        }

        compose.onNodeWithText("Jorge Ramos Villanueva").assertExists()
        compose.onNodeWithText("S/ 50.00").assertExists()
        compose.onNodeWithText("Jorge Ramos Villanueva").performClick()

        assertEquals(1842, opened)
    }

    @Test
    fun `new charge starts from the subscriptions search`() {
        var newCharge = false
        compose.setContent { MaterialTheme { CashContent(PayerFinderState(), {}, {}, onNewCharge = { newCharge = true }) } }

        compose.onNodeWithTag(CashTestTags.NEW_CHARGE).performClick()

        assertTrue(newCharge)
    }

    @Test
    fun `search failure shows a readable message`() {
        val repository = mockk<IRepository>()
        coEvery { repository.findPaymentByElectronicPayerName(any()) } throws IllegalStateException("HTTP 500")
        val viewModel = PayerFinderViewmodel(repository, mockk<ObservabilityClient>(relaxed = true))

        viewModel.onEvent(PayerFinderEvent.SearchQueryChanged("ramos"))

        assertEquals("No se pudo buscar. Revisa tu conexión y vuelve a intentar.", viewModel.state.value.error)
    }
}
