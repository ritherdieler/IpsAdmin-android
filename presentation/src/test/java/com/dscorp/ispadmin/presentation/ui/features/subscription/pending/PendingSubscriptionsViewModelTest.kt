package com.dscorp.ispadmin.presentation.ui.features.subscription.pending

import com.dscorp.ispadmin.domain.exception.NoConnectivity
import com.dscorp.ispadmin.domain.model.PendingSubscription
import com.dscorp.ispadmin.domain.model.PendingSubscriptionStatus
import com.dscorp.ispadmin.domain.usecase.subscription.ObservePendingSubscriptionsUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.PendingSubscriptionSyncResult
import com.dscorp.ispadmin.domain.usecase.subscription.SyncPendingSubscriptionsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class PendingSubscriptionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val observePendingSubscriptionsUseCase = mockk<ObservePendingSubscriptionsUseCase>()
    private val syncPendingSubscriptionsUseCase = mockk<SyncPendingSubscriptionsUseCase>()
    private val discardPendingSubscriptionUseCase = mockk<com.dscorp.ispadmin.domain.usecase.subscription.DiscardPendingSubscriptionUseCase>()
    private lateinit var viewModel: PendingSubscriptionsViewModel
    private val pendingFlow = MutableSharedFlow<List<PendingSubscription>>(replay = 1)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { observePendingSubscriptionsUseCase() } returns Result.success(pendingFlow)
        viewModel = PendingSubscriptionsViewModel(
            observePendingSubscriptionsUseCase = observePendingSubscriptionsUseCase,
            syncPendingSubscriptionsUseCase = syncPendingSubscriptionsUseCase,
            mainImmediate = testDispatcher,
            discardPendingSubscriptionUseCase = discardPendingSubscriptionUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load emits pending list in ui state`() = runTest(testDispatcher) {
        viewModel.onIntent(PendingSubscriptionsIntent.Load)
        advanceUntilIdle()

        pendingFlow.emit(
            listOf(
                PendingSubscription(
                    localId = "local-1",
                    clientRequestId = "client-1",
                    subscriptionJson = """{"firstName":"Ana","lastName":"Perez","dni":"12345678"}""",
                    createdAt = 1_700_000_000_000L,
                    status = PendingSubscriptionStatus.CONFLICT
                )
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.items.size)
        assertEquals("Ana Perez", state.items[0].clientName)
        assertEquals("12345678", state.items[0].dni)
        assertEquals(PendingSubscriptionStatus.CONFLICT, state.items[0].status)
        assertEquals(1_700_000_000_000L, state.items[0].createdAt)
    }

    @Test
    fun `sync invokes SyncPendingSubscriptionsUseCase and clears syncing flag`() = runTest(testDispatcher) {
        coEvery { syncPendingSubscriptionsUseCase() } returns Result.success(
            PendingSubscriptionSyncResult(syncedCount = 1)
        )
        viewModel.onIntent(PendingSubscriptionsIntent.Load)
        advanceUntilIdle()

        viewModel.onIntent(PendingSubscriptionsIntent.Sync)
        advanceUntilIdle()

        coVerify(exactly = 1) { syncPendingSubscriptionsUseCase() }
        assertFalse(viewModel.uiState.value.isSyncing)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `sync success with items emits success snackbar event`() = runTest(testDispatcher) {
        coEvery { syncPendingSubscriptionsUseCase() } returns Result.success(
            PendingSubscriptionSyncResult(syncedCount = 2)
        )
        val events = mutableListOf<PendingSubscriptionsUiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.onIntent(PendingSubscriptionsIntent.Sync)
        advanceUntilIdle()

        assertEquals(
            PendingSubscriptionsUiEvent.ShowSuccessSnackbar(
                "Sincronización completada exitosamente (2 suscripciones enviadas)"
            ),
            events.single()
        )
        job.cancel()
    }

    @Test
    fun `sync with empty queue emits empty snackbar event`() = runTest(testDispatcher) {
        coEvery { syncPendingSubscriptionsUseCase() } returns Result.success(
            PendingSubscriptionSyncResult(syncedCount = 0)
        )
        val events = mutableListOf<PendingSubscriptionsUiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.onIntent(PendingSubscriptionsIntent.Sync)
        advanceUntilIdle()

        assertEquals(
            PendingSubscriptionsUiEvent.ShowSuccessSnackbar(
                "No hay suscripciones pendientes por sincronizar"
            ),
            events.single()
        )
        job.cancel()
    }

    @Test
    fun `partial sync keeps failedCount and lastError and still emits success`() = runTest(testDispatcher) {
        coEvery { syncPendingSubscriptionsUseCase() } returns Result.success(
            PendingSubscriptionSyncResult(
                syncedCount = 1,
                failedCount = 1,
                lastError = "timeout"
            )
        )
        val events = mutableListOf<PendingSubscriptionsUiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.onIntent(PendingSubscriptionsIntent.Sync)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.failedCount)
        assertEquals("timeout", viewModel.uiState.value.lastError)
        assertEquals("timeout", viewModel.uiState.value.errorMessage)
        assertEquals(
            PendingSubscriptionsUiEvent.ShowSuccessSnackbar(
                "Sincronización completada exitosamente (1 suscripciones enviadas)"
            ),
            events.single()
        )
        job.cancel()
    }

    @Test
    fun `ip conflict sync exposes actionable error message`() = runTest(testDispatcher) {
        val message = "IP ya en uso. Coordina otra IP con el equipo e intenta de nuevo."
        coEvery { syncPendingSubscriptionsUseCase() } returns Result.success(
            PendingSubscriptionSyncResult(
                syncedCount = 0,
                failedCount = 1,
                lastError = message
            )
        )

        viewModel.onIntent(PendingSubscriptionsIntent.Sync)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSyncing)
        assertEquals(1, viewModel.uiState.value.failedCount)
        assertEquals(message, viewModel.uiState.value.errorMessage)
        assertEquals(message, viewModel.uiState.value.lastError)
    }

    @Test
    fun `sync failure exposes error message`() = runTest(testDispatcher) {
        coEvery { syncPendingSubscriptionsUseCase() } returns Result.failure(NoConnectivity())
        viewModel.onIntent(PendingSubscriptionsIntent.Load)
        advanceUntilIdle()

        viewModel.onIntent(PendingSubscriptionsIntent.Sync)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSyncing)
        assertEquals(NoConnectivity().message, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `queued items read like the canvas`() = runTest(testDispatcher) {
        viewModel.onIntent(PendingSubscriptionsIntent.Load)
        advanceUntilIdle()
        pendingFlow.emit(
            listOf(
                PendingSubscription(
                    localId = "local-1", clientRequestId = "c1", createdAt = 1_700_000_000_000L,
                    subscriptionJson = """{"firstName":"HILDA","lastName":"CCORI MAMANI","dni":"45871236","installationType":"WIRELESS","ip":"10.0.0.5"}""",
                    status = PendingSubscriptionStatus.PENDING,
                ),
                PendingSubscription(
                    localId = "local-2", clientRequestId = "c2", createdAt = 1_700_000_000_000L,
                    subscriptionJson = """{"firstName":"Tomas","lastName":"Vega"}""",
                    status = PendingSubscriptionStatus.CONFLICT, lastError = "El DNI 45871236 ya tiene una suscripción activa.",
                ),
            )
        )
        advanceUntilIdle()

        val (waiting, failed) = viewModel.uiState.value.items
        assertEquals("Hilda Ccori Mamani", waiting.clientName)
        assertEquals("Inalámbrico · IP 10.0.0.5 · DNI 45871236", waiting.detail)
        assertEquals("Esperando señal", waiting.statusLabel)
        assertEquals("No se pudo enviar", failed.statusLabel)
        assertEquals("El DNI 45871236 ya tiene una suscripción activa.", failed.error)
    }

    @Test
    fun `discard removes the queued registration`() = runTest(testDispatcher) {
        coEvery { discardPendingSubscriptionUseCase("local-1") } returns Result.success(Unit)

        viewModel.onIntent(PendingSubscriptionsIntent.Discard("local-1"))
        advanceUntilIdle()

        coVerify { discardPendingSubscriptionUseCase("local-1") }
    }
}
