package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** a-cmp-02..10: a charge with a pending proof must use it or discard it; it may carry a discount. */
@OptIn(ExperimentalCoroutinesApi::class)
class PendingProofChargeTest {
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<SubscriptionHubGateway>()
    private lateinit var viewModel: SubscriptionHubViewModel

    private val whatsApp = PendingProof(
        inboundMessageId = 900, provider = "YAPE", amount = 120.0, operation = "03948211", received = "hoy 10:42 a. m.",
        payerName = "Jorge Ramos V.",
    )
    private val app = PendingProof(
        appProofId = "p_55", provider = "PLIN", amount = 150.0, operation = "04112233", received = "hoy 9:15 a. m.",
        paymentIds = listOf(1), otherServiceFees = listOf(PendingProofFee(9001, "Las Flores 88", "Set 2026", 50.0)),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.subscription(1842) } returns subscription()
        coEvery { gateway.recentPayments(1842, any()) } returns listOf(receipt(2, 200), receipt(1, 100))
        coEvery { gateway.paymentMethods() } returns METHODS
        every { gateway.currentUser() } returns User(id = 7, name = "Rosa", type = User.UserType.SECRETARY)
        every { gateway.lastMethod() } returns "Efectivo"
        every { gateway.rememberMethod(any()) } returns Unit
        coEvery { gateway.restoreInternet(any()) } returns Result.success(Unit)
        viewModel = SubscriptionHubViewModel(gateway, mainImmediate = dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun kotlinx.coroutines.test.TestScope.openWith(vararg proofs: PendingProof) {
        coEvery { gateway.pendingProofs(1842) } returns proofs.toList()
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()
        viewModel.onIntent(SubscriptionHubIntent.OpenCharge)
        advanceUntilIdle()
    }

    private val draft get() = viewModel.uiState.value.charge!!.draft

    @Test
    fun `a pending proof blocks the charge until staff use it`() = runTest(dispatcher) {
        openWith(whatsApp)

        assertEquals("Usa el comprobante o descártalo para continuar", draft.blockingReason)
        viewModel.onIntent(SubscriptionHubIntent.MethodSelected("Plin"))
        assertEquals("Efectivo", draft.method)
    }

    @Test
    fun `a WhatsApp receipt fixes the method and payer and goes with the charge`() = runTest(dispatcher) {
        val batch = slot<ChargeBatch>()
        coEvery { gateway.registerCharge(capture(batch)) } returns emptyList()
        openWith(whatsApp)

        viewModel.onIntent(SubscriptionHubIntent.UseProof(whatsApp.key))
        assertEquals("Yape", draft.method)
        assertEquals(20.0, draft.leftover!!, 0.001)
        // The receipt covers everything: no discount is offered with it.
        assertNull(draft.proofShortfall)
        assertEquals(false, draft.offersDiscount)
        viewModel.onIntent(SubscriptionHubIntent.SubmitCharge)
        advanceUntilIdle()

        assertEquals(
            ChargeBatch(listOf(1, 2), "Yape", 0.0, null, "Jorge Ramos V.", responsibleId = 7, inboundMessageId = 900, amountReceived = 120.0),
            batch.captured,
        )
        assertNull(viewModel.uiState.value.charge)
    }

    @Test
    fun `a receipt worth less than the total asks to mark only what it covers or apply the agreed discount`() = runTest(dispatcher) {
        openWith(whatsApp.copy(amount = 50.0))

        viewModel.onIntent(SubscriptionHubIntent.UseProof(whatsApp.key))

        assertEquals(
            "El comprobante es de S/ 50.00: marca solo los recibos que cubre o registra el descuento acordado",
            draft.blockingReason,
        )
        assertEquals(50.0, draft.proofShortfall!!, 0.001)
        viewModel.onIntent(SubscriptionHubIntent.ReceiptToggled(2))
        assertNull(draft.blockingReason)
    }

    @Test
    fun `a receipt below the total is registered with the discount it leaves and its reason`() = runTest(dispatcher) {
        val batch = slot<ChargeBatch>()
        coEvery { gateway.registerCharge(capture(batch)) } returns emptyList()
        openWith(whatsApp.copy(amount = 90.0))

        viewModel.onIntent(SubscriptionHubIntent.UseProof(whatsApp.key))
        viewModel.onIntent(SubscriptionHubIntent.DiscountToggled)
        // The amount is not typed: it is what the receipt leaves unpaid.
        viewModel.onIntent(SubscriptionHubIntent.DiscountChanged("3"))
        assertEquals(10.0, draft.discountValue, 0.001)
        assertEquals(90.0, draft.toPay, 0.001)
        assertEquals("Elige el motivo del descuento", draft.blockingReason)
        viewModel.onIntent(SubscriptionHubIntent.DiscountReasonSelected("Otro"))
        assertEquals("Escribe el detalle del motivo", draft.blockingReason)
        viewModel.onIntent(SubscriptionHubIntent.DiscountDetailChanged("3 días sin servicio"))
        assertNull(draft.blockingReason)
        viewModel.onIntent(SubscriptionHubIntent.SubmitCharge)
        advanceUntilIdle()

        assertEquals(
            ChargeBatch(
                listOf(1, 2), "Yape", 10.0, "Otro: 3 días sin servicio", "Jorge Ramos V.",
                responsibleId = 7, inboundMessageId = 900, amountReceived = 90.0,
            ),
            batch.captured,
        )
    }

    @Test
    fun `a Mi GigaFiber proof pays the chosen fees of every service in one charge, with the discount`() = runTest(dispatcher) {
        coEvery { gateway.chargeWithAppProof(any(), any(), any(), any(), any()) } returns Unit
        openWith(app.copy(amount = 95.0))

        viewModel.onIntent(SubscriptionHubIntent.UseProof(app.key))
        assertEquals(setOf(1), draft.selectedIds)
        assertEquals("Plin", draft.method)
        assertEquals(100.0, draft.total, 0.001)
        viewModel.onIntent(SubscriptionHubIntent.ReceiptToggled(2))
        assertEquals(setOf(1), draft.selectedIds)
        viewModel.onIntent(SubscriptionHubIntent.DiscountToggled)
        viewModel.onIntent(SubscriptionHubIntent.DiscountReasonSelected("Fallas de internet"))
        viewModel.onIntent(SubscriptionHubIntent.SubmitCharge)
        advanceUntilIdle()

        coVerify { gateway.chargeWithAppProof("p_55", listOf(1, 9001), null, 5.0, "Fallas de internet") }
        coVerify(exactly = 0) { gateway.registerCharge(any()) }
    }

    @Test
    fun `discarding needs a reason and then shows the next pending proof`() = runTest(dispatcher) {
        coEvery { gateway.discardProof(any(), any()) } returns Unit
        openWith(whatsApp, app)

        viewModel.onIntent(SubscriptionHubIntent.DiscardProof(whatsApp.key))
        viewModel.onIntent(SubscriptionHubIntent.ConfirmDiscard)
        coVerify(exactly = 0) { gateway.discardProof(any(), any()) }
        coEvery { gateway.pendingProofs(1842) } returns listOf(app)
        viewModel.onIntent(SubscriptionHubIntent.DiscardReasonPicked(DiscardReason.MISREAD))
        viewModel.onIntent(SubscriptionHubIntent.ConfirmDiscard)
        advanceUntilIdle()

        coVerify { gateway.discardProof(whatsApp, DiscardReason.MISREAD) }
        assertNull(viewModel.uiState.value.charge!!.discarding)
        assertEquals(listOf(app.key), draft.pending.map { it.key })
    }

    @Test
    fun `a 409 from Core shows its message and reloads the pending proofs`() = runTest(dispatcher) {
        coEvery { gateway.registerCharge(any()) } throws CoreRejection(409, "Este cliente tiene un comprobante por revisar")
        openWith()
        viewModel.onIntent(SubscriptionHubIntent.SubmitCharge)
        coEvery { gateway.pendingProofs(1842) } returns listOf(whatsApp)
        advanceUntilIdle()

        assertEquals("Este cliente tiene un comprobante por revisar", viewModel.uiState.value.charge!!.error)
        assertEquals(listOf(whatsApp.key), draft.pending.map { it.key })
    }

    @Test
    fun `the reasons map to the CRM codes of each channel`() {
        assertEquals("MISREAD" to "MISREAD", DiscardReason.MISREAD.whatsApp to DiscardReason.MISREAD.app)
        assertEquals("DUPLICATE" to "ALREADY_USED", DiscardReason.DUPLICATE.whatsApp to DiscardReason.DUPLICATE.app)
        assertEquals("NOT_VOUCHER" to "OTHER", DiscardReason.NOT_VOUCHER.whatsApp to DiscardReason.NOT_VOUCHER.app)
        assertEquals(ServiceStatus.ACTIVE, ServiceStatus.ACTIVE)
    }
}
