package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.dscorp.ispadmin.presentation.theme.MyTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class RegisterSubscriptionComposeFragment : Fragment() {
    
    private val viewModel: RegisterSubscriptionComposeViewModel by viewModel()
    private val args: RegisterSubscriptionComposeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            val installationOrderId = args.installationOrderId
            setContent {
                MyTheme {
                    RegisterSubscriptionFormScreen(
                        installationOrderId = installationOrderId.takeIf { it > 0 },
                        viewModel = viewModel,
                        onSubscriptionRegisterSuccess = {
                            // Si hay un ID de orden de instalación, cerrar la orden
                            if (installationOrderId > 0) {
                                viewModel.closeInstallationOrder(installationOrderId)
                            }
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }


}