package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Money
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dscorp.components.components.loaders.Loader
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.extensions.toFormattedDateString
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.CleanDetailField
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.ErrorView
import org.koin.androidx.compose.koinViewModel

@Composable
fun SubscriptionDetailScreen(
    subscriptionId: Int,
    viewModel: SubscriptionDetailViewModel = koinViewModel(),
    navController: NavController
) {

    LaunchedEffect(Unit) {
        viewModel.getSubscription(subscriptionId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyTheme {
        when {
            uiState.isLoading ->Loader()

            uiState.error != null -> {
                ErrorView(
                    errorMessage = uiState.error!!,
                    onRetry = { viewModel.getSubscription(subscriptionId) },
                    onBack = {
                        viewModel.clearError()
                        navController.popBackStack()
                    }
                )
            }

            uiState.subscription != null -> SubscriptionDetailForm(uiState.subscription!!)
        }
    }
}

@Composable
fun SubscriptionDetailForm(subscription: SubscriptionResponse) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = when (subscription.serviceStatus) {
                ServiceStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subscription.getFullName().uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (subscription.serviceStatus) {
                        ServiceStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subscription.serviceStatus.getFormattedStatus(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = when (subscription.serviceStatus) {
                        ServiceStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Información del Cliente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            subscription.dni?.let {
                CleanDetailField(
                    icon = Icons.Rounded.AccountCircle,
                    label = "DNI",
                    value = it
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            subscription.phone?.let {
                CleanDetailField(
                    icon = Icons.Rounded.Phone,
                    label = "Teléfono",
                    value = it
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            subscription.email?.let {
                if (it.isNotEmpty()) {
                    CleanDetailField(
                        icon = Icons.Rounded.Email,
                        label = "Email",
                        value = it
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            subscription.address?.let {
                CleanDetailField(
                    icon = Icons.Rounded.Home,
                    label = "Dirección",
                    value = it
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            subscription.phone?.let { phoneNumber ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$phoneNumber")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = "Llamar",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Llamar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            try {
                                val cleanNumber =
                                    phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/51$cleanNumber")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "No se pudo abrir WhatsApp",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Chat,
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "WhatsApp",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Información del Servicio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            subscription.plan?.let {
                val speedInfo = if (it.downloadSpeed != null) {
                    "${it.downloadSpeed} / ${it.uploadSpeed ?: "N/A"}"
                } else {
                    "N/A"
                }
                CleanDetailField(
                    icon = Icons.Rounded.Wifi,
                    label = "Plan",
                    value = "${it.name} - $speedInfo"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            CleanDetailField(
                icon = Icons.Rounded.SignalCellularAlt,
                label = "Tipo de Instalación",
                value = when (subscription.installationType) {
                    InstallationType.FIBER -> "Fibra Óptica"
                    InstallationType.WIRELESS -> "Inalámbrico"
                    else -> "Solo TV Fibra"
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            subscription.price?.let {
                CleanDetailField(
                    icon = Icons.Rounded.Money,
                    label = "Precio Mensual",
                    value = "S/ %.2f".format(it)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            subscription.subscriptionDate?.let {
                CleanDetailField(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "Fecha de Suscripción",
                    value = it.toFormattedDateString()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            subscription.place?.let {
                CleanDetailField(
                    icon = Icons.Rounded.LocationOn,
                    label = "Lugar",
                    value = it.name ?: "N/A"
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Información de Red",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            subscription.ip?.let {
                if (it.isNotEmpty()) {
                    CleanDetailField(
                        icon = Icons.Rounded.Router,
                        label = "Dirección IP",
                        value = it
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            subscription.napBox?.let {
                CleanDetailField(
                    icon = Icons.Rounded.Router,
                    label = "NAP Box",
                    value = it.code.ifEmpty { "N/A" }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            subscription.hostDevice?.let {
                CleanDetailField(
                    icon = Icons.Rounded.Router,
                    label = "Dispositivo Host",
                    value = it.name
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            subscription.password?.let {
                if (it.isNotEmpty()) {
                    CleanDetailField(
                        icon = Icons.Rounded.Password,
                        label = "Contraseña",
                        value = it
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Información Financiera",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Facturas Pendientes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${subscription.pendingInvoiceQuantity}",
                        style = MaterialTheme.typography.displaySmall,
                        color = if (subscription.pendingInvoiceQuantity > 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Deuda Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "S/ %.2f".format(subscription.totalDebt),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (subscription.totalDebt > 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Antigüedad",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${subscription.antiquityInMonths}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "meses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${subscription.ics}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            subscription.lastPaymentDate?.let {
                if (it.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    CleanDetailField(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "Último Pago",
                        value = it
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        subscription.technician?.let {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Información del Técnico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                CleanDetailField(
                    icon = Icons.Rounded.Person,
                    label = "Técnico Asignado",
                    value = "${it.name} ${it.lastName}"
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        subscription.note?.let {
            if (it.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "Notas Adicionales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    CleanDetailField(
                        icon = Icons.Rounded.Info,
                        label = "Notas",
                        value = it
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SubscriptionDetailFormPreview() {
    val mockSubscription = SubscriptionResponse(
        id = 1,
        firstName = "Juan Carlos",
        lastName = "Pérez García",
        dni = "12345678",
        phone = "987654321",
        email = "juan.perez@email.com",
        address = "Av. Principal 123, Urbanización Los Jardines, Lima",
        serviceStatus = ServiceStatus.ACTIVE,
        plan = PlanResponse(
            id = "1",
            name = "Plan Premium",
            price = 120.0,
            downloadSpeed = "100 Mbps",
            uploadSpeed = "50 Mbps",
            type = InstallationType.FIBER
        ),
        installationType = InstallationType.FIBER,
        price = 120.0,
        subscriptionDate = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000),
        place = Place(
            id = "1",
            name = "Lima Centro",
            latitude = -12.0464f,
            longitude = -77.0428f
        ),
        ip = "192.168.1.100",
        napBox = NapBoxResponse(
            id = "1",
            code = "NAP-001-A",
            address = "Esquina Av. Principal con Calle 5",
            mufaId = 1,
            latitude = -12.0464,
            longitude = -77.0428,
            ports_number = 8,
            placeName = "Lima Centro",
            placeId = 1
        ),
        hostDevice = NetworkDevice(
            id = 1,
            name = "Router TP-Link AC1200",
            username = "admin",
            password = "admin123",
            ipAddress = "192.168.1.1",
            networkDeviceType = NetworkDevice.NetworkDeviceType.FIBER_ROUTER
        ),
        password = "wifi2024secure",
        technician = User(
            id = 1,
            name = "Carlos",
            lastName = "Rodriguez",
            type = User.UserType.TECHNICIAN,
            username = "carlos.tech",
            verified = true,
            dni = "87654321",
            email = "carlos@isp.com",
            phone = "912345678"
        ),
        pendingInvoiceQuantity = 2,
        totalDebt = 240.0,
        antiquityInMonths = 6,
        qualification = 4.5,
        ics = 85,
        lastPaymentDate = "15/10/2024",
        note = "Cliente preferencial. Solicita factura electrónica mensualmente.",
        new = false,
        isPaymentCommitment = false,
        isMigration = false,
        isReactivation = false,
        paymentCommitmentDate = null,
        lastCutOffDate = null,
        reactivationDate = null,
        cpeDeviceId = null,
        location = null,
        networkDevices = emptyList()
    )

    MyTheme {
        SubscriptionDetailForm(subscription = mockSubscription)
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Servicio Suspendido")
@Composable
fun SubscriptionDetailFormSuspendedPreview() {
    val mockSubscription = SubscriptionResponse(
        id = 2,
        firstName = "María Elena",
        lastName = "Torres Vega",
        dni = "98765432",
        phone = "965432198",
        email = "maria.torres@email.com",
        address = "Jr. Las Flores 456, San Juan de Lurigancho",
        serviceStatus = ServiceStatus.CANCELLED,
        plan = PlanResponse(
            id = "2",
            name = "Plan Básico",
            price = 60.0,
            downloadSpeed = "50 Mbps",
            uploadSpeed = "25 Mbps",
            type = InstallationType.FIBER
        ),
        installationType = InstallationType.FIBER,
        price = 60.0,
        subscriptionDate = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000),
        place = Place(
            id = "2",
            name = "San Juan de Lurigancho",
            latitude = -11.9946f,
            longitude = -76.9988f
        ),
        ip = "192.168.2.50",
        napBox = null,
        hostDevice = null,
        password = "wifibasic2024",
        technician = null,
        pendingInvoiceQuantity = 5,
        totalDebt = 300.0,
        antiquityInMonths = 3,
        qualification = 3.0,
        ics = 45,
        lastPaymentDate = "20/08/2024",
        note = null,
        new = false,
        isPaymentCommitment = true,
        isMigration = false,
        isReactivation = false,
        paymentCommitmentDate = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
        lastCutOffDate = null,
        reactivationDate = null,
        cpeDeviceId = null,
        location = null,
        networkDevices = emptyList()
    )

    MyTheme {
        SubscriptionDetailForm(subscription = mockSubscription)
    }
}
