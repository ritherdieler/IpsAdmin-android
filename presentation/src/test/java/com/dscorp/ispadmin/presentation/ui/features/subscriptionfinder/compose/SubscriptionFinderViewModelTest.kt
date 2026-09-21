package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.CustomerData
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.PagedResult
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.domain.model.needsTr069Retry
import com.dscorp.ispadmin.domain.usecase.service.ReactivateServiceUseCase
import com.dscorp.ispadmin.domain.usecase.service.RebootFiberOnuUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RetryTr069ProvisioningUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.SearchSubscriptionsUseCase
import com.dscorp.ispadmin.observability.ObservabilityClient
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionFinderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: IRepository
    private lateinit var reactivateServiceUseCase: ReactivateServiceUseCase
    private lateinit var rebootFiberOnuUseCase: RebootFiberOnuUseCase
    private lateinit var searchSubscriptionsUseCase: SearchSubscriptionsUseCase
    private lateinit var observabilityClient: ObservabilityClient
    private lateinit var retryTr069ProvisioningUseCase: RetryTr069ProvisioningUseCase

    private lateinit var viewModel: SubscriptionFinderViewModel

    private fun resume(id: Int) = SubscriptionResume(
        id = id,
        planName = "Plan",
        customerName = "JUAN PEREZ",
        antiquity = "1",
        qualification = "A",
        placeName = "Lima",
        ics = "1",
        lastPaymentDate = null,
        pendingInvoicesQuantity = 0,
        totalDebt = 0.0,
        ipAddress = "",
        customer = CustomerData(
            subscriptionId = id,
            name = "JUAN",
            lastName = "PEREZ",
            dni = "12345678",
            place = "Lima",
            address = "Calle 1",
            phone = "999999999",
            email = ""
        ),
        serviceStatus = ServiceStatus.ACTIVE,
        installationType = InstallationType.FIBER,
        napBox = null,
        placeId = "1",
        location = GeoLocation()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        reactivateServiceUseCase = mockk()
        rebootFiberOnuUseCase = mockk()
        searchSubscriptionsUseCase = mockk()
        observabilityClient = mockk(relaxed = true)
        retryTr069ProvisioningUseCase = mockk()

        viewModel = SubscriptionFinderViewModel(
            repository = repository,
            reactivateServiceUseCase = reactivateServiceUseCase,
            rebootFiberOnuUseCase = rebootFiberOnuUseCase,
            searchSubscriptionsUseCase = searchSubscriptionsUseCase,
            observabilityClient = observabilityClient,
            retryTr069ProvisioningUseCase = retryTr069ProvisioningUseCase
        )
        viewModel.observeSubscriptions()
        viewModel.findSubscription()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search by name returns grouped results and updates pagination state`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { searchSubscriptionsUseCase.invoke("JUAN", null, 0, 20) } returns Result.success(
            PagedResult(items = listOf(resume(1), resume(2)), page = 0, size = 20, total = 2, totalPages = 1)
        )

        viewModel.documentNumberFlow.emit(SubscriptionFilter.BY_NAME(name = "JUAN", lastName = ""))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSearching)
        assertTrue(state.searchPerformed)
        assertFalse(state.canLoadMore)
        assertEquals(2, state.subscriptions[ServiceStatus.ACTIVE]?.size)
        coVerify(exactly = 1) { searchSubscriptionsUseCase.invoke("JUAN", null, 0, 20) }
    }

    @Test
    fun `search by name sets error message when use case fails`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { searchSubscriptionsUseCase.invoke(any(), any(), any(), any()) } returns
            Result.failure(Exception("network"))

        viewModel.documentNumberFlow.emit(SubscriptionFilter.BY_NAME(name = "JUAN", lastName = ""))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSearching)
        assertNotNull(state.searchError)
    }

    @Test
    fun `search by name with no matches marks search performed and empty results`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { searchSubscriptionsUseCase.invoke(any(), any(), any(), any()) } returns
            Result.success(PagedResult())

        viewModel.documentNumberFlow.emit(SubscriptionFilter.BY_NAME(name = "ZZZ", lastName = ""))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSearching)
        assertTrue(state.searchPerformed)
        assertTrue(state.subscriptions.isEmpty())
    }

    @Test
    fun `load next page appends results and updates page`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { searchSubscriptionsUseCase.invoke("JUAN", null, 0, 20) } returns Result.success(
            PagedResult(items = listOf(resume(1)), page = 0, size = 20, total = 2, totalPages = 2)
        )
        coEvery { searchSubscriptionsUseCase.invoke("JUAN", null, 1, 20) } returns Result.success(
            PagedResult(items = listOf(resume(2)), page = 1, size = 20, total = 2, totalPages = 2)
        )

        viewModel.documentNumberFlow.emit(SubscriptionFilter.BY_NAME(name = "JUAN", lastName = ""))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canLoadMore)
        assertEquals(0, viewModel.uiState.value.currentPage)

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentPage)
        assertFalse(state.canLoadMore)
        assertEquals(2, state.subscriptions[ServiceStatus.ACTIVE]?.size)
        coVerify(exactly = 1) { searchSubscriptionsUseCase.invoke("JUAN", null, 1, 20) }
    }

    @Test
    fun `needsTr069Retry is true only when fiber tr069 is incomplete`() {
        assertTrue(resume(1).copy(tr069ProvisionStatus = "PENDING").needsTr069Retry())
        assertTrue(resume(1).copy(tr069ProvisionStatus = "MANUAL_REQUIRED").needsTr069Retry())
        assertTrue(resume(1).copy(tr069ProvisionStatus = "FAILED").needsTr069Retry())
        assertFalse(resume(1).copy(tr069ProvisionStatus = "COMPLETE").needsTr069Retry())
        assertFalse(resume(1).copy(tr069ProvisionStatus = null).needsTr069Retry())
        assertFalse(
            resume(1).copy(
                installationType = InstallationType.WIRELESS,
                tr069ProvisionStatus = "PENDING"
            ).needsTr069Retry()
        )
        assertFalse(
            resume(1).copy(
                serviceStatus = ServiceStatus.CANCELLED,
                tr069ProvisionStatus = "FAILED"
            ).needsTr069Retry()
        )
    }

    @Test
    fun `retryTr069 updates list status and exposes result message`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val pending = resume(7).copy(tr069ProvisionStatus = "PENDING")
        coEvery { searchSubscriptionsUseCase.invoke("JUAN", null, 0, 20) } returns Result.success(
            PagedResult(items = listOf(pending), page = 0, size = 20, total = 1, totalPages = 1)
        )
        coEvery { retryTr069ProvisioningUseCase.invoke(7) } returns Result.success(
            Subscription(
                subscriptionId = 7,
                tr069ProvisionStatus = "COMPLETE",
                tr069Message = "Listo"
            )
        )

        viewModel.documentNumberFlow.emit(SubscriptionFilter.BY_NAME(name = "JUAN", lastName = ""))
        advanceUntilIdle()
        viewModel.retryTr069(7)
        advanceUntilIdle()

        val listed = viewModel.uiState.value.subscriptions[ServiceStatus.ACTIVE]?.single()
        assertEquals("COMPLETE", listed?.tr069ProvisionStatus)
        val feedback = viewModel.uiState.value.retryTr069State
        assertTrue(feedback is RetryTr069State.Success)
        assertEquals(
            "Aprovisionamiento TR-069 completado",
            (feedback as RetryTr069State.Success).message
        )
        coVerify(exactly = 1) { retryTr069ProvisioningUseCase.invoke(7) }
    }

    @Test
    fun `retryTr069 still pending keeps the acs message as feedback`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { searchSubscriptionsUseCase.invoke(any(), any(), any(), any()) } returns Result.success(
            PagedResult(
                items = listOf(resume(7).copy(tr069ProvisionStatus = "FAILED")),
                page = 0,
                size = 20,
                total = 1,
                totalPages = 1
            )
        )
        coEvery { retryTr069ProvisioningUseCase.invoke(7) } returns Result.success(
            Subscription(
                subscriptionId = 7,
                tr069ProvisionStatus = "PENDING",
                tr069Message = "IP/SSID no se confirmaron"
            )
        )

        viewModel.documentNumberFlow.emit(SubscriptionFilter.BY_NAME(name = "JUAN", lastName = ""))
        advanceUntilIdle()
        viewModel.retryTr069(7)
        advanceUntilIdle()

        val listed = viewModel.uiState.value.subscriptions[ServiceStatus.ACTIVE]?.single()
        assertEquals("PENDING", listed?.tr069ProvisionStatus)
        val feedback = viewModel.uiState.value.retryTr069State as RetryTr069State.Success
        assertEquals("IP/SSID no se confirmaron", feedback.message)
    }

    @Test
    fun `retryTr069 exposes backend error`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { retryTr069ProvisioningUseCase.invoke(9) } returns
            Result.failure(IllegalStateException("OLT no está COMPLETE"))

        viewModel.retryTr069(9)
        advanceUntilIdle()

        val feedback = viewModel.uiState.value.retryTr069State
        assertTrue(feedback is RetryTr069State.Error)
        assertEquals("OLT no está COMPLETE", (feedback as RetryTr069State.Error).message)
    }
}
