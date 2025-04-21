package com.dscorp.ispadmin.presentation.ui.features.payment.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Discount
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.DetailField
import com.dscorp.ispadmin.domain.model.Payment

class PaymentDetailFragment : Fragment() {

    private val args: PaymentDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PaymentDetailScreen(
                    payment = args.payment,
                    onNavigateBack = { findNavController().navigateUp() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    payment: Payment,
    onNavigateBack: () -> Unit
) {
    MyTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "Detalles del Pago") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with payment status
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = payment.amountToPayStr(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = payment.paidStatusStr(),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (payment.paid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // Payment Details
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Billing Date
                            DetailField(
                                icon = Icons.Rounded.CalendarMonth,
                                label = "Fecha de facturación",
                                value = payment.billingDateStr()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Payment Date
                            DetailField(
                                icon = Icons.Rounded.CalendarMonth,
                                label = "Fecha de pago",
                                value = payment.detailPaymentDateStr()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Payment Method
                            DetailField(
                                icon = Icons.Rounded.CreditCard,
                                label = "Método de pago",
                                value = payment.method?:""
                            )
                            
                            // Show electronic payer for YAPE payments
                            if (payment.method.equals("YAPE", ignoreCase = true) && !payment.electronicPayerName.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Electronic Payer
                                DetailField(
                                    icon = Icons.Rounded.Person,
                                    label = "Pagador electrónico",
                                    value = payment.electronicPayerName ?: ""
                                )
                            }
                            
                            // Only show discount if it exists
                            if (!payment.discountAmountStr().isNullOrEmpty() && payment.discountAmountStr() != "") {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Discount Amount
                                DetailField(
                                    icon = Icons.Rounded.AttachMoney,
                                    label = "Monto descontado",
                                    value = payment.discountAmountStr()
                                )
                                
                                // Only show discount reason if it exists
                                if (!payment.discountReason.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Discount Reason
                                    DetailField(
                                        icon = Icons.Rounded.Discount,
                                        label = "Razón del descuento",
                                        value = payment.discountReason ?: ""
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 