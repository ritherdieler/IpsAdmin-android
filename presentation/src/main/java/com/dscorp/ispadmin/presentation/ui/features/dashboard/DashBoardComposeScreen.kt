package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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

/** Dashboard host: one hero band per tab, with the tab selector drawn inside it. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DashBoardComposeScreen(navController: NavHostController) {
    MyTheme {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        // The legacy endpoint feeds the commercial tab and the financial history charts.
        val commercialViewModel: DashBoardViewModel = koinViewModel()
        val commercial by commercialViewModel.state.collectAsStateWithLifecycle()
        val titles = DashboardTab.entries.map { stringResource(it.title) }
        val header: @Composable () -> Unit = { HeroTabs(titles, selected, { selected = it }) }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (DashboardTab.entries[selected]) {
                DashboardTab.FINANCIAL -> FinancialScreen(header = header, legacy = commercial.dashboardData)
                DashboardTab.COMMERCIAL -> CommercialContent(commercial, commercialViewModel::getDashBoardData, header)
                DashboardTab.CLIENTS -> ClientsScreen(header)
            }
        }
    }
}
