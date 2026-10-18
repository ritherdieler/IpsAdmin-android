package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<PaymentsGateway>()
    private lateinit var viewModel: PaymentSearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.methods() } returns listOf("Efectivo", "Yape")
        coEvery { gateway.search(any(), any(), any()) } returns ccori()
        viewModel = PaymentSearchViewModel(gateway, dispatcher) { LocalDate.of(2026, 10, 18) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `opening lists the last 30 days and typing searches once it settles`() = runTest(dispatcher) {
        viewModel.onIntent(PaymentSearchIntent.Load)
        advanceUntilIdle()
        assertEquals(listOf("Efectivo", "Yape"), viewModel.uiState.value.methods)
        assertEquals("4 pagos en los últimos 30 días", viewModel.uiState.value.result?.footer)
        coVerify(exactly = 1) { gateway.search("", null, 30) }

        viewModel.onIntent(PaymentSearchIntent.QueryChanged("cc"))
        advanceTimeBy(100)
        viewModel.onIntent(PaymentSearchIntent.QueryChanged("ccori"))
        advanceUntilIdle()

        coVerify(exactly = 0) { gateway.search("cc", any(), any()) }
        coVerify(exactly = 1) { gateway.search("ccori", null, 30) }
        assertEquals(listOf("ccori"), viewModel.uiState.value.words)
    }

    @Test
    fun `method and period narrow the search`() = runTest(dispatcher) {
        viewModel.onIntent(PaymentSearchIntent.Load)
        viewModel.onIntent(PaymentSearchIntent.MethodSelected("Yape"))
        viewModel.onIntent(PaymentSearchIntent.PeriodSelected(PaymentPeriod.LAST_7))
        advanceUntilIdle()

        coVerify { gateway.search("", "Yape", 7) }
        assertEquals("Yape", viewModel.uiState.value.method)
    }

    @Test
    fun `a failed search keeps a readable error`() = runTest(dispatcher) {
        coEvery { gateway.search(any(), any(), any()) } throws IllegalStateException("HTTP 500")

        viewModel.onIntent(PaymentSearchIntent.Load)
        advanceUntilIdle()

        assertEquals("No se pudieron buscar los pagos. Revisa tu conexión y vuelve a intentar.", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.result)
    }

    @Test
    fun `the detail loads one payment`() = runTest(dispatcher) {
        coEvery { gateway.payment(46790) } returns PAID_JULY
        val detail = PaymentReceiptViewModel(gateway, dispatcher)

        detail.load(46790)
        advanceUntilIdle()

        assertEquals("Pago de jul 2026", detail.uiState.value.detail?.title)
        assertEquals(1842, detail.uiState.value.detail?.subscriptionId)
    }
}
