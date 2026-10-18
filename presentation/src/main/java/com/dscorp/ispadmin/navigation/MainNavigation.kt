package com.dscorp.ispadmin.navigation

import com.dscorp.ispadmin.domain.model.User.UserType
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Installation
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Payment
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Subscription
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.SupportTicket

/** Root destinations shown in the bottom bar. Caja opens the payer finder until the cash screen exists. */
enum class MainDestination(val label: String, val route: FeatureRoutes) {
    HOME("Inicio", FeatureRoutes.Dashboard),
    TODAY("Hoy", FeatureRoutes.Dashboard),
    SUBSCRIPTIONS("Suscripciones", Subscription.Find),
    TICKETS("Tickets", SupportTicket.List),
    CASH("Caja", Payment.FindPayer),
    INSTALLATIONS("Instalaciones", Installation.List),
    MORE("Más", FeatureRoutes.More),
}

/** Secondary entries listed in «Más», filtered by role. */
enum class MoreItem(val label: String, val route: FeatureRoutes) {
    REGISTER_SUBSCRIPTION("Registrar suscripción", Subscription.Register()),
    CREATE_TICKET("Nuevo ticket", SupportTicket.Create),
    SUPPORT_TICKETS("Tickets de soporte", SupportTicket.List),
    INSTALLATION_ORDERS("Órdenes de instalación", Installation.List),
    PENDING_SUBSCRIPTIONS("Pendientes de envío", Subscription.PendingSubscriptions),
    OUTLAYS("Egresos", FeatureRoutes.Outlay.Month),
    REGISTER_OUTLAY("Registrar egreso", FeatureRoutes.Outlay.Register),
    PLANS("Planes", FeatureRoutes.Plan.List),
    PROFILE("Mi perfil", FeatureRoutes.Profile),
}

data class ShellChrome(
    val title: String,
    val showBack: Boolean,
    val showTopBar: Boolean,
    val bottomBar: List<MainDestination>,
    val selected: MainDestination?,
)

object MainNavigation {
    private const val DEFAULT_TITLE = "IspAdmin"

    fun bottomBarFor(type: UserType): List<MainDestination> = when (type) {
        UserType.ADMIN -> listOf(MainDestination.HOME, MainDestination.SUBSCRIPTIONS, MainDestination.TICKETS, MainDestination.CASH, MainDestination.MORE)
        UserType.ACCOUNTANT -> listOf(MainDestination.HOME, MainDestination.SUBSCRIPTIONS, MainDestination.CASH, MainDestination.MORE)
        UserType.SECRETARY -> listOf(MainDestination.HOME, MainDestination.SUBSCRIPTIONS, MainDestination.CASH, MainDestination.TICKETS, MainDestination.MORE)
        UserType.TECHNICIAN -> listOf(MainDestination.TODAY, MainDestination.TICKETS, MainDestination.INSTALLATIONS, MainDestination.SUBSCRIPTIONS, MainDestination.MORE)
        UserType.SALES -> listOf(MainDestination.INSTALLATIONS, MainDestination.SUBSCRIPTIONS, MainDestination.MORE)
        UserType.LOGISTIC, UserType.CLIENT -> listOf(MainDestination.MORE)
    }

    fun moreItemsFor(type: UserType): List<MoreItem> = when (type) {
        UserType.ADMIN -> listOf(
            MoreItem.REGISTER_SUBSCRIPTION, MoreItem.CREATE_TICKET, MoreItem.INSTALLATION_ORDERS,
            MoreItem.PENDING_SUBSCRIPTIONS, MoreItem.OUTLAYS, MoreItem.REGISTER_OUTLAY, MoreItem.PLANS, MoreItem.PROFILE,
        )
        UserType.ACCOUNTANT -> listOf(
            MoreItem.REGISTER_SUBSCRIPTION, MoreItem.SUPPORT_TICKETS, MoreItem.CREATE_TICKET, MoreItem.INSTALLATION_ORDERS,
            MoreItem.PENDING_SUBSCRIPTIONS, MoreItem.OUTLAYS, MoreItem.REGISTER_OUTLAY, MoreItem.PROFILE,
        )
        UserType.SECRETARY -> listOf(
            MoreItem.CREATE_TICKET, MoreItem.INSTALLATION_ORDERS, MoreItem.PENDING_SUBSCRIPTIONS,
            MoreItem.OUTLAYS, MoreItem.REGISTER_OUTLAY, MoreItem.PROFILE,
        )
        UserType.TECHNICIAN -> listOf(MoreItem.REGISTER_SUBSCRIPTION, MoreItem.PENDING_SUBSCRIPTIONS, MoreItem.PROFILE)
        UserType.SALES, UserType.LOGISTIC, UserType.CLIENT -> listOf(MoreItem.PROFILE)
    }

    fun startRouteFor(type: UserType): FeatureRoutes = bottomBarFor(type).first().route

    fun screenTitle(route: FeatureRoutes): String = when (route) {
        is FeatureRoutes.Dashboard -> "Inicio"
        is FeatureRoutes.More -> "Más"
        is FeatureRoutes.Profile -> "Perfil"
        is Subscription.Find -> "Suscripciones"
        is Subscription.Register -> "Nueva suscripción"
        is Subscription.PendingSubscriptions -> "Pendientes de envío"
        is Subscription.Details -> "Ficha de suscripción"
        is Subscription.Provisioning -> "Registro de la ONU"
        is Subscription.ChangePlan -> "Cambiar plan"
        is Subscription.ChangeNap -> "Cambiar caja NAP"
        is Subscription.UpdateLocation -> "Actualizar ubicación"
        is Subscription.DeferCut -> "Postergar corte"
        is Subscription.EditHolder -> "Editar titular"
        is Subscription.Migrate -> "Migrar a fibra óptica"
        is Subscription.Edit -> "Editar suscripción"
        is Payment.Register -> "Registrar pago"
        is Payment.History -> "Historial de pagos"
        is Payment.Detail -> "Detalle de pago"
        is Payment.FindPayer -> "Caja"
        is Payment.SearchPayers -> "Buscar pagos"
        is Payment.Receipt -> "Detalle de pago"
        is SupportTicket.List -> "Tickets"
        is SupportTicket.Create -> "Nuevo ticket"
        is SupportTicket.Detail -> "Ticket"
        is SupportTicket.Close -> "Cerrar ticket"
        is FeatureRoutes.AsyncImageViewer -> "Imagen"
        is Installation.List -> "Instalaciones"
        is Installation.Create -> "Nueva orden de instalación"
        is FeatureRoutes.Plan.List -> "Planes"
        is FeatureRoutes.Outlay.Register -> "Registrar egreso"
        is FeatureRoutes.Outlay.Month -> "Egresos"
        else -> DEFAULT_TITLE
    }

    fun shellChrome(type: UserType, route: FeatureRoutes): ShellChrome {
        val bar = bottomBarFor(type)
        val selected = bar.firstOrNull { it.route == route }
        val isTopLevel = selected != null || route is FeatureRoutes.Home
        return ShellChrome(
            title = selected?.label ?: screenTitle(route),
            showBack = !isTopLevel,
            // Dashboard, the subscription search and the ficha draw their own header, as in the design.
            showTopBar = route !is FeatureRoutes.Dashboard && route !is Subscription.Find && route !is Subscription.Details &&
                route !is SupportTicket.Detail && route !is SupportTicket.Close && route !is SupportTicket.Create && route !is Subscription.Register && route !is Payment.FindPayer &&
                route !is Payment.SearchPayers && route !is Payment.Receipt && route !is Subscription.ChangePlan && route !is Subscription.ChangeNap && route !is Subscription.UpdateLocation && route !is Subscription.DeferCut && route !is Subscription.EditHolder &&
                !(type == UserType.SALES && route is Installation.List),
            bottomBar = if (isTopLevel && bar.size > 1) bar else emptyList(),
            selected = selected,
        )
    }
}
