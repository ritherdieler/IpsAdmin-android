package com.dscorp.ispadmin.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Support
import androidx.compose.ui.graphics.vector.ImageVector
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes

sealed class DrawerItem(
    val title: String,
    val route: Any,
    val icon: ImageVector
) {
    object Profile : DrawerItem("Mi Perfil", FeatureRoutes.Profile, Icons.Default.AccountCircle)
    object InstallationOrders :DrawerItem("Órdenes de Instalación", FeatureRoutes.Installation.List, Icons.Default.Build)

    object SupportTicketsList :DrawerItem("Tickets de Soporte", FeatureRoutes.SupportTicket.List, Icons.Default.Support)

    object CreateSupportTicket :DrawerItem("Creat ticket", FeatureRoutes.SupportTicket.Create, Icons.Default.Support)

    object SubscriptionFinder : DrawerItem("Buscador de suscripciones",FeatureRoutes.Subscription.Find,Icons.Default.Receipt)

    object RegisterSubscription : DrawerItem(  "Registrar suscripcion", FeatureRoutes.Subscription.Register(),Icons.Default.Receipt )

    object PayerFinder : DrawerItem("Buscador de pagadores", FeatureRoutes.Payment.FindPayer, Icons.Default.Receipt)

}

object DrawerNavigation {

    fun getDrawerItemsForUser(userType: User.UserType): List<DrawerItem> {
        return when (userType) {
            User.UserType.TECHNICIAN -> listOf(
                DrawerItem.SupportTicketsList,
                DrawerItem.InstallationOrders,
                DrawerItem.RegisterSubscription,
                DrawerItem.SubscriptionFinder,
                DrawerItem.Profile,
            )

            User.UserType.SALES -> listOf(
                DrawerItem.InstallationOrders,
                DrawerItem.Profile
            )

            User.UserType.ADMIN, User.UserType.SECRETARY, User.UserType.ACCOUNTANT -> listOf(
                DrawerItem.SubscriptionFinder,
                DrawerItem.RegisterSubscription,
                DrawerItem.CreateSupportTicket,
                DrawerItem.SupportTicketsList,
                DrawerItem.InstallationOrders,
                DrawerItem.PayerFinder,
                DrawerItem.Profile,
                )

            else -> throw Exception("Tipo de usuario no soportado")
        }
    }
} 