package com.dscorp.ispadmin.presentation.ui.features.payment.register

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.User.UserType
import com.dscorp.ispadmin.observability.ObservabilityClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterPaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: IRepository
    private lateinit var observabilityClient: ObservabilityClient
    private lateinit var viewModel: RegisterPaymentViewModel

    private val user = User(
        id = 1,
        name = "Test",
        lastName = "User",
        username = "test",
        type = UserType.ADMIN,
        verified = true,
        email = "",
        phone = "",
        dni = ""
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        observabilityClient = mockk(relaxed = true)
        every { repository.getUserSession() } returns user
        viewModel = RegisterPaymentViewModel(repository, observabilityClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun registerPayment_callsRepositoryRegisterPayment() = runTest {
        val payment = Payment(
            id = 99,
            subscriptionId = 882,
            amountToPay = 50.0,
            paid = false,
            method = "",
            responsibleId = 0
        )
        coEvery { repository.registerPayment(any()) } returns Payment(
            id = 99,
            subscriptionId = 882,
            amountToPay = 50.0,
            paid = true,
            method = "cash",
            responsibleId = 1
        )

        viewModel.onEvent(RegisterPaymentEvent.SetPayment(payment))
        viewModel.onEvent(RegisterPaymentEvent.PaymentMethodSelected("cash"))
        viewModel.onEvent(RegisterPaymentEvent.RegisterPayment)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.registerPayment(
                match {
                    it.id == 99 && it.method == "cash" && it.responsibleId == 1
                }
            )
        }
        verify { observabilityClient.addBreadcrumb(any(), any(), any()) }
        assertTrue(viewModel.state.value.isSuccess)
    }

    @Test
    fun loadPaymentData_loadsCoreCatalogAndValidatesPayerNameFromIt() = runTest {
        val payment = Payment(id = 99, subscriptionId = 882, amountToPay = 50.0, paid = false, method = "", responsibleId = 0)
        coEvery { repository.getPaymentById("99") } returns payment
        coEvery { repository.getPaymentMethods() } returns listOf(
            com.dscorp.ispadmin.domain.model.PaymentMethodOption("Efectivo", requiresPayerName = false),
            com.dscorp.ispadmin.domain.model.PaymentMethodOption("Billetera", requiresPayerName = true),
        )

        viewModel.onEvent(RegisterPaymentEvent.LoadPaymentData(99))
        advanceUntilIdle()
        org.junit.Assert.assertEquals(listOf("Efectivo", "Billetera"), viewModel.state.value.paymentMethods.map { it.name })

        viewModel.onEvent(RegisterPaymentEvent.PaymentMethodSelected("Billetera"))
        viewModel.onEvent(RegisterPaymentEvent.RegisterPayment)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.errorMessages.containsKey("electronicPayerName"))
        coVerify(exactly = 0) { repository.registerPayment(any()) }
    }

    @Test
    fun registerPayment_sendsTheAmountReceivedWhenEntered() = runTest {
        val payment = Payment(id = 99, subscriptionId = 882, amountToPay = 50.0, paid = false, method = "", responsibleId = 0)
        coEvery { repository.registerPayment(any()) } returns payment

        viewModel.onEvent(RegisterPaymentEvent.SetPayment(payment))
        viewModel.onEvent(RegisterPaymentEvent.PaymentMethodSelected("Efectivo"))
        viewModel.onEvent(RegisterPaymentEvent.AmountReceivedChanged("60"))
        viewModel.onEvent(RegisterPaymentEvent.RegisterPayment)
        advanceUntilIdle()

        coVerify { repository.registerPayment(match { it.amountReceived == 60.0 }) }
    }
}
