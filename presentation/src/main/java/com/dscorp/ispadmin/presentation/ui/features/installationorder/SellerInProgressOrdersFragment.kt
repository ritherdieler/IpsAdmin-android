package com.dscorp.ispadmin.presentation.ui.features.installationorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.dscorp.ispadmin.presentation.theme.MyTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class SellerInProgressOrdersFragment : Fragment() {

    private val viewModel: SellerInProgressOrdersViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MyTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    LaunchedEffect(Unit) {
                        viewModel.loadInProgressOrders()
                    }
                    SellerInProgressOrdersScreen(
                        uiState = uiState,
                        onOrderSelected = { /* Implementar navegación a detalles si es necesario */ },
                        onErrorDismissed = viewModel::dismissError,
                        onRefresh = viewModel::loadInProgressOrders,
                        onNavigateBack = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
    

} 