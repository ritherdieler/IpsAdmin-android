package com.dscorp.ispadmin.presentation.ui.features.payment.detail

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.DetailField
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyCustomDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    viewModel: PaymentDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Contenido principal cuando hay datos
                    uiState.payment?.let { payment ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
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
                            
                            Divider(
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
                                        value = payment.method ?: ""
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
                    
                    // Mostrar progreso de carga
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    // Mostrar error si hay
                    uiState.error?.let { error ->
                        MyCustomDialog(
                            onDismissRequest = { viewModel.clearError() },
                            content = {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Error",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    MyButton(
                                        onClick = { viewModel.clearError() },
                                        text = "Aceptar",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
} 