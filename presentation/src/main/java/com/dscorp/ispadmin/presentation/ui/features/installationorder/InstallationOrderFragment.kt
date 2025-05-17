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
                    args.orderId
                    
                    when (screen) {
                        "create" -> CreateInstallationOrderScreen(
                            uiState = uiState,
                            onCreateOrderClicked = viewModel::createOrder,
                            onFirstNameChange = viewModel::onFirstNameChange,
                            onLastNameChange = viewModel::onLastNameChange,
                            onAddressChange = viewModel::onAddressChange,
                            onPhoneChange = viewModel::onPhoneChange,
                            onDniChange = viewModel::onDniChange,
                            onPlaceChange = viewModel::onPlaceChange,
                            onErrorDismissed = viewModel::dismissError,
                            onSuccessDismissed = viewModel::dismissSuccess,
                            onOrderCreationHandled = { /* No es necesario hacer nada aquí */ },
                            onNavigateBack = { findNavController().popBackStack() }
                        )
                        "assign" -> {}
                        "close" -> {}
                        "cancel" -> {}
                    }
                }
            }
        }
    }
}