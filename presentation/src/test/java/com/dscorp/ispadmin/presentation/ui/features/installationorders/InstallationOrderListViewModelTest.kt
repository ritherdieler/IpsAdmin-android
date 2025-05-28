package com.dscorp.ispadmin.presentation.ui.features.installationorders

import androidx.paging.PagingData
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class InstallationOrderListViewModelTest {

    private lateinit var viewModel: InstallationOrderListViewModel
    private lateinit var installationOrderUseCase: InstallationOrderUseCase
    private lateinit var userUseCase: UserUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        installationOrderUseCase = mockk()
        userUseCase = mockk()
        
        val currentUser = User(
            id = 1,
            name = "Test",
            lastName = "User",
            type = User.UserType.ADMIN
        )
        coEvery { userUseCase.getCurrentUser() } returns currentUser
        
        viewModel = InstallationOrderListViewModel(
            installationOrderUseCase = installationOrderUseCase,
            userUseCase = userUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when loading installation orders as admin, should get all orders`() = runTest {
        // Given
        val mockOrders = listOf(
            InstallationOrder(
                id = 1,
                customerFirstName = "John",
                customerLastName = "Doe",
                customerAddress = "123 Test St",
                customerPhone = "1234567890",
                status = InstallationOrderStatus.SOLICITADO
            )
        )
        coEvery { installationOrderUseCase.getAllInstallationOrdersPaginated() } returns flowOf(PagingData.from(mockOrders))

        // When
        viewModel.onEvent(InstallationOrderListEvent.LoadInstallationOrders)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { installationOrderUseCase.getAllInstallationOrdersPaginated() }
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `when filtering by status, should update filter and reload orders`() = runTest {
        // Given
        val mockOrders = listOf(
            InstallationOrder(
                id = 1,
                customerFirstName = "John",
                customerLastName = "Doe",
                customerAddress = "123 Test St",
                customerPhone = "1234567890",
                status = InstallationOrderStatus.SOLICITADO
            )
        )
        coEvery { installationOrderUseCase.getAllInstallationOrdersPaginated() } returns flowOf(PagingData.from(mockOrders))

        // When
        viewModel.onEvent(InstallationOrderListEvent.FilterByStatus(InstallationOrderStatus.SOLICITADO))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { installationOrderUseCase.getAllInstallationOrdersPaginated() }
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `when selecting order with SOLICITADO status, should show assign dialog`() = runTest {
        // Given
        val order = InstallationOrder(
            id = 1,
            customerFirstName = "John",
            customerLastName = "Doe",
            customerAddress = "123 Test St",
            customerPhone = "1234567890",
            status = InstallationOrderStatus.SOLICITADO
        )

        // When
        viewModel.onEvent(InstallationOrderListEvent.OrderSelected(order))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.showAssignDialog)
        assertEquals(order, viewModel.uiState.value.selectedOrder)
    }

    @Test
    fun `when loading technicians, should update technicians list`() = runTest {
        // Given
        val technicians = listOf(
            User(id = 1, name = "Tech", lastName = "1", type = User.UserType.TECHNICIAN),
            User(id = 2, name = "Tech", lastName = "2", type = User.UserType.TECHNICIAN)
        )
        coEvery { userUseCase.getTechnicianUsers() } returns technicians

        // When
        viewModel.onEvent(InstallationOrderListEvent.LoadTechnicians)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(technicians, viewModel.uiState.value.technicians)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `when assigning technician with missing data, should show error`() = runTest {
        // When
        viewModel.onEvent(InstallationOrderListEvent.AssignTechnician)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("Debe seleccionar un técnico y una fecha", viewModel.uiState.value.error)
    }

    @Test
    fun `when transferring order with missing data, should show error`() = runTest {
        // When
        viewModel.onEvent(InstallationOrderListEvent.TransferOrder)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("Faltan datos para transferir la orden", viewModel.uiState.value.error)
    }

    @Test
    fun `when closing dialogs, should reset related state`() = runTest {
        // Given
        val order = InstallationOrder(
            id = 1,
            customerFirstName = "John",
            customerLastName = "Doe",
            customerAddress = "123 Test St",
            customerPhone = "1234567890",
            status = InstallationOrderStatus.SOLICITADO
        )
        val technician = User(id = 1, name = "Tech", lastName = "1", type = User.UserType.TECHNICIAN)
        val date = LocalDateTime.now()

        // When
        viewModel.onEvent(InstallationOrderListEvent.OrderSelected(order))
        viewModel.onEvent(InstallationOrderListEvent.TechnicianSelected(technician))
        viewModel.onEvent(InstallationOrderListEvent.ScheduledDateSelected(date))
        viewModel.onEvent(InstallationOrderListEvent.CloseAssignDialog)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showAssignDialog)
        assertNull(viewModel.uiState.value.selectedOrder)
        assertNull(viewModel.uiState.value.selectedTechnician)
        assertNull(viewModel.uiState.value.scheduledDate)
    }
} 