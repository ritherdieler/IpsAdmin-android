package com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder.compose

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.dscorp.ispadmin.presentation.ui.features.payment.register.RegisterPaymentEvent
import com.dscorp.ispadmin.presentation.ui.features.payment.register.RegisterPaymentViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PayerFinderScreen(
    modifier: Modifier = Modifier,
    paymentViewModel: RegisterPaymentViewModel = koinViewModel()
) {
    val state by paymentViewModel.state.collectAsState()
    rememberCoroutineScope()

    Scaffold { paddingValues ->
        PayerFinder(
            results = state.electronicPayers,
            onTextChanged = { searchText ->
                paymentViewModel.onEvent(RegisterPaymentEvent.ElectronicPayerNameChanged(searchText))
            }
        )
    }
}