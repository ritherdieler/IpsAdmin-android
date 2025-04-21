package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.CustomOutlinedTextField
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutLinedDropDown
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume

/**
 * Card component displaying a subscription with expandable details.
 * 
 * @param subscriptionResume Data for the subscription to display
 * @param onMenuItemSelected Callback when a menu item is selected
 * @param onExpandChange Callback when the expanded state changes 
 * @param expanded Whether the card is expanded
 * @param customerFormData Form data for the customer, if this card is expanded
 * @param placesState State for places dropdown
 * @param saveState State for save operation
 * @param onFieldChange Callback when a field is changed
 * @param onPlaceSelected Callback when a place is selected
 * @param onUpdatePlace Callback to update place ID and name
 * @param onSaveClick Callback when save button is clicked
 * @param modifier Optional modifier for customizing the component
 */
@Composable
fun SubscriptionCard(
    subscriptionResume: SubscriptionResume,
    onMenuItemSelected: (SubscriptionMenu) -> Unit,
    onExpandChange: (SubscriptionResume, Boolean) -> Unit,
    expanded: Boolean,
    customerFormData: CustomerFormData? = null,
    placesState: PlacesState = PlacesState(),
    saveState: SaveSubscriptionState = SaveSubscriptionState.Success,
    onFieldChange: (String, String) -> Unit = { _, _ -> },
    onPlaceSelected: (Place) -> Unit = {},
    onUpdatePlace: (Int, String) -> Unit = { _, _ -> },
    onSaveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Calculate card background color based on service status
    val cardBackgroundColor = when (subscriptionResume.serviceStatus) {
        ServiceStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    
    val contentColor = when (subscriptionResume.serviceStatus) {
        ServiceStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CardHeader(
                subscription = subscriptionResume,
                onMenuItemSelected = onMenuItemSelected
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            CardBody(subscriptionResume = subscriptionResume)

            ExpandableCardFooter(
                expanded = expanded,
                onClick = { onExpandChange(subscriptionResume, !expanded) }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                // Show CustomerDataForm if this card is expanded and we have form data
                if (customerFormData != null && customerFormData.subscriptionId == subscriptionResume.id) {
                    CustomerDataForm(
                        formData = customerFormData,
                        placesState = placesState,
                        saveState = saveState,
                        onFieldChange = onFieldChange,
                        onPlaceSelected = onPlaceSelected,
                        onUpdatePlace = onUpdatePlace,
                        onSaveClick = onSaveClick
                    )
                } else {
                    // Show a loading placeholder while waiting for data
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        tonalElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp), // Approximate height to avoid layout shifts
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Form for editing customer data inside a subscription card
 */
@Composable
fun CustomerDataForm(
    formData: CustomerFormData,
    placesState: PlacesState,
    saveState: SaveSubscriptionState,
    onFieldChange: (String, String) -> Unit,
    onPlaceSelected: (Place) -> Unit,
    onUpdatePlace: (Int, String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            CustomerFormFields(
                formData = formData,
                placesState = placesState,
                onFieldChange = onFieldChange,
                onPlaceSelected = onPlaceSelected,
                onUpdatePlace = onUpdatePlace
            )

            Spacer(modifier = Modifier.height(16.dp))

            SaveButton(
                enabled = formData.isValid() && saveState !is SaveSubscriptionState.Loading,
                saveState = saveState,
                onSaveClick = onSaveClick
            )
        }
    }
}

@Composable
private fun CustomerFormFields(
    formData: CustomerFormData,
    placesState: PlacesState,
    onFieldChange: (String, String) -> Unit,
    onPlaceSelected: (Place) -> Unit,
    onUpdatePlace: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier.fillMaxWidth()) {
        val (
            name, lastName, phone, dni, address, email, place,
            nameError, lastNameError, phoneError, dniError, addressError, emailError
        ) = createRefs()

        // Name and Last Name row
        CustomOutlinedTextField(
            modifier = Modifier.constrainAs(name) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(lastName.start, margin = 8.dp)
                width = Dimension.fillToConstraints
            },
            value = formData.name,
            onValueChange = { onFieldChange("name", it) },
            label = "Nombre",
            isError = formData.nameError != null
        )
        
        if (formData.nameError != null) {
            Text(
                text = formData.nameError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.constrainAs(nameError) {
                    top.linkTo(name.bottom, margin = 2.dp)
                    start.linkTo(name.start, margin = 16.dp)
                }
            )
        }

        CustomOutlinedTextField(
            modifier = Modifier.constrainAs(lastName) {
                top.linkTo(name.top)
                start.linkTo(name.end, margin = 8.dp)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            value = formData.lastName,
            onValueChange = { onFieldChange("lastName", it) },
            label = "Apellido",
            isError = formData.lastNameError != null
        )
        
        if (formData.lastNameError != null) {
            Text(
                text = formData.lastNameError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.constrainAs(lastNameError) {
                    top.linkTo(lastName.bottom, margin = 2.dp)
                    start.linkTo(lastName.start, margin = 16.dp)
                }
            )
        }

        // Reference point for the next row, considering possible error messages
        val nameRowBottom = if (formData.nameError != null || formData.lastNameError != null) {
            if (formData.nameError != null && formData.lastNameError != null) {
                // Both have errors, use the bottom of both error texts
                nameError
            } else if (formData.nameError != null) {
                // Only name has error
                nameError
            } else {
                // Only lastName has error
                lastNameError
            }
        } else {
            // No errors, use bottom of fields
            name
        }

        // Phone and DNI row
        CustomOutlinedTextField(
            modifier = Modifier.constrainAs(phone) {
                top.linkTo(nameRowBottom.bottom, margin = 12.dp)
                start.linkTo(parent.start)
                end.linkTo(dni.start, margin = 8.dp)
                width = Dimension.fillToConstraints
            },
            value = formData.phone,
            onValueChange = { onFieldChange("phone", it) },
            label = "Teléfono",
            maxLength = 9,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = formData.phoneError != null
        )
        
        if (formData.phoneError != null) {
            Text(
                text = formData.phoneError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.constrainAs(phoneError) {
                    top.linkTo(phone.bottom, margin = 2.dp)
                    start.linkTo(phone.start, margin = 16.dp)
                }
            )
        }

        CustomOutlinedTextField(
            modifier = Modifier.constrainAs(dni) {
                top.linkTo(phone.top)
                start.linkTo(phone.end, margin = 8.dp)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            value = formData.dni,
            onValueChange = { onFieldChange("dni", it) },
            label = "DNI",
            maxLength = 8,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = formData.dniError != null
        )
        
        if (formData.dniError != null) {
            Text(
                text = formData.dniError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.constrainAs(dniError) {
                    top.linkTo(dni.bottom, margin = 2.dp)
                    start.linkTo(dni.start, margin = 16.dp)
                }
            )
        }

        // Reference point for the next row
        val phoneRowBottom = if (formData.phoneError != null || formData.dniError != null) {
            if (formData.phoneError != null && formData.dniError != null) {
                phoneError
            } else if (formData.phoneError != null) {
                phoneError
            } else {
                dniError
            }
        } else {
            phone
        }

        // Place dropdown with loading states
        PlaceSelector(
            modifier = Modifier.constrainAs(place) {
                top.linkTo(phoneRowBottom.bottom, margin = 12.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            placesState = placesState,
            currentPlace = formData.place,
            currentPlaceId = formData.placeId,
            onPlaceSelected = onPlaceSelected,
            onUpdatePlace = onUpdatePlace,
            isError = formData.placeError != null
        )

        // Address and Email fields
        CustomOutlinedTextField(
            modifier = Modifier.constrainAs(address) {
                top.linkTo(place.bottom, margin = 12.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            value = formData.address,
            onValueChange = { onFieldChange("address", it) },
            label = "Dirección",
            isError = formData.addressError != null
        )
        
        if (formData.addressError != null) {
            Text(
                text = formData.addressError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.constrainAs(addressError) {
                    top.linkTo(address.bottom, margin = 2.dp)
                    start.linkTo(address.start, margin = 16.dp)
                }
            )
        }

        val addressRowBottom = if (formData.addressError != null) {
            addressError
        } else {
            address
        }

        CustomOutlinedTextField(
            modifier = Modifier.constrainAs(email) {
                top.linkTo(addressRowBottom.bottom, margin = 12.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            value = formData.email,
            onValueChange = { onFieldChange("email", it) },
            label = "Email",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = formData.emailError != null
        )
        
        if (formData.emailError != null) {
            Text(
                text = formData.emailError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.constrainAs(emailError) {
                    top.linkTo(email.bottom, margin = 2.dp)
                    start.linkTo(email.start, margin = 16.dp)
                }
            )
        }
    }
}

@Composable
private fun PlaceSelector(
    placesState: PlacesState,
    currentPlace: String,
    currentPlaceId: Int,
    onPlaceSelected: (Place) -> Unit,
    onUpdatePlace: (Int, String) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    when {
        placesState.places.isNotEmpty() -> {
            // If there's no explicit selection in placesState but we have currentPlaceId,
            // find the matching place to display
            val selectedPlace = placesState.selectedPlace ?: if (currentPlaceId > 0) {
                placesState.places.find { place -> 
                    place.id?.toIntOrNull() == currentPlaceId 
                }
            } else null
            
            MyOutLinedDropDown(
                modifier = modifier,
                items = placesState.places,
                selected = selectedPlace,
                label = "Lugar",
                onItemSelected = { selectedPlace ->
                    onPlaceSelected(selectedPlace)
                    val id = selectedPlace.id
                    val name = selectedPlace.name
                    if (id != null && name != null) {
                        onUpdatePlace(id.toInt(), name)
                    }
                },
                hasError = isError
            )
        }
        placesState.isLoading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        placesState.error != null -> {
            CustomOutlinedTextField(
                modifier = modifier,
                enabled = false,
                value = currentPlace,
                onValueChange = { },
                label = "Lugar",
                isError = isError
            )
        }
    }
}

/**
 * Footer component with animation for expanding/collapsing card details
 */
@Composable
fun ExpandableCardFooter(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationDegree by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = if (expanded) "Collapse" else "Expand",
            modifier = Modifier
                .size(32.dp)
                .rotate(rotationDegree),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun CardBody(subscriptionResume: SubscriptionResume, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Status indicator and service type
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicator de tipo de servicio (Fibra o Inalámbrico)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .padding(end = 8.dp),
                color = when (subscriptionResume.installationType) {
                    InstallationType.FIBER -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    InstallationType.WIRELESS -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = when (subscriptionResume.installationType) {
                        InstallationType.FIBER -> "Fibra"
                        InstallationType.WIRELESS -> "Inalámbrico"
                        InstallationType.ONLY_TV_FIBER -> "Solo cable"
                        else -> "Otro"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when (subscriptionResume.installationType) {
                        InstallationType.FIBER -> MaterialTheme.colorScheme.tertiary
                        InstallationType.WIRELESS -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Indicador de estado del servicio
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                color = when (subscriptionResume.serviceStatus) {
                    ServiceStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    ServiceStatus.CUT_OFF -> MaterialTheme.colorScheme.errorContainer
                    ServiceStatus.SUSPENDED -> MaterialTheme.colorScheme.tertiaryContainer
                    ServiceStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = when (subscriptionResume.serviceStatus) {
                        ServiceStatus.ACTIVE -> "Activo"
                        ServiceStatus.CUT_OFF -> "Cortado"
                        ServiceStatus.SUSPENDED -> "Suspendido"
                        ServiceStatus.CANCELLED -> "Cancelado"
                        else -> "Desconocido"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when (subscriptionResume.serviceStatus) {
                        ServiceStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                        ServiceStatus.CUT_OFF -> MaterialTheme.colorScheme.onErrorContainer
                        ServiceStatus.SUSPENDED -> MaterialTheme.colorScheme.onTertiaryContainer
                        ServiceStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        
        // Deuda destacada cuando existe
        if (subscriptionResume.totalDebt > 0) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Deuda pendiente",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Monto de deuda
                        Text(
                            text = "S/. ${String.format("%.2f", subscriptionResume.totalDebt)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Cantidad de facturas
                        Text(
                            text = "${subscriptionResume.pendingInvoicesQuantity} facturas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        
        // Información principal en dos columnas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (subscriptionResume.totalDebt > 0) 4.dp else 12.dp)
        ) {
            // Primera columna: Información del plan
            Column(modifier = Modifier.weight(0.6f)) {
                // Plan con estilo destacado
                Text(
                    text = "Plan",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subscriptionResume.planName.capitalize(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Otros datos en formato más compacto
                SubscriptionInfoItem(
                    label = "Antigüedad",
                    value = "${subscriptionResume.antiquity} meses",
                    alignment = Alignment.CenterStart
                )
                SubscriptionInfoItem(
                    label = "Lugar",
                    value = subscriptionResume.placeName.capitalize(),
                    alignment = Alignment.CenterStart
                )
                SubscriptionInfoItem(
                    label = "IP",
                    value = subscriptionResume.ipAddress,
                    isClickable = true,
                    onClick = { openInBrowser(subscriptionResume.ipAddress, context) },
                    alignment = Alignment.CenterStart
                )
                SubscriptionInfoItem(
                    label = "ICS",
                    value = subscriptionResume.ics,
                    alignment = Alignment.CenterStart
                )
            }
            
            // Segunda columna: Acciones y último pago
            Column(
                modifier = Modifier.weight(0.5f),
                horizontalAlignment = Alignment.End
            ) {
                // Último pago destacado
                Text(
                    text = "Último pago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
                Text(
                    text = subscriptionResume.lastPaymentDate ?: "Sin pagos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 8.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Acciones rápidas: WhatsApp y Mapa
                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .align(Alignment.End)
                ) {
                    // Map button en un contenedor material
                    Surface(
                        modifier = Modifier.clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        IconButton(onClick = { openMap(subscriptionResume, context) }) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Ver ubicación en mapa",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.size(8.dp))
                    
                    // WhatsApp button en un contenedor material
                    Surface(
                        modifier = Modifier.clip(CircleShape),
                        color = Color(0xFF25D366).copy(alpha = 0.1f),
                        contentColor = Color(0xFF25D366)
                    ) {
                        IconButton(onClick = { sendWhatsapp(subscriptionResume, context) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "Enviar mensaje por WhatsApp",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionInfoItem(
    label: String,
    value: String,
    isClickable: Boolean = false,
    onClick: () -> Unit = {},
    alignment: Alignment = Alignment.Center,
    modifier: Modifier = Modifier
) {
    val baseModifier = modifier
        .padding(vertical = 4.dp)
        .fillMaxWidth()
    
    val textModifier = if (isClickable) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    
    Box(
        modifier = baseModifier,
        contentAlignment = alignment
    ) {
        Row(
            modifier = textModifier
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isClickable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SaveButton(
    saveState: SaveSubscriptionState,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean
) {

    Button(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
        ),
        enabled = enabled,
        onClick = onSaveClick
    ) {
        if (saveState is SaveSubscriptionState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Guardar",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// Extension function to safely convert String to Int
private fun String.toIntOrNull(): Int? {
    return try {
        this.toInt()
    } catch (e: NumberFormatException) {
        null
    }
}

