package com.dscorp.ispadmin.presentation.ui.features.installationorders

import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.PlaceUseCase
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class CreateInstallationOrderViewModelTest {

    private lateinit var viewModel: CreateInstallationOrderViewModel
    private lateinit var installationOrderUseCase: InstallationOrderUseCase
    private lateinit var userUseCase: UserUseCase
    private lateinit var placeUseCase: PlaceUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        installationOrderUseCase = mockk()
        userUseCase = mockk()
        placeUseCase = mockk()
        
        val currentUser = User(id = 1, name = "Test", lastName = "User")
        coEvery { userUseCase.getCurrentUser() } returns currentUser
        
        viewModel = CreateInstallationOrderViewModel(
            installationOrderUseCase = installationOrderUseCase,
            userUseCase = userUseCase,
            placeUseCase = placeUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when form is empty, isFormValid should be false`() = runTest {
        val initialState = viewModel.uiState.value
        assertFalse(initialState.isFormValid)
    }

    @Test
    fun `when all fields are filled, isFormValid should be true`() = runTest {
        val place = Place(id = "1", name = "Test Place")
        
        viewModel.onEvent(InstallationOrderEvent.OnFirstNameChange("John"))
        viewModel.onEvent(InstallationOrderEvent.OnLastNameChange("Doe"))
        viewModel.onEvent(InstallationOrderEvent.OnAddressChange("123 Test St"))
        viewModel.onEvent(InstallationOrderEvent.OnPhoneChange("1234567890"))
        viewModel.onEvent(InstallationOrderEvent.OnDniChange("12345678"))
        viewModel.onEvent(InstallationOrderEvent.OnPlaceChange(place))
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `when creating order with valid form, should update state with success`() = runTest {
        val place = Place(id = "1", name = "Test Place")
        val expectedOrder = InstallationOrder(
            id = 1,
            customerFirstName = "John",
            customerLastName = "Doe",
            customerAddress = "123 Test St",
            customerPhone = "1234567890",
            customerDni = "12345678",
            status = InstallationOrderStatus.SOLICITADO,
            seller = User(id = 1, name = "Test", lastName = "User"),
            place = place
        )
        
        coEvery { installationOrderUseCase.createInstallationOrder(any()) } returns expectedOrder
        
        viewModel.onEvent(InstallationOrderEvent.OnFirstNameChange("John"))
        viewModel.onEvent(InstallationOrderEvent.OnLastNameChange("Doe"))
        viewModel.onEvent(InstallationOrderEvent.OnAddressChange("123 Test St"))
        viewModel.onEvent(InstallationOrderEvent.OnPhoneChange("1234567890"))
        viewModel.onEvent(InstallationOrderEvent.OnDniChange("12345678"))
        viewModel.onEvent(InstallationOrderEvent.OnPlaceChange(place))
        viewModel.onEvent(InstallationOrderEvent.OnCreateOrder)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals("Orden de instalación creada correctamente", viewModel.uiState.value.successMessage)
        assertEquals(expectedOrder, viewModel.uiState.value.orderCreated)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `when creating order with invalid form, should show error`() = runTest {
        viewModel.onEvent(InstallationOrderEvent.OnCreateOrder)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals("Por favor, complete todos los campos.", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.orderCreated)
    }

    @Test
    fun `when loading places fails, should show error`() = runTest {
        coEvery { placeUseCase.getPlaces() } throws Exception("Error loading places")
        
        viewModel.onEvent(InstallationOrderEvent.OnLoadPlaces)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals("Error loading places", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `when dismissing error, error should be null`() = runTest {
        viewModel.onEvent(InstallationOrderEvent.OnCreateOrder) // This will set an error
        viewModel.onEvent(InstallationOrderEvent.OnDismissError)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `when dismissing success, successMessage should be null`() = runTest {
        val place = Place(id = "1", name = "Test Place")
        coEvery { installationOrderUseCase.createInstallationOrder(any()) } returns InstallationOrder(
            id = 1,
            customerFirstName = "John",
            customerLastName = "Doe",
            customerAddress = "123 Test St",
            customerPhone = "1234567890",
            customerDni = "12345678",
            status = InstallationOrderStatus.SOLICITADO,
            seller = User(id = 1, name = "Test", lastName = "User"),
            place = place
        )
        
        viewModel.onEvent(InstallationOrderEvent.OnFirstNameChange("John"))
        viewModel.onEvent(InstallationOrderEvent.OnLastNameChange("Doe"))
        viewModel.onEvent(InstallationOrderEvent.OnAddressChange("123 Test St"))
        viewModel.onEvent(InstallationOrderEvent.OnPhoneChange("1234567890"))
        viewModel.onEvent(InstallationOrderEvent.OnDniChange("12345678"))
        viewModel.onEvent(InstallationOrderEvent.OnPlaceChange(place))
        viewModel.onEvent(InstallationOrderEvent.OnCreateOrder)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(InstallationOrderEvent.OnDismissSuccess)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.successMessage)
    }
} 