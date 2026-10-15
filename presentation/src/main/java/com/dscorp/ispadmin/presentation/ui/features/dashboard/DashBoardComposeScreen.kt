package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.dscorp.ispadmin.navigation.NavRoutes
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.designsystem.components.HeroTabs
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.dashboard.clients.ClientsScreen
import com.dscorp.ispadmin.presentation.ui.features.financial.FinancialScreen
import org.koin.androidx.compose.koinViewModel

private enum class DashboardTab(val title: Int) {
    FINANCIAL(R.string.dashboard_tab_financial),
    COMMERCIAL(R.string.dashboard_tab_commercial),
    CLIENTS(R.string.dashboard_tab_clients),
}

/** Data that the host loads once and shares with the tabs. */
data class DashboardTabData(
    val legacy: com.dscorp.ispadmin.domain.model.DashBoardDataResponse? = null,
    val collections: com.dscorp.ispadmin.domain.model.dashboard.CollectionSummary? = null,
)

/** Dashboard host: one hero band per tab, with the tab selector drawn inside it. */
@Composable
fun DashBoardComposeScreen(navController: NavHostController, onOpenDrawer: () -> Unit = {}) {
    val actions = DashboardActions(
        openDrawer = onOpenDrawer,
        openTickets = { navController.navigate(NavRoutes.FeatureRoutes.SupportTicket.List) { launchSingleTop = true } },
        openSubscriptions = { navController.navigate(NavRoutes.FeatureRoutes.Subscription.Find) { launchSingleTop = true } },
    )
    MyTheme { CompositionLocalProvider(LocalDashboardActions provides actions) {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        // The legacy endpoint feeds the commercial tab and the financial history charts.
        val commercialViewModel: DashBoardViewModel = koinViewModel()
        val commercial by commercialViewModel.state.collectAsStateWithLifecycle()
        val collectionsViewModel: CollectionsViewModel = koinViewModel()
        val collections by collectionsViewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(collectionsViewModel) { collectionsViewModel.onIntent(CollectionsIntent.Load) }
        val titles = DashboardTab.entries.map { stringResource(it.title) }
        val header: @Composable () -> Unit = { HeroTabs(titles, selected, { selected = it }) }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (DashboardTab.entries[selected]) {
                DashboardTab.FINANCIAL -> FinancialScreen(header = header, data = DashboardTabData(commercial.dashboardData, collections.summary))
                DashboardTab.COMMERCIAL -> CommercialContent(commercial, commercialViewModel::getDashBoardData, header)
                DashboardTab.CLIENTS -> ClientsScreen(header, collections.summary)
            }
        }
    } }
}
