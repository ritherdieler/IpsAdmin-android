package com.dscorp.ispadmin.presentation.ui.features.subscription.provisioning

import com.dscorp.ispadmin.domain.model.ProvisioningAction
import com.dscorp.ispadmin.domain.model.ProvisioningOperation
import com.dscorp.ispadmin.domain.model.ProvisioningProgress
import com.dscorp.ispadmin.domain.repository.ProvisioningRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProvisioningViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ProvisioningRepository>()
    private val failed = ProvisioningProgress(
        operation = ProvisioningOperation(id = "op", subscriptionId = 42, revision = 3, state = "FAILED"),
        canRetry = true, canCancel = true,
    )
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `leaving during an action allows progress to reload on return`() = runTest(dispatcher) {
        coEvery { repository.latest(42) } returns Result.success(failed)
        coEvery { repository.retry(any()) } coAnswers { kotlinx.coroutines.delay(10_000); Result.success(failed) }
        val vm = ProvisioningViewModel(repository = repository, mainImmediate = dispatcher)
        vm.onIntent(ProvisioningIntent.Load(42))
        runCurrent()
        vm.onIntent(ProvisioningIntent.Retry)
        runCurrent()
        assertTrue(vm.uiState.value.isSubmitting)
        vm.onIntent(ProvisioningIntent.Stop)
        vm.onIntent(ProvisioningIntent.Load(42))
        runCurrent()
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals("op", vm.uiState.value.progress?.operation?.id)
    }

    @Test fun `retry uses server revision and prevents duplicate taps`() = runTest(dispatcher) {
        coEvery { repository.latest(42) } returns Result.success(failed)
        coEvery { repository.retry(any()) } returns Result.success(failed.copy(canRetry = false))
        val vm = ProvisioningViewModel(repository = repository, mainImmediate = dispatcher)
        vm.onIntent(ProvisioningIntent.Load(42))
        runCurrent()
        assertEquals("op", vm.uiState.value.progress?.operation?.id)
        vm.onIntent(ProvisioningIntent.Retry)
        vm.onIntent(ProvisioningIntent.Retry)
        runCurrent()
        coVerify(exactly = 1) { repository.retry(ProvisioningAction(42, "op", 3)) }
    }

    @Test fun `cancellation requires confirmation and uses cancellation endpoint`() = runTest(dispatcher) {
        coEvery { repository.latest(42) } returns Result.success(failed)
        coEvery { repository.cancel(any()) } returns Result.success(failed.copy(
            operation = failed.operation.copy(state = "CANCELLED"), canRetry = false, canCancel = false, canStartAgain = true))
        val vm = ProvisioningViewModel(repository = repository, mainImmediate = dispatcher)
        vm.onIntent(ProvisioningIntent.Load(42))
        runCurrent()
        vm.onIntent(ProvisioningIntent.AskCancel)
        assertTrue(vm.uiState.value.confirmCancel)
        coVerify(exactly = 0) { repository.cancel(any()) }
        vm.onIntent(ProvisioningIntent.ConfirmCancel)
        runCurrent()
        assertTrue(vm.uiState.value.progress?.canStartAgain == true)
        coVerify(exactly = 1) { repository.cancel(ProvisioningAction(42, "op", 3)) }
    }
}
