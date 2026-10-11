package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.financial.FinancialScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashBoardComposeScreen(navController: NavHostController) {
    var commercial by remember { mutableStateOf(false) }
    MyTheme {
        if (commercial) {
            val viewModel: DashBoardViewModel = koinViewModel()
            val state by viewModel.state.collectAsState()
            Column {
                TextButton(onClick = { commercial = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.financial_return))
                }
                DashboardScreen(state, { viewModel.getDashBoardData() }, showFinancials = false)
            }
        } else {
            FinancialScreen(onOperational = { commercial = true })
        }
    }
}
