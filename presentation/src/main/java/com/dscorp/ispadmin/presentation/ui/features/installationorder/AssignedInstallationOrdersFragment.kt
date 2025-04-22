package com.dscorp.ispadmin.presentation.ui.features.installationorder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dscorp.ispadmin.presentation.theme.MyTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AssignedInstallationOrdersFragment : Fragment() {

    private val viewModel: AssignedInstallationOrdersViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {

                val uiState by viewModel.uiState.collectAsState()

                MyTheme {
                    AssignedInstallationOrdersScreen(
                        uiState = uiState,
                        onOrderSelected = { order ->
                            // Navegar a la pantalla de registro de suscripción con el ID de la orden
                            findNavController().navigate(
                                AssignedInstallationOrdersFragmentDirections
                                    .actionToRegisterSubscription().apply {
                                        installationOrderId = order.id
                                    }
                            )
                        },
                        onRefresh = {
                            viewModel.loadAssignedOrders()
                        },
                        onErrorDismissed = {
                            viewModel.dismissError()
                        },
                        onNavigateBack = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // No es necesario hacer nada aquí porque el estado ya se está pasando directamente a la UI
            }
        }
    }
} 