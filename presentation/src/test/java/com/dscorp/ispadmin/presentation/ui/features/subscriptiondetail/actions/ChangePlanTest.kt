package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.datasource.remote.PlanCountResponse
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.subscription
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val BASICO = PlanCountResponse(1, "Basico", 50.0, 200, 200, InstallationType.FIBER, true, 813)
private val DUO = PlanCountResponse(2, "Duo basico 80", 80.0, 200, 200, InstallationType.FIBER, true, 119)
private val I70 = PlanCountResponse(3, "Internet70", 70.0, 300, 300, InstallationType.FIBER, true, 36)
private val OLD = PlanCountResponse(4, "Viejo", 40.0, 100, 100, InstallationType.FIBER, false, 2)
private val RADIO = PlanCountResponse(5, "Basico wireless 50", 50.0, 20, 20, InstallationType.WIRELESS, true, 300)
private val TV = PlanCountResponse(6, "Cable basico", 30.0, 0, 0, InstallationType.ONLY_TV_FIBER, true, 40)
private val PLANS = listOf(I70, BASICO, OLD, RADIO, DUO, TV)

private fun onBasico() = subscription().apply {
    plan = PlanResponse("1", "Basico", 50.0, "200", "200", InstallationType.FIBER)
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1400dp")
class ChangePlanTest {
    @get:Rule val compose = createComposeRule()
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<ChangePlanGateway>()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `only active plans of the same type, most used first`() {
        val model = ChangePlan.from(onBasico(), PLANS)

        assertEquals(CurrentPlan("Basico", "Fibra · 200/200 Mbps", "S/ 50.00"), model.current)
        assertEquals("Planes de fibra activos (3)", model.title)
        assertEquals(listOf("Basico", "Duo basico 80", "Internet70"), model.options.map { it.name })
        assertEquals(PlanOption(1, "Basico", "S/ 50.00", "200/200 Mbps · 813 abonados", current = true), model.options[0])
        assertNull(model.change(1))
        assertEquals(PlanChange("S/ 80.00", "+S/ 30.00", "Cambiar a Duo basico 80"), model.change(2))
        assertEquals("+S/ 20.00", model.change(3)?.delta)
        assertEquals("Cambiar a inalámbrico o Solo TV es una migración", model.note)
        val radio = ChangePlan.from(onBasico().apply { installationType = InstallationType.WIRELESS }, PLANS)
        assertEquals("Planes inalámbricos activos (1)", radio.title)
        assertEquals("Pasar a fibra es una migración", radio.note)
    }

    @Test
    fun `cheaper plans read as a negative difference`() {
        val onDuo = onBasico().apply { plan = PlanResponse("2", "Duo basico 80", 80.0, "200", "200", InstallationType.FIBER) }

        assertEquals("−S/ 30.00", ChangePlan.from(onDuo, PLANS).change(1)?.delta)
    }

    @Test
    fun `saving changes the plan and reports it`() = runTest(dispatcher) {
        coEvery { gateway.subscription(1842) } returns onBasico()
        coEvery { gateway.plans() } returns PLANS
        coEvery { gateway.changePlan(1842, 2) } returns Unit
        val viewModel = ChangePlanViewModel(gateway, dispatcher)

        viewModel.onIntent(ChangePlanIntent.Load(1842))
        advanceUntilIdle()
        viewModel.onIntent(ChangePlanIntent.Select(2))
        viewModel.onIntent(ChangePlanIntent.Submit)
        advanceUntilIdle()

        coVerify { gateway.changePlan(1842, 2) }
        assertEquals("Plan cambiado a Duo basico 80", viewModel.uiState.value.done)
    }

    @Test
    fun `a failed change keeps the choice and a readable error`() = runTest(dispatcher) {
        coEvery { gateway.subscription(1842) } returns onBasico()
        coEvery { gateway.plans() } returns PLANS
        coEvery { gateway.changePlan(any(), any()) } throws IllegalStateException("HTTP 500")
        val viewModel = ChangePlanViewModel(gateway, dispatcher)

        viewModel.onIntent(ChangePlanIntent.Load(1842))
        advanceUntilIdle()
        viewModel.onIntent(ChangePlanIntent.Select(2))
        viewModel.onIntent(ChangePlanIntent.Submit)
        advanceUntilIdle()

        assertEquals("No se pudo cambiar el plan. Revisa tu conexión y vuelve a intentar.", viewModel.uiState.value.error)
        assertEquals(2, viewModel.uiState.value.selectedId)
        assertNull(viewModel.uiState.value.done)
    }

    @Test
    fun `the screen shows the new fee and asks before nothing changes`() {
        val intents = mutableListOf<ChangePlanIntent>()
        val model = ChangePlan.from(onBasico(), PLANS)
        compose.setContent {
            MaterialTheme { ChangePlanContent(ChangePlanUiState(model = model, selectedId = 2), onIntent = { intents += it }, onBack = {}) }
        }

        compose.onNodeWithText("Plan actual").assertExists()
        compose.onNodeWithText("Cambiar a inalámbrico o Solo TV es una migración").assertExists()
        compose.onNodeWithText("+S/ 30.00").assertExists()
        compose.onNodeWithText("Cambiar a Duo basico 80").performClick()
        compose.onNodeWithText("Internet70").performClick()

        assertEquals(listOf(ChangePlanIntent.Submit, ChangePlanIntent.Select(3)), intents)
    }

    @Test
    fun `with the current plan chosen there is nothing to save`() {
        compose.setContent {
            MaterialTheme {
                ChangePlanContent(ChangePlanUiState(model = ChangePlan.from(onBasico(), PLANS), selectedId = 1), onIntent = {}, onBack = {})
            }
        }

        compose.onNodeWithTag(ChangePlanTestTags.SUBMIT).assertIsNotEnabled()
        compose.onNodeWithText("Elige el plan nuevo").assertExists()
    }
}
