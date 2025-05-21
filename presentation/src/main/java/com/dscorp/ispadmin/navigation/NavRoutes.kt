package com.dscorp.ispadmin.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoutes {

    @Serializable
    object Splash

    @Serializable
    object Features

    @Serializable
    object Auth

    sealed class AuthRoutes : NavRoutes {
        @Serializable
        object Login : AuthRoutes()

        @Serializable
        object Register : AuthRoutes()
    }

    @Serializable
    sealed class FeatureRoutes : NavRoutes {

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
        sealed class Subscription : NavRoutes {

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
        sealed class Payment : NavRoutes {

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
        sealed class Installation : NavRoutes {

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
        sealed class SupportTicket : NavRoutes {

            @Serializable
            object List : SupportTicket()

            @Serializable
            object Create : SupportTicket()

            @Serializable
            data class Detail(val ticketId: Int) : SupportTicket()

            @Serializable
            data class Close(val ticketId: Int) : SupportTicket()

            @Serializable
            data class ViewImage(val imageUrl: String) : SupportTicket()
        }
    }

}

