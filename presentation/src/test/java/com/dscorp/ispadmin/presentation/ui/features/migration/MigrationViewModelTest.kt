package com.dscorp.ispadmin.presentation.ui.features.migration

import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.example.data2.data.apirequestmodel.MigrationRequest
import com.example.data2.data.repository.IRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MigrationViewModelTest {

    private lateinit var repository: IRepository
    private lateinit var migrationViewModel: MigrationViewModel

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = Mockito.mock(IRepository::class.java)
        migrationViewModel = MigrationViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when migration is done then should return Success state`() = runTest(dispatcher) {
        // Given
        val migrationRequest = Mockito.mock(MigrationRequest::class.java)
        Mockito.`when`(migrationRequest.isValid()).thenReturn(true)
        val subscription = Mockito.mock(SubscriptionResponse::class.java)
        Mockito.`when`(repository.doMigration(migrationRequest)).thenAnswer {
            runBlocking { return@runBlocking subscription }
        }

        val collectedStates = mutableListOf<MigrationUiState>()

        // When
        backgroundScope.launch {
            migrationViewModel.uiState.onEach { collectedStates.add(it) }.collect {}
        }
        migrationViewModel.doMigration(migrationRequest)

        // Then
        assertEquals(MigrationUiState.Empty, collectedStates[0])
        assertEquals(MigrationUiState.Loading, collectedStates[1])
        assertEquals(MigrationUiState.Success(subscription), collectedStates[2])
    }

    @Test
    fun `doMigration sets uiState to Loading and then Error when repository call fails`() =
        runTest {
            // Given
            val migrationRequest = Mockito.mock(MigrationRequest::class.java)
            val exception = NullPointerException("error")
            Mockito.`when`(migrationRequest.isValid()).thenReturn(true)

            Mockito.`when`(repository.doMigration(migrationRequest)).thenAnswer {
                runBlocking { throw exception }
            }
            val collectedStates = mutableListOf<MigrationUiState>()

            // When
            backgroundScope.launch(dispatcher) {
                migrationViewModel.uiState.onEach { collectedStates.add(it) }.collect {}
            }
            migrationViewModel.doMigration(migrationRequest)

            // Then
            assertEquals(MigrationUiState.Empty, collectedStates[0])
            assertEquals(MigrationUiState.Loading, collectedStates[1])
            assertEquals(MigrationUiState.Error(exception), collectedStates[2])

        }

    @Test
    fun `getPlans should return success`() = runTest {
        // Given
        val plans = listOf<PlanResponse>()
        val unconfirmedOnus = listOf<Onu>()
        Mockito.`when`(repository.getPlans()).thenAnswer {
            runBlocking { return@runBlocking plans }
        }
        Mockito.`when`(repository.getUnconfirmedOnus()).thenAnswer {
            runBlocking { return@runBlocking unconfirmedOnus }
        }

        val collectedStates = mutableListOf<MigrationUiState>()

        // When
        backgroundScope.launch(dispatcher) {
            migrationViewModel.uiState.onEach {collectedStates.add(it)}.collect {}
        }
//        migrationViewModel.getMigrationFormData()

        // Then
        assertEquals(MigrationUiState.Empty, collectedStates[0])
        assertEquals(MigrationUiState.Loading, collectedStates[1])
//        assertEquals(MigrationUiState.FormDataReady(plans, unconfirmedOnus, subscription), collectedStates[2])
    }

}