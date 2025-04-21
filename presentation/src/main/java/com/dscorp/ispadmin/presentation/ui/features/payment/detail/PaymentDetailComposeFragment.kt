package com.dscorp.ispadmin.presentation.ui.features.payment.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.dscorp.ispadmin.domain.model.Payment

class PaymentDetailComposeFragment : Fragment() {
    
    private val args: PaymentDetailFragmentArgs by navArgs()
    private var payment: Payment? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        payment = args.payment
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                payment?.let { paymentData ->
                    PaymentDetailScreen(
                        payment = paymentData,
                        onNavigateBack = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
} 