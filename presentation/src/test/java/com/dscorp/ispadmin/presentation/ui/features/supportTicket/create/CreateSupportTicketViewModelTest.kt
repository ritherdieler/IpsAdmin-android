package com.dscorp.ispadmin.presentation.ui.features.supportTicket.create

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionFastSearchResponse
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.observability.ObservabilityClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateSupportTicketViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: IRepository
    private lateinit var viewModel: CreateSupportTicketViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        coEvery { repository.getPlaces() } returns emptyList()
        viewModel = CreateSupportTicketViewModel(
            repository = repository,
            observabilityClient = mockk<ObservabilityClient>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting a client loads subscription and fills name phone ip and location`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { repository.subscriptionById(10) } returns subscription(
            id = 10,
            firstName = "Ana",
            lastName = "Lopez",
            phone = "987654321",
            ip = "192.168.1.50",
            placeName = "Huacho"
        )

        viewModel.updateSelectedSubscription(SubscriptionFastSearchResponse(id = 10, fullName = "Ana Lopez"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingSubscription)
        assertTrue(state.hasLoadedClientDetails)
        assertEquals("Ana Lopez", state.clientName)
        assertEquals("987654321", state.phone)
        assertEquals("192.168.1.50", state.clientIp)
        assertEquals("Huacho", state.clientLocation)
        coVerify(exactly = 1) { repository.subscriptionById(10) }
    }

    @Test
    fun `changing client clears previous phone ip and location immediately`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { repository.subscriptionById(1) } returns subscription(
            id = 1,
            phone = "111111111",
            ip = "10.0.0.1",
            placeName = "Lima"
        )
        coEvery { repository.subscriptionById(2) } coAnswers {
            delay(1_000)
            subscription(id = 2, phone = "222222222", ip = "10.0.0.2", placeName = "Barranca")
        }

        viewModel.updateSelectedSubscription(SubscriptionFastSearchResponse(id = 1, fullName = "Cliente Uno"))
        advanceUntilIdle()
        viewModel.updateSelectedSubscription(SubscriptionFastSearchResponse(id = 2, fullName = "Cliente Dos"))

        val loadingState = viewModel.uiState.value
        assertTrue(loadingState.isLoadingSubscription)
        assertFalse(loadingState.hasLoadedClientDetails)
        assertEquals("", loadingState.phone)
        assertEquals("", loadingState.clientIp)
        assertEquals("", loadingState.clientLocation)
        assertEquals("", loadingState.clientName)
    }

    @Test
    fun `stale subscription response is ignored when another client is selected`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { repository.subscriptionById(1) } coAnswers {
            delay(100)
            subscription(id = 1, phone = "111111111", ip = "10.0.0.1", placeName = "Lima")
        }
        coEvery { repository.subscriptionById(2) } returns subscription(
            id = 2,
            firstName = "Maria",
            lastName = "Garcia",
            phone = "222222222",
            ip = "10.0.0.2",
            placeName = "Barranca"
        )

        viewModel.updateSelectedSubscription(SubscriptionFastSearchResponse(id = 1, fullName = "Cliente Uno"))
        viewModel.updateSelectedSubscription(SubscriptionFastSearchResponse(id = 2, fullName = "Maria Garcia"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.selectedSubscription?.id)
        assertEquals("222222222", state.phone)
        assertEquals("10.0.0.2", state.clientIp)
        assertEquals("Barranca", state.clientLocation)
        assertEquals("Maria Garcia", state.clientName)
    }

    @Test
    fun `subscription load error clears client data and exposes error`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { repository.subscriptionById(5) } throws IllegalStateException("timeout")

        viewModel.updateSelectedSubscription(SubscriptionFastSearchResponse(id = 5, fullName = "Error"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingSubscription)
        assertFalse(state.hasLoadedClientDetails)
        assertEquals("", state.phone)
        assertEquals("", state.clientIp)
        assertEquals("", state.clientLocation)
        assertNotNull(state.subscriptionLoadError)
    }

    @Test
    fun `loading subscription does not overwrite category or description`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.updateCategory("Internet Lento")
        viewModel.updateDescription("Se cae cada hora")
        coEvery { repository.subscriptionById(8) } returns subscription(id = 8)

        viewModel.updateSelectedSubscription(SubscriptionFastSearchResponse(id = 8, fullName = "Juan Perez"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Internet Lento", state.category)
        assertEquals("Se cae cada hora", state.description)
    }

    @Test
    fun `create ticket sends phone loaded from subscription`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { repository.subscriptionById(10) } returns subscription(
            id = 10,
            phone = "987654321",
            ip = "192.168.0.9",
            placeName = "Huacho"
        )
        val requestSlot = slot<com.dscorp.ispadmin.data.apirequestmodel.AssistanceTicketRequest>()
        coEvery { repository.createTicket(capture(requestSlot)) } returns mockk(relaxed = true)

        viewModel.updateSelectedSubscription(SubscriptionFastSearchResponse(id = 10, fullName = "Ana Lopez"))
        advanceUntilIdle()
        viewModel.updateCategory("Otros")
        viewModel.updateDescription("Sin internet")
        viewModel.createTicket()
        advanceUntilIdle()

        assertEquals("987654321", requestSlot.captured.phone)
        assertEquals(10, requestSlot.captured.subscriptionId)
        assertTrue(viewModel.uiState.value.isTicketCreated)
    }

    @Test
    fun `changing the subscriber does not flag an error until the form is sent`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateSelectedSubscription(null)
        assertEquals(null, viewModel.uiState.value.subscriptionError)

        viewModel.updateCategory("Otros")
        viewModel.updateDescription("Sin internet")
        viewModel.createTicket()
        advanceUntilIdle()
        assertEquals("Debe seleccionar un cliente", viewModel.uiState.value.subscriptionError)
    }

    @Test
    fun `an optional assignee gets the new ticket right after it is created`() = runTest(testDispatcher) {
        advanceUntilIdle()
        coEvery { repository.getTechnicians() } returns listOf(
            com.dscorp.ispadmin.domain.model.User(id = 7, name = "marco", lastName = "salinas"),
        )
        coEvery { repository.createTicket(any()) } returns mockk(relaxed = true) { io.mockk.every { id } returns 3613 }
        coEvery { repository.assignSupportTicketToUser(3613, com.dscorp.ispadmin.data.response.AssistanceTicketStatus.ASSIGNED, 7) } returns mockk(relaxed = true)
        coEvery { repository.subscriptionById(10) } returns subscription(id = 10)

        viewModel.loadTechnicians()
        advanceUntilIdle()
        assertEquals(listOf(7 to "Marco Salinas"), viewModel.uiState.value.technicians)
        viewModel.updateSelectedSubscription(SubscriptionFastSearchResponse(id = 10, fullName = "Ana Lopez"))
        advanceUntilIdle()
        viewModel.updateCategory("Otros")
        viewModel.updateDescription("Sin internet")
        viewModel.updateAssignee(7)
        viewModel.createTicket()
        advanceUntilIdle()

        io.mockk.coVerify(exactly = 1) {
            repository.assignSupportTicketToUser(3613, com.dscorp.ispadmin.data.response.AssistanceTicketStatus.ASSIGNED, 7)
        }
        assertTrue(viewModel.uiState.value.isTicketCreated)
    }

    private fun subscription(
        id: Int,
        firstName: String = "Juan",
        lastName: String = "Perez",
        phone: String? = "987654321",
        ip: String? = "10.0.0.1",
        placeName: String? = "Huacho"
    ) = SubscriptionResponse(
        id = id,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        ip = ip,
        place = placeName?.let { Place(id = "1", name = it) },
        serviceStatus = ServiceStatus.ACTIVE,
        isMigration = false,
        installationType = InstallationType.FIBER,
        email = null,
        pendingInvoiceQuantity = 0,
        antiquityInMonths = 0,
        qualification = 0,
        ics = 0,
        totalDebt = 0.0,
        lastPaymentDate = null
    )
}
