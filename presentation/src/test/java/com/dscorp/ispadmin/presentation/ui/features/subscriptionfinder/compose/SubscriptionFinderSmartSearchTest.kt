package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.PagedResult
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.usecase.subscription.SearchSubscriptionsUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class InMemoryRecents(var items: List<RecentSubscription> = emptyList()) : RecentSubscriptionsStore {
    override fun load() = items
    override fun save(items: List<RecentSubscription>) { this.items = items }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionFinderSmartSearchTest {

    private val dispatcher = StandardTestDispatcher()
    private val search = mockk<SearchSubscriptionsUseCase>()
    private val recents = InMemoryRecents()
    private lateinit var viewModel: SubscriptionFinderViewModel

    private fun page(vararg ids: Int, total: Long = ids.size.toLong(), totalPages: Int = 1, page: Int = 0) =
        Result.success(PagedResult(items = ids.map { row(id = it) }, page = page, size = 20, total = total, totalPages = totalPages))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { search.invoke("", "CUT_OFF", 0, 1) } returns page(total = 490)
        coEvery { search.invoke("", "CANCELLED", 0, 1) } returns page(total = 449)
        viewModel = SubscriptionFinderViewModel(
            repository = mockk(relaxed = true),
            reactivateServiceUseCase = mockk(),
            rebootFiberOnuUseCase = mockk(),
            searchSubscriptionsUseCase = search,
            observabilityClient = mockk(relaxed = true),
            retryTr069ProvisioningUseCase = mockk(),
            recentSubscriptionsStore = recents,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `quick accesses show real totals from the Core`() = runTest(dispatcher) {
        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(
            listOf(QuickAccess(ServiceStatus.CUT_OFF, "Cortados", 490), QuickAccess(ServiceStatus.CANCELLED, "Cancelados", 449)),
            viewModel.uiState.value.quickAccess,
        )
    }

    @Test
    fun `opening a quick access lists that state page by page`() = runTest(dispatcher) {
        coEvery { search.invoke("", "CUT_OFF", 0, 20) } returns page(1, 2, total = 40, totalPages = 2)
        coEvery { search.invoke("", "CUT_OFF", 1, 20) } returns page(3, total = 40, totalPages = 2, page = 1)
        viewModel.initialize()
        advanceUntilIdle()

        viewModel.openQuickAccess(ServiceStatus.CUT_OFF)
        advanceUntilIdle()
        assertEquals(listOf(1, 2), viewModel.uiState.value.results.map { it.id })
        assertTrue(viewModel.uiState.value.canLoadMore)

        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3), viewModel.uiState.value.results.map { it.id })
        coVerify(exactly = 1) { search.invoke("", "CUT_OFF", 1, 20) }
    }

    @Test
    fun `chips narrow the visible results`() = runTest(dispatcher) {
        coEvery { search.invoke("RAM", null, 0, 20) } returns Result.success(
            PagedResult(
                items = listOf(
                    row(id = 1, debt = 100.0),
                    row(id = 2, debt = 0.0, status = ServiceStatus.ACTIVE, type = InstallationType.WIRELESS),
                ),
                page = 0, size = 20, total = 2, totalPages = 1,
            )
        )
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.onSmartQueryChanged("ram")
        advanceUntilIdle()

        viewModel.onResultFilterSelected(ResultFilter.WITH_DEBT)
        assertEquals(listOf(1), viewModel.uiState.value.results.map { it.id })

        viewModel.onResultFilterSelected(ResultFilter.WIRELESS)
        assertEquals(listOf(2), viewModel.uiState.value.results.map { it.id })
        assertEquals(ResultFilter.WIRELESS, viewModel.uiState.value.resultFilter)
    }

    @Test
    fun `clearing the box goes back to the start screen`() = runTest(dispatcher) {
        coEvery { search.invoke("RAM", null, 0, 20) } returns page(1)
        viewModel.initialize()
        advanceUntilIdle()
        viewModel.onSmartQueryChanged("ram")
        advanceUntilIdle()

        viewModel.onSmartQueryChanged("")
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.smartQuery)
        assertTrue(viewModel.uiState.value.results.isEmpty())
        assertTrue(viewModel.uiState.value.showStart)
    }

    @Test
    fun `opened subscriptions become recents newest first without duplicates`() = runTest(dispatcher) {
        recents.items = listOf(RecentSubscription(99, "Ana Paredes", "Basico · La Villa", ServiceStatus.ACTIVE))
        viewModel.initialize()
        advanceUntilIdle()
        assertEquals(listOf(99), viewModel.uiState.value.recents.map { it.id })

        (1..6).forEach { viewModel.onSubscriptionOpened(row(id = it)) }
        viewModel.onSubscriptionOpened(row(id = 3))

        assertEquals(listOf(3, 6, 5, 4, 2), viewModel.uiState.value.recents.map { it.id })
        assertEquals(RecentSubscription(3, "Jorge Ramos Villanueva", "Basico · La Villa", ServiceStatus.CUT_OFF), recents.items.first())

        viewModel.clearRecents()
        assertTrue(viewModel.uiState.value.recents.isEmpty())
        assertTrue(recents.items.isEmpty())
    }
}
