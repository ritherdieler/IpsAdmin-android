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
import androidx.navigation.fragment.navArgs
import com.dscorp.ispadmin.presentation.theme.MyTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class InstallationOrderFragment : Fragment() {

    private val viewModel: InstallationOrderViewModel by viewModel()
    private val args: InstallationOrderFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MyTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    
                    val screen = args.screen
                    val orderId = args.orderId
                    
                    when (screen) {
                        "create" -> CreateInstallationOrderScreen(
                            uiState = uiState,
                            onCreateOrder = viewModel::createInstallationOrder,
                            onErrorDismissed = viewModel::clearError,
                            onSuccessDismissed = viewModel::clearSuccessMessage,
                            onOrderCreationHandled = viewModel::clearOrderCreated,
                            onNavigateBack = { findNavController().popBackStack() }
                        )
                        "assign" -> AssignTechnicianScreen(
                            uiState = uiState,
                            orderId = orderId,
                            onAssignTechnician = { techId, assignedById, date ->
                                viewModel.assignTechnicianToOrder(orderId, techId, assignedById, date)
                            },
                            onErrorDismissed = viewModel::clearError,
                            onSuccessDismissed = viewModel::clearSuccessMessage,
                            onOrderUpdateHandled = viewModel::clearOrderUpdated,
                            onNavigateBack = { findNavController().popBackStack() }
                        )
                        "close" -> CloseInstallationOrderScreen(
                            uiState = uiState,
                            orderId = orderId,
                            onCloseOrder = { viewModel.closeInstallationOrder(orderId) },
                            onErrorDismissed = viewModel::clearError,
                            onSuccessDismissed = viewModel::clearSuccessMessage,
                            onOrderUpdateHandled = viewModel::clearOrderUpdated,
                            onNavigateBack = { findNavController().popBackStack() }
                        )
                        "cancel" -> CancelInstallationOrderScreen(
                            uiState = uiState,
                            orderId = orderId,
                            onCancelOrder = { reason -> viewModel.cancelInstallationOrder(orderId, reason) },
                            onErrorDismissed = viewModel::clearError,
                            onSuccessDismissed = viewModel::clearSuccessMessage,
                            onOrderUpdateHandled = viewModel::clearOrderUpdated,
                            onNavigateBack = { findNavController().popBackStack() }
                        )
                    }
                }
            }
        }
    }
} 