package com.dscorp.ispadmin.navigation

import com.dscorp.ispadmin.domain.model.User.UserType
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Installation
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Outlay
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Payment
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Plan
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Subscription
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.SupportTicket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainNavigationTest {

    @Test
    fun `bottom bar follows the role`() {
        assertEquals(
            listOf(MainDestination.HOME, MainDestination.SUBSCRIPTIONS, MainDestination.TICKETS, MainDestination.CASH, MainDestination.MORE),
            MainNavigation.bottomBarFor(UserType.ADMIN),
        )
        assertEquals(
            listOf(MainDestination.HOME, MainDestination.SUBSCRIPTIONS, MainDestination.CASH, MainDestination.MORE),
            MainNavigation.bottomBarFor(UserType.ACCOUNTANT),
        )
        assertEquals(
            listOf(MainDestination.HOME, MainDestination.SUBSCRIPTIONS, MainDestination.CASH, MainDestination.TICKETS, MainDestination.MORE),
            MainNavigation.bottomBarFor(UserType.SECRETARY),
        )
        assertEquals(
            listOf(MainDestination.TICKETS, MainDestination.INSTALLATIONS, MainDestination.SUBSCRIPTIONS, MainDestination.MORE),
            MainNavigation.bottomBarFor(UserType.TECHNICIAN),
        )
        assertEquals(listOf(MainDestination.INSTALLATIONS, MainDestination.MORE), MainNavigation.bottomBarFor(UserType.SALES))
    }

    @Test
    fun `roles without operations only get more and start there`() {
        listOf(UserType.LOGISTIC, UserType.CLIENT).forEach { type ->
            assertEquals(type.name, listOf(MainDestination.MORE), MainNavigation.bottomBarFor(type))
            assertEquals(type.name, listOf(MoreItem.PROFILE), MainNavigation.moreItemsFor(type))
            assertEquals(type.name, FeatureRoutes.More, MainNavigation.startRouteFor(type))
        }
    }

    // Migrated from DrawerNavigationRolesTest: no role may crash the shell.
    @Test
    fun `every user type gets navigation without crashing`() {
        UserType.entries.forEach { type ->
            assertTrue("$type has no bar", MainNavigation.bottomBarFor(type).isNotEmpty())
            assertTrue("$type has no profile", MoreItem.PROFILE in MainNavigation.moreItemsFor(type))
        }
    }

    // Migrated from DrawerNavigationPendingSubscriptionsTest.
    @Test
    fun `technician reaches pending subscriptions and registration`() {
        val items = MainNavigation.moreItemsFor(UserType.TECHNICIAN)
        assertEquals(Subscription.PendingSubscriptions, items.first { it.label == "Pendientes de envío" }.route)
        assertTrue(items.any { it.route == Subscription.Register() })
    }

    @Test
    fun `start route is the first bar destination`() {
        assertEquals(FeatureRoutes.Dashboard, MainNavigation.startRouteFor(UserType.SECRETARY))
        assertEquals(SupportTicket.List, MainNavigation.startRouteFor(UserType.TECHNICIAN))
        assertEquals(Installation.List, MainNavigation.startRouteFor(UserType.SALES))
    }

    @Test
    fun `every destination of the old drawer stays reachable for each role`() {
        val oldDrawer: Map<UserType, Set<Any>> = mapOf(
            UserType.ADMIN to setOf(
                FeatureRoutes.Dashboard, Subscription.Find, Subscription.Register(), Subscription.PendingSubscriptions,
                SupportTicket.List, SupportTicket.Create, Installation.List, Payment.FindPayer, Outlay.Register,
                FeatureRoutes.Profile, Plan.List,
            ),
            UserType.ACCOUNTANT to setOf(
                FeatureRoutes.Dashboard, Subscription.Find, Subscription.Register(), Subscription.PendingSubscriptions,
                SupportTicket.List, SupportTicket.Create, Installation.List, Payment.FindPayer, Outlay.Register,
                FeatureRoutes.Profile,
            ),
            UserType.SECRETARY to setOf(
                FeatureRoutes.Dashboard, Subscription.Find, Subscription.PendingSubscriptions, SupportTicket.List,
                SupportTicket.Create, Installation.List, Payment.FindPayer, Outlay.Register, FeatureRoutes.Profile,
            ),
            UserType.TECHNICIAN to setOf(
                SupportTicket.List, Installation.List, Subscription.Find, Subscription.Register(),
                Subscription.PendingSubscriptions, FeatureRoutes.Profile,
            ),
            UserType.SALES to setOf(Installation.List, FeatureRoutes.Profile),
        )
        oldDrawer.forEach { (type, routes) ->
            val reachable = MainNavigation.bottomBarFor(type).map { it.route } + MainNavigation.moreItemsFor(type).map { it.route }
            assertEquals(type.name, emptySet<Any>(), routes - reachable.toSet())
        }
    }

    @Test
    fun `only registrar roles see register subscription in more`() {
        assertTrue(MoreItem.REGISTER_SUBSCRIPTION in MainNavigation.moreItemsFor(UserType.TECHNICIAN))
        assertFalse(MoreItem.REGISTER_SUBSCRIPTION in MainNavigation.moreItemsFor(UserType.SECRETARY))
        assertTrue(MoreItem.PLANS in MainNavigation.moreItemsFor(UserType.ADMIN))
        assertFalse(MoreItem.PLANS in MainNavigation.moreItemsFor(UserType.ACCOUNTANT))
    }

    @Test
    fun `top level screens show the bar and no back arrow`() {
        val chrome = MainNavigation.shellChrome(UserType.SECRETARY, Subscription.Find)
        assertFalse(chrome.showBack)
        assertEquals(MainNavigation.bottomBarFor(UserType.SECRETARY), chrome.bottomBar)
        assertEquals(MainDestination.SUBSCRIPTIONS, chrome.selected)
        assertEquals("Suscripciones", chrome.title)
    }

    @Test
    fun `secondary screens show a back arrow and hide the bar`() {
        val chrome = MainNavigation.shellChrome(UserType.SECRETARY, Subscription.Details(0))
        assertTrue(chrome.showBack)
        assertEquals(emptyList<MainDestination>(), chrome.bottomBar)
        assertEquals("Ficha de suscripción", chrome.title)
    }

    @Test
    fun `a destination outside the role bar is secondary`() {
        val chrome = MainNavigation.shellChrome(UserType.ACCOUNTANT, SupportTicket.List)
        assertTrue(chrome.showBack)
        assertEquals(emptyList<MainDestination>(), chrome.bottomBar)
    }

    @Test
    fun `dashboard keeps the bar but draws its own top bar`() {
        val chrome = MainNavigation.shellChrome(UserType.ADMIN, FeatureRoutes.Dashboard)
        assertFalse(chrome.showTopBar)
        assertEquals(MainDestination.HOME, chrome.selected)
    }

    @Test
    fun `a single destination role hides the bar`() {
        assertEquals(emptyList<MainDestination>(), MainNavigation.shellChrome(UserType.LOGISTIC, FeatureRoutes.More).bottomBar)
    }

    @Test
    fun `screens that showed ISP Admin get their own title`() {
        assertEquals("Planes", MainNavigation.screenTitle(Plan.List))
        assertEquals("Registro de la ONU", MainNavigation.screenTitle(Subscription.Provisioning(0)))
        assertEquals("Más", MainNavigation.screenTitle(FeatureRoutes.More))
    }

    @Test
    fun `route names resolve plans and more`() {
        assertEquals(Plan.List, FeatureRoutes.FromString(Plan.List::class.qualifiedName))
        assertEquals(FeatureRoutes.More, FeatureRoutes.FromString(FeatureRoutes.More::class.qualifiedName))
    }

    @Test
    fun `routes with path arguments resolve so secondary screens get title and back`() {
        val details = FeatureRoutes.FromString("${Subscription.Details::class.qualifiedName}/{subscriptionId}")
        assertEquals(Subscription.Details(0), details)
        val chrome = MainNavigation.shellChrome(UserType.ADMIN, details)
        assertTrue(chrome.showBack)
        assertEquals("Ficha de suscripción", chrome.title)

        assertEquals(
            Payment.History(0, ""),
            FeatureRoutes.FromString("${Payment.History::class.qualifiedName}/{subscriptionId}/{status}"),
        )
        assertEquals(
            Subscription.ChangePlan(0),
            FeatureRoutes.FromString("${Subscription.ChangePlan::class.qualifiedName}/{subscriptionId}"),
        )
        assertEquals(
            SupportTicket.Detail(0),
            FeatureRoutes.FromString("${SupportTicket.Detail::class.qualifiedName}/{ticketId}"),
        )
    }
}
