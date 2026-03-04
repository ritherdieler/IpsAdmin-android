package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.CancellationResumeDto
import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import com.dscorp.ispadmin.domain.model.EconomicResume
import com.dscorp.ispadmin.domain.model.SubscriptionsResumeStatics
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.Loader
import com.dscorp.ispadmin.presentation.ui.features.dashboard.components.BarChartContainer
import com.dscorp.ispadmin.presentation.ui.features.dashboard.components.CancellationChartContainer
import com.dscorp.ispadmin.presentation.ui.features.dashboard.components.LineChartContainer
import com.dscorp.ispadmin.presentation.ui.features.dashboard.components.MigrationChartContainer
import com.dscorp.ispadmin.presentation.ui.features.dashboard.components.PieChartContainer
import com.dscorp.ispadmin.presentation.ui.features.dashboard.components.ReconnectionChartContainer
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    uiState: DashboardState,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading && uiState.dashboardData == null) {
                Loader()
            } else {
                uiState.dashboardData?.let { data ->
                    DashboardContent(data, onRefresh)
                }
            }
        }
    }
}

@Composable
fun DashboardContent(data: DashBoardDataResponse, onRefresh: () -> Unit) {
    val scrollState = rememberScrollState()
    
    // Animación secuencial para los elementos
    var visibleSections by remember { mutableStateOf(0) }
    val maxSections = 11
    
    LaunchedEffect(key1 = true) {
        repeat(maxSections) {
            delay(120)  // Tiempo reducido entre animaciones para mayor fluidez
            visibleSections = it + 1
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera con título principal
        AnimatedVisibility(
            visible = visibleSections >= 1,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Text(
                text = "Panel de Control",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )
        }
        
        // Resumen monetario mensual
        AnimatedVisibility(
            visible = visibleSections >= 2,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Column {
                SectionTitle(
                    title = stringResource(R.string.monthly_monetary_resume),
                    icon = R.drawable.ic_monetization
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Gráfico circular
                PieChartContainer(data = data)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Tabla de resumen económico
        AnimatedVisibility(
            visible = visibleSections >= 3,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Column {
                EconomicResumeTable(data = data)
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
        
        // Resumen de clientes mensual
        AnimatedVisibility(
            visible = visibleSections >= 4,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Column {
                SectionTitle(
                    title = stringResource(R.string.monthly_costumers_resume),
                    icon = R.drawable.ic_people
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tabla de clientes
                CustomersResumeTable(data = data)
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
        
        // Ingresos brutos por mes
        AnimatedVisibility(
            visible = visibleSections >= 5,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Column {
                SectionTitle(
                    title = stringResource(R.string.gross_revenue_per_month),
                    icon = R.drawable.ic_chart
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Gráfico lineal de ingresos brutos
                LineChartContainer(
                    title = stringResource(R.string.gross_revenue_per_month),
                    data = data.grossRevenueHistoryStatics.sortedBy { it.billingDate }
                )
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
        
        // Historial de clientes activos
        AnimatedVisibility(
            visible = visibleSections >= 6,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Column {
                SectionTitle(
                    title = stringResource(R.string.active_customers_history),
                    icon = R.drawable.ic_history
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Gráfico lineal de clientes activos
                LineChartContainer(
                    title = stringResource(R.string.active_customers_history),
                    data = data.subscriptionsHistoryStatics
                )
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
        
        // Historial de pagos electrónicos
        AnimatedVisibility(
            visible = visibleSections >= 7,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Column {
                SectionTitle(
                    title = stringResource(R.string.electronic_payments_history),
                    icon = R.drawable.ic_payment
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Gráfico de barras de métodos de pago
                BarChartContainer(
                    title = stringResource(R.string.electronic_payments_history),
                    data = data.paymentResume
                )
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
        
        // Historial de cobros mensuales
        AnimatedVisibility(
            visible = visibleSections >= 8,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Column {
                SectionTitle(
                    title = stringResource(R.string.monthly_collects_history),
                    icon = R.drawable.ic_calendar
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Gráfico de barras de cobros mensuales
                BarChartContainer(
                    title = stringResource(R.string.monthly_collects_history),
                    data = data.monthlyCollects,
                    stacked = true
                )
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
        
        // Evolución de cancelaciones
        AnimatedVisibility(
            visible = visibleSections >= 9,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Column {
                SectionTitle(
                    title = "Evolución de Cancelaciones",
                    icon = R.drawable.ic_cancel_user
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Gráfico de líneas de cancelaciones
                data.subscriptionLogSummary?.let { logSummary ->
                    CancellationChartContainer(data = logSummary)
                }
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
        
        // Evolución de reconexiones
        AnimatedVisibility(
            visible = visibleSections >= 10,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Column {
                SectionTitle(
                    title = "Evolución de Reconexiones",
                    icon = R.drawable.ic_rotate_right
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Gráfico de líneas de reconexiones
                data.subscriptionLogSummary?.let { logSummary ->
                    ReconnectionChartContainer(data = logSummary)
                }
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
        
        // Evolución de migraciones
        AnimatedVisibility(
            visible = visibleSections >= 11,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
        ) {
            Column {
                SectionTitle(
                    title = "Evolución de Migraciones",
                    icon = R.drawable.ic_fiber
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Gráfico de líneas de migraciones
                data.subscriptionLogSummary?.let { logSummary ->
                    MigrationChartContainer(data = logSummary)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: Int? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            icon?.let {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(30.dp)
                        .padding(end = 12.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
fun EconomicResumeTable(data: DashBoardDataResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezados
            TableRow(
                title = stringResource(R.string.concept),
                value = stringResource(R.string.pen_symbol),
                isHeader = true
            )
            
            // Filas de datos
            
            TableRow(
                title = stringResource(R.string.fixed_costs),
                value = data.fixedCostsAsString(),
                icon = R.drawable.ic_fixed_cost,
                valueColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
            
            TableRow(
                title = stringResource(R.string.variable_costs),
                value = data.variableCostsAsString(),
                icon = R.drawable.ic_variable_cost,
                valueColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
            
            TableRow(
                title = stringResource(R.string.utilities),
                value = data.marginAsString(),
                icon = R.drawable.ic_profit,
                valueColor = MaterialTheme.colorScheme.primary
            )
            
            TableRow(
                title = stringResource(R.string.freeCash),
                value = data.freeCashAsString(),
                icon = R.drawable.ic_free_cash,
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CustomersResumeTable(data: DashBoardDataResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezados
            TableRow(
                title = stringResource(R.string.concept),
                value = stringResource(R.string.quantity),
                isHeader = true
            )
            
            // Filas de datos
            TableRow(
                title = stringResource(R.string.cableTv),
                value = data.subscriptionsResume.cableTvInstallations.toString(),
                icon = R.drawable.ic_tv,
                isLight = true
            )
            
            TableRow(
                title = stringResource(R.string.Fiber),
                value = data.subscriptionsResume.fiberInternetInstallations.toString(),
                icon = R.drawable.ic_fiber
            )
            
            TableRow(
                title = stringResource(R.string.wireless),
                value = data.subscriptionsResume.wirelessInternetInstallations.toString(),
                icon = R.drawable.ic_wifi,
                isLight = true
            )
            
            TableRow(
                title = stringResource(R.string.cancelledByUser),
                value = data.cancellationsResume.cancelledByUsers.toString(),
                icon = R.drawable.ic_cancel_user,
                valueColor = MaterialTheme.colorScheme.error
            )
            
            TableRow(
                title = stringResource(R.string.cancelledBySystem),
                value = data.cancellationsResume.cancelledBySystem.toString(),
                icon = R.drawable.ic_cancel_system,
                valueColor = MaterialTheme.colorScheme.error,
                isLight = true
            )

            TableRow(
                title = stringResource(R.string.reconnections),
                value = data.reconnections.toString(),
                icon = R.drawable.ic_rotate_right,
                isLight = true
            )
        }
    }
}

@Composable
fun TableRow(
    title: String,
    value: String,
    isHeader: Boolean = false,
    isLight: Boolean = false,
    icon: Int? = null,
    valueColor: Color = Color.Unspecified
) {
    val backgroundColor = when {
        isHeader -> MaterialTheme.colorScheme.primary
        isLight -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
    }
    
    val textColor = if (isHeader) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val finalValueColor = if (valueColor != Color.Unspecified) valueColor else textColor
    
    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isHeader && icon != null) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                
                Text(
                    text = title,
                    style = if (isHeader) 
                        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    else 
                        MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
            
            Text(
                text = value,
                style = if (isHeader) 
                    MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                else 
                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = finalValueColor
            )
        }
    }
    
    if (!isHeader) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    val mockEconomicResume = EconomicResume(
        grossRevenue = 10000.0,
        totalRaised = 7500.0,
        totalDiscount = 500.0,
        totalToCollect = 2000.0,
        outLaysFromCurrentMonth = 1000.0,
        fixedCosts = 2000.0,
        margin = 4500.0,
        freeCash = 2500.0,
        corporateGrossRevenue = 8000.0
    )
    
    val mockSubscriptionsResume = SubscriptionsResumeStatics(
        cableTvInstallations = 5,
        fiberInternetInstallations = 15,
        wirelessInternetInstallations = 8
    )
    
    val mockCancellationsResume = CancellationResumeDto(
        cancelledByUsers = 2,
        cancelledBySystem = 1
    )
    
    MyTheme {
        DashboardScreen(
            uiState = DashboardState(
                isLoading = false,
                dashboardData = DashBoardDataResponse(
                    economicResume = mockEconomicResume,
                    activeSubscriptions = 694,
                    subscriptionsResume = mockSubscriptionsResume,
                    cancellationsResume = mockCancellationsResume,
                    paymentResume = mapOf("Efectivo" to 5000.0, "Yape" to 2500.0),
                    subscriptionsHistoryStatics = emptyList(),
                    monthlyCollects = emptyList(),
                    grossRevenueHistoryStatics = emptyList(),
                    reconnections = 4272,
                    assistanceTicketResume = null,
                    planAnalysisResume = null,
                    clientQualityResume = null,
                    fixedCostAnalysisResume = null,
                    installationOrdersResume = null,
                    geographicPerformanceResume = null,
                    teamPerformanceResume = null,
                    networkHealthResume = null,
                    clientLifecycleResume = null,
                    subscriptionLogSummary = mapOf(),
                    
                )
            ),
            onRefresh = {}
        )
    }
} 