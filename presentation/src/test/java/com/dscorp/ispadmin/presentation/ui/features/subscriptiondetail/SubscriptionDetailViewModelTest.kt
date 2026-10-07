package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.CustomerDetail
import com.dscorp.ispadmin.domain.model.CustomerServiceSummary
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `shows the other services of the same customer`() = runTest(dispatcher) {
        val repository = mockk<IRepository>()
        val actions = mockk<SubscriptionActionsRepository>()
        coEvery { repository.subscriptionById(10) } returns subscription(id = 10, customerId = 40)
        coEvery { actions.getCustomer(40) } returns CustomerDetail(
            id = 40,
            subscriptions = listOf(
                CustomerServiceSummary(id = 10, address = "Casa", planName = "f50"),
                CustomerServiceSummary(id = 11, address = "Tienda", planName = "f100"),
            ),
        )
        val viewModel = SubscriptionDetailViewModel(repository, mockk(relaxed = true), actions)

        viewModel.getSubscription(10)
        advanceUntilIdle()

        val others = viewModel.uiState.value.otherServices
        assertEquals(1, others.size)
        assertEquals(11, others.single().id)
        assertEquals("Tienda", others.single().address)
    }

    private fun subscription(id: Int, customerId: Int) = SubscriptionResponse(
        id = id,
        serviceStatus = ServiceStatus.ACTIVE,
        isMigration = false,
        installationType = InstallationType.FIBER,
        email = null,
        pendingInvoiceQuantity = 0,
        antiquityInMonths = 1,
        qualification = 0,
        ics = 0,
        totalDebt = 0.0,
        lastPaymentDate = null,
        customerId = customerId,
    )
}
