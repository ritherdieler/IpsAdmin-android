package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.accessmigration

import com.dscorp.ispadmin.domain.model.AccessMigrationProgress
import com.dscorp.ispadmin.domain.model.AccessMigrationStage
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.repository.AccessMigrationRepository
import com.dscorp.ispadmin.domain.usecase.subscription.PollAccessMigrationUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccessMigrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<AccessMigrationRepository>()
    private lateinit var viewModel: AccessMigrationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AccessMigrationViewModel(
            repository = repository,
            pollAccessMigrationUseCase = PollAccessMigrationUseCase(
                accessMigrationRepository = repository,
                delayMs = 1L,
                timeoutMs = 50L,
                clock = { testDispatcher.scheduler.currentTime },
            ),
            mainImmediate = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should show static IP when accessMode is STATIC_IP`() = runTest(testDispatcher) {
        viewModel.onIntent(
            AccessMigrationIntent.Bind(
                subscriptionId = 10,
                accessMode = "STATIC_IP",
                ip = "10.11.1.40",
                pppoeUsername = null,
                migrationStage = "ELIGIBLE",
                installationType = InstallationType.FIBER,
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Dirección IP", state.accessLabel)
        assertEquals("10.11.1.40", state.accessValue)
        assertTrue(state.canMigrate)
        assertEquals(AccessMigrationStage.ELIGIBLE, state.stage)
    }

    @Test
    fun `should show PPPoE username instead of empty IP when accessMode is PPPoE`() =
        runTest(testDispatcher) {
            viewModel.onIntent(
                AccessMigrationIntent.Bind(
                    subscriptionId = 11,
                    accessMode = "PPPOE_DYNAMIC",
                    ip = null,
                    pppoeUsername = "gf-11",
                    migrationStage = "DONE",
                    installationType = InstallationType.FIBER,
                )
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Usuario PPPoE", state.accessLabel)
            assertEquals("gf-11", state.accessValue)
            assertFalse(state.canMigrate)
            assertEquals(AccessMigrationStage.DONE, state.stage)
        }

    @Test
    fun `should launch migration and poll progress until DONE`() = runTest(testDispatcher) {
        viewModel.onIntent(
            AccessMigrationIntent.Bind(
                subscriptionId = 10,
                accessMode = "STATIC_IP",
                ip = "10.11.1.40",
                migrationStage = "ELIGIBLE",
                installationType = InstallationType.FIBER,
            )
        )
        advanceUntilIdle()

        coEvery { repository.startMigration(10) } returns progress(
            stage = AccessMigrationStage.OLT_READY,
        )
        coEvery { repository.getProgress(10) } returnsMany listOf(
            progress(stage = AccessMigrationStage.SECRET_READY),
            progress(stage = AccessMigrationStage.CPE_APPLIED),
            progress(
                stage = AccessMigrationStage.DONE,
                done = true,
                pppoeUsername = "gf-10",
            ),
        )

        viewModel.onIntent(AccessMigrationIntent.StartMigration)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.startMigration(10) }
        coVerify(atLeast = 1) { repository.getProgress(10) }
        val state = viewModel.uiState.value
        assertFalse(state.isMigrating)
        assertEquals(AccessMigrationStage.DONE, state.stage)
        assertEquals("Usuario PPPoE", state.accessLabel)
        assertEquals("gf-10", state.accessValue)
        assertFalse(state.canMigrate)
        assertNull(state.errorMessage)
    }

    @Test
    fun `should expose FAILED_STRANDED error and stop polling`() = runTest(testDispatcher) {
        viewModel.onIntent(
            AccessMigrationIntent.Bind(
                subscriptionId = 22,
                accessMode = "STATIC_IP",
                ip = "10.11.1.55",
                migrationStage = "ELIGIBLE",
                installationType = InstallationType.FIBER,
            )
        )
        advanceUntilIdle()

        coEvery { repository.startMigration(22) } returns progress(
            subscriptionId = 22,
            stage = AccessMigrationStage.CPE_APPLIED,
        )
        coEvery { repository.getProgress(22) } returns progress(
            subscriptionId = 22,
            stage = AccessMigrationStage.FAILED_STRANDED,
            failureReason = "CPE no responde Inform",
            done = true,
        )

        viewModel.onIntent(AccessMigrationIntent.StartMigration)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isMigrating)
        assertEquals(AccessMigrationStage.FAILED_STRANDED, state.stage)
        assertEquals("CPE no responde Inform", state.errorMessage)
        assertEquals("CPE no responde Inform", state.failureReason)
        assertTrue(state.canMigrate.not())
    }

    private fun progress(
        subscriptionId: Int = 10,
        stage: AccessMigrationStage,
        done: Boolean = false,
        pppoeUsername: String? = null,
        failureReason: String? = null,
    ) = AccessMigrationProgress(
        subscriptionId = subscriptionId,
        stage = stage,
        failureReason = failureReason,
        pppoeUsername = pppoeUsername,
        done = done,
    )
}
