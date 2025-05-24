package com.dscorp.ispadmin.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoutes {

    @Serializable
    object Splash : NavRoutes()

    @Serializable
    object Features : NavRoutes()

    @Serializable
    object Auth : NavRoutes()

    @Serializable
    sealed class AuthRoutes : NavRoutes() {
        @Serializable
        object Login : AuthRoutes()

        @Serializable
        object Register : AuthRoutes()
    }

    @Serializable
    sealed class FeatureRoutes : NavRoutes() {
        @Serializable
        object Home : FeatureRoutes()

        @Serializable
        object Dashboard : FeatureRoutes()

        @Serializable
        object Profile : FeatureRoutes()

        @Serializable
        object Reports : FeatureRoutes()

        @Serializable
        data class AsyncImageViewer(val imageUrl: String) : FeatureRoutes()

        @Serializable
        sealed class Subscription : FeatureRoutes() {

            @Serializable
            object Register : Subscription()

            @Serializable
            object Find : Subscription()

            @Serializable
            data class Details(val subscriptionId: Int) : Subscription()

            @Serializable
            data class ChangePlan(val subscriptionId: Int) : Subscription()

            @Serializable
            data class Migrate(val subscriptionId: Int) : Subscription()

            @Serializable
            data class Edit(val subscriptionId: Int) : Subscription()
        }

        @Serializable
        sealed class Payment : FeatureRoutes() {

            @Serializable
            data class Register(val paymentId: Int) : Payment()

            @Serializable
            data class History(val subscriptionId: Int, val serviceStatus: String) : Payment()

            @Serializable
            data class Detail(val paymentId: String) : Payment()

            @Serializable
            object FindPayer : Payment()
        }


        @Serializable
        sealed class Installation : FeatureRoutes() {

            @Serializable
            object List : Installation()

            @Serializable
            object Create : Installation()

            @Serializable
            object Pending : Installation()

            @Serializable
            object Assigned : Installation()

            @Serializable
            object InProgress : Installation()

            @Serializable
            object Closed : Installation()
        }

        @Serializable
        sealed class SupportTicket : FeatureRoutes() {

            @Serializable
            object List : SupportTicket()

            @Serializable
            object Create : SupportTicket()

            @Serializable
            data class Detail(val ticketId: Int) : SupportTicket()

            @Serializable
            data class Close(val ticketId: Int) : SupportTicket()
        }

        companion object {
            fun FromString(string: String?): FeatureRoutes {
                return when (string) {
                    Home::class.qualifiedName -> Home
                    Dashboard::class.qualifiedName -> Dashboard
                    Profile::class.qualifiedName -> Profile
                    Reports::class.qualifiedName -> Reports
                    AsyncImageViewer::class.qualifiedName -> AsyncImageViewer("")

                    Subscription.Register::class.qualifiedName -> Subscription.Register
                    Subscription.Find::class.qualifiedName -> Subscription.Find
                    Subscription.Details::class.qualifiedName -> Subscription.Details(0)
                    Subscription.ChangePlan::class.qualifiedName -> Subscription.ChangePlan(0)
                    Subscription.Migrate::class.qualifiedName -> Subscription.Migrate(0)
                    Subscription.Edit::class.qualifiedName -> Subscription.Edit(0)

                    Payment.Register::class.qualifiedName -> Payment.Register(0)
                    Payment.History::class.qualifiedName -> Payment.History(0, "")
                    Payment.Detail::class.qualifiedName -> Payment.Detail("")
                    Payment.FindPayer::class.qualifiedName -> Payment.FindPayer

                    Installation.List::class.qualifiedName -> Installation.List
                    Installation.Create::class.qualifiedName -> Installation.Create
                    Installation.Pending::class.qualifiedName -> Installation.Pending
                    Installation.Assigned::class.qualifiedName -> Installation.Assigned
                    Installation.InProgress::class.qualifiedName -> Installation.InProgress
                    Installation.Closed::class.qualifiedName -> Installation.Closed

                    SupportTicket.List::class.qualifiedName -> SupportTicket.List
                    SupportTicket.Create::class.qualifiedName -> SupportTicket.Create
                    SupportTicket.Detail::class.qualifiedName -> SupportTicket.Detail(0)
                    SupportTicket.Close::class.qualifiedName -> SupportTicket.Close(0)
                    else -> Home // Default case
                }
            }

        }
    }
}
