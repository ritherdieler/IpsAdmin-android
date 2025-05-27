package com.dscorp.ispadmin.presentation.ui.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dscorp.ispadmin.navigation.NavRoutes
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Dashboard
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Installation
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Payment
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Profile
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Subscription
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.SupportTicket
import kotlinx.coroutines.launch

/**
 * Contenido del drawer de navegación
 */
@Composable
fun MenuDrawerContent(
    navController: NavHostController,
    onItemClicked: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    
    var selectedRoute by remember { mutableStateOf(navController.currentDestination?.route ?: NavRoutes.FeatureRoutes.Profile) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp)
    ) {
        // Encabezado
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Perfil",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "IPS Admin",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Elementos del menú
        MenuItem(
            icon = Icons.Default.AccountCircle,
            title = "Mi Perfil",
            isSelected = selectedRoute == Profile,
            onClick = {
                scope.launch {
                    selectedRoute = Profile
                    navController.navigate(Profile)
                    onItemClicked()
                }
            }
        )
        
        MenuItem(
            icon = Icons.Default.Dashboard,
            title = "Dashboard",
            isSelected = selectedRoute == Dashboard,
            onClick = {
                scope.launch {
                    selectedRoute = Dashboard
                    navController.navigate(Dashboard)
                    onItemClicked()
                }
            }
        )
        
        // Sección de suscripciones
        DrawerSectionTitle(title = "Suscripciones")
        
        MenuItem(
            icon = Icons.Default.Receipt,
            title = "Registrar suscripción",
            isSelected = selectedRoute == Subscription.Register,
            onClick = {
                scope.launch {
                    selectedRoute = Subscription.Register
                    navController.navigate(Subscription.Register)
                    onItemClicked()
                }
            }
        )
        
        MenuItem(
            icon = Icons.Default.Search,
            title = "Buscar suscripción",
            isSelected = selectedRoute == Subscription.Find,
            onClick = {
                scope.launch {
                    selectedRoute = Subscription.Find
                    navController.navigate(Subscription.Find)
                    onItemClicked()
                }
            }
        )
        
        // Sección de pagos
        DrawerSectionTitle(title = "Pagos")
        
        MenuItem(
            icon = Icons.Default.Payment,
            title = "Buscar pagador",
            isSelected = selectedRoute == Payment.FindPayer,
            onClick = {
                scope.launch {
                    selectedRoute = Payment.FindPayer
                    navController.navigate(Payment.FindPayer)
                    onItemClicked()
                }
            }
        )

        // Sección de tickets de soporte
        DrawerSectionTitle(title = "Tickets de Soporte")
        
        MenuItem(
            icon = Icons.Default.Support,
            title = "Crear ticket",
            isSelected = selectedRoute == SupportTicket.Create,
            onClick = {
                scope.launch {
                    selectedRoute = SupportTicket.Create
                    navController.navigate(SupportTicket.Create)
                    onItemClicked()
                }
            }
        )
        
        MenuItem(
            icon = Icons.Default.Support,
            title = "Ver tickets",
            isSelected = selectedRoute == SupportTicket.List,
            onClick = {
                scope.launch {
                    selectedRoute = SupportTicket.List
                    navController.navigate(SupportTicket.List)
                    onItemClicked()
                }
            }
        )

        // Sección de órdenes de instalación
        DrawerSectionTitle(title = "Órdenes de Instalación")
        
        MenuItem(
            icon = Icons.Default.Build,
            title = "Lista de órdenes",
            isSelected = selectedRoute == Installation.List,
            onClick = {
                scope.launch {
                    selectedRoute = Installation.List
                    navController.navigate(Installation.List)
                    onItemClicked()
                }
            }
        )
    }
}

@Composable
fun MenuItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}

@Composable
fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}
