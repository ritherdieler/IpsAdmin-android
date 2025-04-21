package com.dscorp.ispadmin.presentation.ui.features.supportTicket.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyAutoCompleteTextViewCompose
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyCustomDialog
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutLinedDropDown
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutlinedTextField
import com.dscorp.ispadmin.domain.model.PlaceResponse
import com.dscorp.ispadmin.domain.model.SubscriptionFastSearchResponse

@Composable
fun CreateSupportTicketScreen(
    uiState: CreateSupportTicketUiState,
    onPhoneChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onIsClientChange: (Boolean) -> Unit,
    onPlaceSelected: (PlaceResponse?) -> Unit,
    onSubscriptionSelected: (SubscriptionFastSearchResponse?) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCreateTicket: () -> Unit,
    onDismissError: () -> Unit,
    categories: List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Información de Contacto",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Campo de teléfono con icono
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Teléfono",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )

                MyOutlinedTextField(
                    value = uiState.phone,
                    onValueChange = { if (it.length <= 9) onPhoneChange(it) },
                    label = "Teléfono de contacto",
                    modifier = Modifier.fillMaxWidth(),
                    hasError = uiState.phoneError != null,
                    errorMessage = uiState.phoneError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Detalles del Problema",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Campo de categoría con icono
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Category,
                    contentDescription = "Categoría",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Añadir texto de error debajo del dropdown si es necesario
                Column(modifier = Modifier.fillMaxWidth()) {
                    MyOutLinedDropDown(
                        items = categories,
                        selected = uiState.category.takeIf { it.isNotEmpty() },
                        onItemSelected = { selectedCategory ->
                            onCategoryChange(selectedCategory ?: "")
                        },
                        label = "Selecciona una categoría",
                        hasError = uiState.categoryError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.categoryError != null) {
                        Text(
                            text = uiState.categoryError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de descripción con icono
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = "Descripción",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp, top = 8.dp)
                )

                MyOutlinedTextField(
                    value = uiState.description,
                    onValueChange = {
                        if (it.length <= 300) onDescriptionChange(
                            it
                        )
                    },
                    label = "Descripción",
                    modifier = Modifier.fillMaxWidth(),
                    hasError = uiState.descriptionError != null,
                    errorMessage = uiState.descriptionError,
                    singleLine = false,
                    maxLines = 5
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tipo de Cliente",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Selector cliente/no cliente con mejor diseño
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onIsClientChange(true) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.isClient,
                            onClick = { onIsClientChange(true) }
                        )
                        Text(
                            text = stringResource(R.string.es_cliente),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp, end = 24.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onIsClientChange(false) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !uiState.isClient,
                            onClick = { onIsClientChange(false) }
                        )
                        Text(
                            text = stringResource(R.string.no_es_cliente),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección específica según tipo de cliente
            if (uiState.isClient) {
                Text(
                    text = "Datos del Cliente",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Cliente",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Componente para buscar clientes
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MyAutoCompleteTextViewCompose(
                            items = uiState.subscriptions,
                            label = "Cliente",
                            selectedItem = uiState.selectedSubscription,
                            onItemSelected = { subscription ->
                                onSubscriptionSelected(subscription)
                            },
                            onSelectionCleared = {
                                onSubscriptionSelected(null)
                            },
                            onTextChanged = { text ->
                                onSearchTextChange(text)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            hasError = uiState.subscriptionError != null
                        )

                        if (uiState.subscriptionError != null) {
                            Text(
                                text = uiState.subscriptionError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Ubicación",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Selector de lugares si no es cliente
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Ubicación",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        MyOutLinedDropDown(
                            items = uiState.places,
                            selected = uiState.selectedPlace,
                            onItemSelected = { place ->
                                onPlaceSelected(place)
                            },
                            label = "Selecciona un lugar",
                            hasError = uiState.placeError != null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.placeError != null) {
                            Text(
                                text = uiState.placeError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Campo de nombre completo del cliente cuando no es cliente
                Text(
                    text = "Datos Personales",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Nombre",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    MyOutlinedTextField(
                        value = uiState.customerName,
                        onValueChange = onCustomerNameChange,
                        label = "Nombre completo",
                        modifier = Modifier.fillMaxWidth(),
                        hasError = uiState.customerNameError != null,
                        errorMessage = uiState.customerNameError,
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de registro con estilo destacado
            MyButton(
                onClick = { onCreateTicket() },
                text = "Registrar Ticket",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Mostrar diálogo de error si es necesario
        if (uiState.error != null) {
            MyCustomDialog(
                onDismissRequest = { onDismissError() },
                content = {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.error),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        MyButton(
                            onClick = { onDismissError() },
                            text = stringResource(R.string.ok),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateSupportTicketScreenPreview() {
    MyTheme {
        CreateSupportTicketScreen(
            uiState = CreateSupportTicketUiState(
                phone = "987654321",
                category = "Internet",
                description = "Problema con la conexión",
                isClient = true,
                selectedSubscription = SubscriptionFastSearchResponse(
                    id = 1,
                    fullName = "Juan Pérez",
                ),
                subscriptions = listOf(
                    SubscriptionFastSearchResponse(
                        id = 1,
                        fullName = "Juan Pérez",
                    ),
                    SubscriptionFastSearchResponse(
                        id = 2,
                        fullName = "María García",
                    )
                ),
                places = emptyList(),
                selectedPlace = null,
                isLoading = false,
                error = null,
                phoneError = null,
                categoryError = null,
                descriptionError = null,
                subscriptionError = null,
                placeError = null,
                customerName = "",
                customerNameError = null,
                isTicketCreated = false
            ),
            onPhoneChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onIsClientChange = {},
            onPlaceSelected = {},
            onSubscriptionSelected = {},
            onSearchTextChange = {},
            onCustomerNameChange = {},
            onCreateTicket = {},
            onDismissError = {},
            categories = listOf("Internet", "Telefonía", "Cable TV", "Otros")
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateSupportTicketScreenWithErrorsPreview() {
    MyTheme {
        CreateSupportTicketScreen(
            uiState = CreateSupportTicketUiState(
                phone = "123",
                category = "",
                description = "Des",
                isClient = false,
                selectedSubscription = null,
                subscriptions = emptyList(),
                places = listOf(
                    PlaceResponse("1", "Lima"),
                    PlaceResponse("2", "Arequipa")
                ),
                selectedPlace = null,
                isLoading = false,
                error = null,
                phoneError = "El teléfono debe tener 9 dígitos",
                categoryError = "Seleccione una categoría",
                descriptionError = "La descripción es muy corta",
                subscriptionError = null,
                placeError = "Seleccione un lugar",
                customerName = "",
                customerNameError = "El nombre completo es obligatorio",
                isTicketCreated = false
            ),
            onPhoneChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onIsClientChange = {},
            onPlaceSelected = {},
            onSubscriptionSelected = {},
            onSearchTextChange = {},
            onCustomerNameChange = {},
            onCreateTicket = {},
            onDismissError = {},
            categories = listOf("Internet", "Telefonía", "Cable TV", "Otros")
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateSupportTicketScreenWithDialogPreview() {
    MyTheme {
        CreateSupportTicketScreen(
            uiState = CreateSupportTicketUiState(
                phone = "987654321",
                category = "Internet",
                description = "Problema con la conexión",
                isClient = true,
                selectedSubscription = null,
                subscriptions = emptyList(),
                places = emptyList(),
                selectedPlace = null,
                isLoading = false,
                error = "Ha ocurrido un error al crear el ticket",
                phoneError = null,
                categoryError = null,
                descriptionError = null,
                subscriptionError = null,
                placeError = null,
                customerName = "",
                customerNameError = null,
                isTicketCreated = false
            ),
            onPhoneChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onIsClientChange = {},
            onPlaceSelected = {},
            onSubscriptionSelected = {},
            onSearchTextChange = {},
            onCustomerNameChange = {},
            onCreateTicket = {},
            onDismissError = {},
            categories = listOf("Internet", "Telefonía", "Cable TV", "Otros")
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateSupportTicketScreenNonClientPreview() {
    MyTheme {
        CreateSupportTicketScreen(
            uiState = CreateSupportTicketUiState(
                phone = "987654321",
                category = "Internet",
                description = "Problema con la conexión en mi negocio",
                isClient = false,
                selectedSubscription = null,
                subscriptions = emptyList(),
                places = listOf(
                    PlaceResponse("1", "Lima"),
                    PlaceResponse("2", "Arequipa")
                ),
                selectedPlace = PlaceResponse("1", "Lima"),
                isLoading = false,
                error = null,
                phoneError = null,
                categoryError = null,
                descriptionError = null,
                subscriptionError = null,
                placeError = null,
                customerName = "Pedro Sánchez",
                customerNameError = null,
                isTicketCreated = false
            ),
            onPhoneChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onIsClientChange = {},
            onPlaceSelected = {},
            onSubscriptionSelected = {},
            onSearchTextChange = {},
            onCustomerNameChange = {},
            onCreateTicket = {},
            onDismissError = {},
            categories = listOf("Internet", "Telefonía", "Cable TV", "Otros")
        )
    }
} 