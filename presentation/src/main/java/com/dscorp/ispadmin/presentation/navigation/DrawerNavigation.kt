package com.dscorp.ispadmin.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes

sealed class DrawerItem(
    val title: String,
    val route: Any,
    val icon: ImageVector
) {

    //Grupo Dashboard
    object Dashboard : DrawerItem("Dashboard", FeatureRoutes.Dashboard, Icons.Default.Dashboard)

    // Grupo: Perfil
    object Profile : DrawerItem("Mi Perfil", FeatureRoutes.Profile, Icons.Default.Person)

    // Grupo: Instalaciones
    object InstallationOrders :
        DrawerItem("Órdenes de Instalación", FeatureRoutes.Installation.List,
            Icons.Filled.Handyman
        )

    // Grupo: Soporte
    object SupportTicketsList :
        DrawerItem("Tickets de Soporte", FeatureRoutes.SupportTicket.List,
            Icons.Filled.ConfirmationNumber
        )

    object CreateSupportTicket :
        DrawerItem("Crear ticket", FeatureRoutes.SupportTicket.Create, Icons.Filled.AddCircle)

    // Grupo: Suscripciones
    object SubscriptionFinder :
        DrawerItem("Buscador de suscripciones", FeatureRoutes.Subscription.Find,
            Icons.AutoMirrored.Filled.ManageSearch
        )

    object RegisterSubscription : DrawerItem(
        "Registrar suscripción",
        FeatureRoutes.Subscription.Register(),
        Icons.AutoMirrored.Filled.NoteAdd
    )

    // Grupo: Pagos
    object PayerFinder :
        DrawerItem("Buscador de pagadores", FeatureRoutes.Payment.FindPayer, Icons.Filled.CreditCard
        )
}

sealed class DrawerGroup(
    val title: String,
    val items: List<DrawerItem>
) {
    object Dashboard : DrawerGroup("Dashboard", listOf(DrawerItem.Dashboard))
    object Profile : DrawerGroup("Perfil", listOf(DrawerItem.Profile))
    object Installation : DrawerGroup("Instalaciones", listOf(DrawerItem.InstallationOrders))
    object Support : DrawerGroup(
        "Soporte",
        listOf(DrawerItem.SupportTicketsList, DrawerItem.CreateSupportTicket)
    )

    object Subscription : DrawerGroup(
        "Suscripciones",
        listOf(DrawerItem.SubscriptionFinder, DrawerItem.RegisterSubscription)
    )

    object Payment : DrawerGroup("Pagos", listOf(DrawerItem.PayerFinder))
}

object DrawerNavigation {
    fun getDrawerGroupsForUser(userType: User.UserType): List<DrawerGroup> {
        return when (userType) {
            User.UserType.TECHNICIAN -> listOf(
                DrawerGroup.Support,
                DrawerGroup.Installation,
                DrawerGroup.Subscription,
                DrawerGroup.Profile
            )

            User.UserType.SALES -> listOf(
                DrawerGroup.Installation,
                DrawerGroup.Profile
            )

            User.UserType.ADMIN, User.UserType.SECRETARY, User.UserType.ACCOUNTANT -> listOf(
                DrawerGroup.Dashboard,
                DrawerGroup.Subscription,
                DrawerGroup.Support,
                DrawerGroup.Installation,
                DrawerGroup.Payment,
                DrawerGroup.Profile
            )

            else -> throw Exception("Tipo de usuario no soportado")
        }
    }

    // Método de compatibilidad para mantener la funcionalidad existente
    fun getDrawerItemsForUser(userType: User.UserType): List<DrawerItem> {
        return getDrawerGroupsForUser(userType).flatMap { it.items }
    }
} 