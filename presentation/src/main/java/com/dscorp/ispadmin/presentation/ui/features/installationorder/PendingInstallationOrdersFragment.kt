package com.dscorp.ispadmin.presentation.ui.features.installationorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.dscorp.ispadmin.presentation.theme.MyTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class PendingInstallationOrdersFragment : Fragment() {

    private val viewModel: PendingInstallationOrdersViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MyTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    
                    PendingInstallationOrdersScreen(
                        uiState = uiState,
                        onOrderSelected = viewModel::onOrderSelected,
                        onTechnicianSelected = viewModel::onTechnicianSelected,
                        onScheduledDateSelected = viewModel::onScheduledDateSelected,
                        onAssignTechnician = viewModel::assignTechnician,
                        onCloseDialog = viewModel::onCloseDialog,
                        onErrorDismissed = viewModel::dismissError,
                        onSuccessDismissed = viewModel::dismissSuccess,
                        onRefresh = viewModel::loadPendingOrders,
                        onNavigateBack = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadPendingOrders()
    }
} 