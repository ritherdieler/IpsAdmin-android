package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import com.dscorp.ispadmin.data.datasource.remote.PlanCountResponse
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.label
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.planLabel
import kotlin.math.abs

data class CurrentPlan(val name: String, val detail: String, val price: String)
data class PlanOption(val id: Int, val name: String, val price: String, val detail: String, val current: Boolean)
data class PlanChange(val price: String, val delta: String, val action: String)

/** a-sus-09: active plans of the same installation type; another type is a migration, not a plan change. */
data class ChangePlan(
    val subscriptionId: Int,
    val current: CurrentPlan,
    val title: String,
    /** Why other installation types are not listed. */
    val note: String,
    val options: List<PlanOption>,
    private val prices: Map<Int, Double>,
    private val currentId: Int?,
    private val currentPrice: Double,
) {
    /** What the subscription would pay with [planId]; null for the plan it already has. */
    fun change(planId: Int): PlanChange? {
        if (planId == currentId) return null
        val option = options.firstOrNull { it.id == planId } ?: return null
        val price = prices.getValue(planId)
        val difference = price - currentPrice
        val sign = if (difference < 0) "−" else "+"
        return PlanChange(formatSoles(price), "$sign${formatSoles(abs(difference))}", "Cambiar a ${option.name}")
    }

    companion object {
        fun from(subscription: SubscriptionResponse, plans: List<PlanCountResponse>): ChangePlan {
            val type = subscription.installationType
            val currentId = subscription.plan?.id?.toIntOrNull()
            val sameType = plans
                .filter { it.id != null && it.type == type && (it.isActive || it.id == currentId) }
                .sortedWith(compareByDescending<PlanCountResponse> { it.id == currentId }.thenByDescending { it.activeSubscriptionsCount })
            val currentPrice = subscription.plan?.price ?: 0.0
            val speeds = speeds(subscription.plan?.downloadSpeed?.toIntOrNull(), subscription.plan?.uploadSpeed?.toIntOrNull())
            return ChangePlan(
                subscriptionId = subscription.id,
                current = CurrentPlan(
                    name = subscription.plan?.name?.let(::planLabel).orEmpty(),
                    detail = listOfNotNull(type.label(), speeds).joinToString(" · "),
                    price = formatSoles(currentPrice),
                ),
                title = "${typePlural(type)} (${sameType.size})",
                note = when (type) {
                    InstallationType.FIBER -> "Cambiar a inalámbrico o Solo TV es una migración"
                    InstallationType.WIRELESS -> "Pasar a fibra es una migración"
                    InstallationType.ONLY_TV_FIBER -> "Agregar internet es una migración"
                },
                options = sameType.map { plan ->
                    val count = plan.activeSubscriptionsCount
                    PlanOption(
                        id = plan.id!!,
                        name = planLabel(plan.name.orEmpty()),
                        price = formatSoles(plan.price ?: 0.0),
                        detail = listOfNotNull(speeds(plan.downloadSpeed, plan.uploadSpeed), "$count ${if (count == 1) "abonado" else "abonados"}")
                            .joinToString(" · "),
                        current = plan.id == currentId,
                    )
                },
                prices = sameType.associate { it.id!! to (it.price ?: 0.0) },
                currentId = currentId,
                currentPrice = currentPrice,
            )
        }

        private fun speeds(down: Int?, up: Int?): String? = if (down != null && up != null && down > 0) "$down/$up Mbps" else null

        private fun typePlural(type: InstallationType): String = when (type) {
            InstallationType.FIBER -> "Planes de fibra activos"
            InstallationType.WIRELESS -> "Planes inalámbricos activos"
            InstallationType.ONLY_TV_FIBER -> "Planes de solo TV activos"
        }
    }
}
