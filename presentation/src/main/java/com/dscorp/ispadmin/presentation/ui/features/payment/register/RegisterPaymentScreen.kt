package com.dscorp.ispadmin.presentation.ui.features.payment.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Payments
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.MyAutoCompleteTextViewCompose
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.ispadmin.presentation.ui.components.MyCustomDialog
import com.dscorp.ispadmin.presentation.ui.components.MyOutLinedDropDown
import com.dscorp.components.components.formfields.MyOutlinedTextField
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla de registro de pago en Compose
 * Esta versión puede recibir tanto un Payment completo como un paymentId
 */
@Composable
fun RegisterPaymentScreen(
    navController: NavHostController,
    paymentId: Int,
    viewModel: RegisterPaymentViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Cargar el pago usando el ID
    LaunchedEffect(key1 = paymentId) {
        viewModel.onEvent(RegisterPaymentEvent.LoadPaymentData(paymentId))
    }
    
    if (state.isSuccess) {
        SuccessDialog(onDismiss = { navController.popBackStack() })
    }
    
    RegisterPaymentScreenContent(
        state = state,
        onNavigateBack = { navController.popBackStack() },
        onPaymentMethodSelected = { selectedMethod ->
            viewModel.onEvent(RegisterPaymentEvent.PaymentMethodSelected(selectedMethod))
        },
        onElectronicPayerNameChanged = { payer ->
            viewModel.onEvent(RegisterPaymentEvent.ElectronicPayerNameChanged(payer))
        },
        onElectronicPayerNameCleared = {
            viewModel.onEvent(RegisterPaymentEvent.ElectronicPayerNameChanged(null))
        },
        onToggleDiscountFields = {
            viewModel.onEvent(RegisterPaymentEvent.ToggleDiscountFields)
        },
        onDiscountAmountChanged = { amount ->
            viewModel.onEvent(RegisterPaymentEvent.DiscountAmountChanged(amount))
        },
        onDiscountReasonChanged = { reason ->
            viewModel.onEvent(RegisterPaymentEvent.DiscountReasonChanged(reason))
        },
        onRegisterPayment = {
            viewModel.onEvent(RegisterPaymentEvent.RegisterPayment)
        }
    )
}

/**
 * Versión alternativa que recibe directamente un objeto Payment
 * Mantiene compatibilidad con código existente
 */
@Composable
fun RegisterPaymentScreen(
    payment: Payment,
    onNavigateBack: () -> Unit,
    viewModel: RegisterPaymentViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(payment) {
        viewModel.onEvent(RegisterPaymentEvent.SetPayment(payment))
    }
    
    if (state.isSuccess) {
        SuccessDialog(onDismiss = onNavigateBack)
    }
    
    RegisterPaymentScreenContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onPaymentMethodSelected = { selectedMethod ->
            viewModel.onEvent(RegisterPaymentEvent.PaymentMethodSelected(selectedMethod))
        },
        onElectronicPayerNameChanged = { payer ->
            viewModel.onEvent(RegisterPaymentEvent.ElectronicPayerNameChanged(payer))
        },
        onElectronicPayerNameCleared = {
            viewModel.onEvent(RegisterPaymentEvent.ElectronicPayerNameChanged(null))
        },
        onToggleDiscountFields = {
            viewModel.onEvent(RegisterPaymentEvent.ToggleDiscountFields)
        },
        onDiscountAmountChanged = { amount ->
            viewModel.onEvent(RegisterPaymentEvent.DiscountAmountChanged(amount))
        },
        onDiscountReasonChanged = { reason ->
            viewModel.onEvent(RegisterPaymentEvent.DiscountReasonChanged(reason))
        },
        onRegisterPayment = {
            viewModel.onEvent(RegisterPaymentEvent.RegisterPayment)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterPaymentScreenContent(
    state: RegisterPaymentState,
    onNavigateBack: () -> Unit,
    onPaymentMethodSelected: (String) -> Unit,
    onElectronicPayerNameChanged: (String) -> Unit,
    onElectronicPayerNameCleared: () -> Unit,
    onToggleDiscountFields: () -> Unit,
    onDiscountAmountChanged: (String) -> Unit,
    onDiscountReasonChanged: (String) -> Unit,
    onRegisterPayment: () -> Unit
) {
    MyTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "Registrar Pago") },
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
                RegisterPaymentForm(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onPaymentMethodSelected = onPaymentMethodSelected,
                    onElectronicPayerNameChanged = onElectronicPayerNameChanged,
                    onElectronicPayerNameCleared = onElectronicPayerNameCleared,
                    onToggleDiscountFields = onToggleDiscountFields,
                    onDiscountAmountChanged = onDiscountAmountChanged,
                    onDiscountReasonChanged = onDiscountReasonChanged,
                    onRegisterPayment = onRegisterPayment
                )
            }
        }
    }
}

@Composable
fun RegisterPaymentForm(
    state: RegisterPaymentState,
    modifier: Modifier = Modifier,
    onPaymentMethodSelected: (String) -> Unit,
    onElectronicPayerNameChanged: (String) -> Unit,
    onElectronicPayerNameCleared: () -> Unit,
    onToggleDiscountFields: () -> Unit,
    onDiscountAmountChanged: (String) -> Unit,
    onDiscountReasonChanged: (String) -> Unit,
    onRegisterPayment: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Debt information card
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachMoney,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Deuda a pagar",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Text(
                    text = state.payment?.amountToPayStr() ?: "0.00",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Text(
            text = "Método de pago",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Payment Method Dropdown
        val paymentMethods = listOf("Efectivo", "Tarjeta", "Yape", "Plin", "Transferencia", "Depósito")
        MyOutLinedDropDown(
            modifier = Modifier.fillMaxWidth(),
            items = paymentMethods,
            selected = state.paymentMethod,
            label = "Seleccionar método",
            onItemSelected = onPaymentMethodSelected,
            hasError = state.errorMessages.containsKey("paymentMethod")
        )
        
        if (state.errorMessages.containsKey("paymentMethod")) {
            Text(
                text = state.errorMessages["paymentMethod"] ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Show Electronic Payer Name field only if payment method is Yape or Plin
        AnimatedVisibility(visible = state.paymentMethod.isNotEmpty() && (state.paymentMethod == "Yape" || state.paymentMethod == "Plin")) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Datos del pagador",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Electronic Payer Name AutoComplete
                MyAutoCompleteTextViewCompose(
                    modifier = Modifier.fillMaxWidth(),
                    items = state.electronicPayers,
                    selectedItem = state.electronicPayerName.takeIf { it.isNotEmpty() },
                    label = "Introduce o selecciona un nombre",
                    onItemSelected = onElectronicPayerNameChanged,
                    onSelectionCleared = onElectronicPayerNameCleared,
                    errorMessage = state.errorMessages["electronicPayerName"],
                    hasError = state.errorMessages.containsKey("electronicPayerName"),
                    onTextChanged = onElectronicPayerNameChanged
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Discount section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Discount,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column {
                            Text(
                                text = "Aplicar descuento",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Active esta opción si necesita aplicar un descuento",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Switch(
                        checked = state.showDiscountFields,
                        onCheckedChange = { onToggleDiscountFields() }
                    )
                }
                
                // Show discount fields if necessary
                AnimatedVisibility(visible = state.showDiscountFields) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Discount Amount Field - Asegurando que el error se muestre correctamente
                        MyOutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.discountAmount,
                            label = "Monto de descuento",
                            errorMessage = state.errorMessages["discountAmount"],
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            onValueChange = onDiscountAmountChanged,
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val discountReasons = listOf("Fallas de internet", "Fallas de TVcable", "Error de facturación")
                        MyAutoCompleteTextViewCompose(
                            modifier = Modifier.fillMaxWidth(),
                            items = discountReasons,
                            selectedItem = state.discountReason.takeIf { it.isNotEmpty() },
                            label = "Razón del descuento",
                            onItemSelected = onDiscountReasonChanged,
                            onSelectionCleared = { onDiscountReasonChanged("") },
                            onTextChanged = onDiscountReasonChanged,
                            errorMessage = state.errorMessages["discountReason"],
                            hasError = state.errorMessages.containsKey("discountReason")
                        )
                        
                        if (state.errorMessages.containsKey("discountReason")) {
                            Text(
                                text = state.errorMessages["discountReason"] ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        
        // Show general error if any
        if (state.errorMessages.containsKey("general")) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = state.errorMessages["general"] ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))
        
        // Register Payment Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Confirmar pago",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )
                
                MyButton(
                    text = "Registrar",
                    isLoading = state.isLoading,
                    onClick = onRegisterPayment
                )
            }
        }
    }
}

@Composable
private fun SuccessDialog(onDismiss: () -> Unit) {
    MyCustomDialog(
        onDismissRequest = onDismiss,
        cancelable = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Pago registrado",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "El pago ha sido registrado exitosamente.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            MyButton(
                text = "Aceptar",
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterPaymentFormPreview() {
    val mockState = RegisterPaymentState(
        payment = Payment().apply {
            amountToPay = 100.0
        },
        paymentMethod = "Tarjeta",
        electronicPayerName = "Juan Pérez",
        electronicPayers = listOf("Juan Pérez", "María López", "Pedro González"),
        showDiscountFields = true,
        discountAmount = "20.0",
        discountReason = "Fallas de internet",
        errorMessages = mapOf(),
        isLoading = false,
        isSuccess = false
    )
    
    MyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RegisterPaymentForm(
                state = mockState,
                onPaymentMethodSelected = {},
                onElectronicPayerNameChanged = {},
                onElectronicPayerNameCleared = {},
                onToggleDiscountFields = {},
                onDiscountAmountChanged = {},
                onDiscountReasonChanged = {},
                onRegisterPayment = {}
            )
        }
    }
}
