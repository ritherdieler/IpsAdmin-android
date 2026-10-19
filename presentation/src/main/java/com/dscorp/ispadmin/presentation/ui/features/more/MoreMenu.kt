package com.dscorp.ispadmin.presentation.ui.features.more

import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.navigation.MoreItem
import com.dscorp.ispadmin.presentation.ui.features.payment.cash.shortName

data class MoreHeader(val initials: String, val name: String, val role: String)
data class MoreEntry(val item: MoreItem, val badge: String?)
data class MoreSection(val title: String, val entries: List<MoreEntry>)

/** a-mas-01: the user on top, the role's entries grouped, logout at the end. */
data class MoreMenu(val header: MoreHeader, val sections: List<MoreSection>, val hasProfile: Boolean) {
    companion object {
        private val SECTIONS = listOf(
            "Gestión" to listOf(MoreItem.PLANS, MoreItem.OUTLAYS, MoreItem.REGISTER_OUTLAY, MoreItem.REPORTS, MoreItem.INSTALLATION_ORDERS),
            "Suscripciones" to listOf(MoreItem.REGISTER_SUBSCRIPTION, MoreItem.PENDING_SUBSCRIPTIONS),
            "Tickets" to listOf(MoreItem.SUPPORT_TICKETS, MoreItem.CREATE_TICKET),
            "Red" to listOf(MoreItem.NETWORK_MAP, MoreItem.OLT_TOOLS),
        )

        fun from(user: User, items: List<MoreItem>, pending: Int?, activePlans: Int?): MoreMenu {
            val fullName = listOf(user.name, user.lastName).joinToString(" ").trim()
            val words = fullName.split(Regex("\\s+")).filter { it.isNotEmpty() }
            return MoreMenu(
                header = MoreHeader(
                    initials = words.take(2).joinToString("") { it.first().uppercase() },
                    name = shortName(fullName),
                    role = roleLabel(user.type),
                ),
                sections = SECTIONS.mapNotNull { (title, order) ->
                    val entries = order.filter { it in items }.map { item ->
                        MoreEntry(item, when (item) {
                            MoreItem.PLANS -> activePlans?.let { "$it ${if (it == 1) "activo" else "activos"}" }
                            MoreItem.PENDING_SUBSCRIPTIONS -> pending?.toString()
                            else -> null
                        })
                    }
                    entries.takeIf { it.isNotEmpty() }?.let { MoreSection(title, it) }
                },
                hasProfile = MoreItem.PROFILE in items,
            )
        }

        private fun roleLabel(type: User.UserType?): String = when (type) {
            User.UserType.ADMIN -> "Administrador"
            User.UserType.ACCOUNTANT -> "Contador"
            User.UserType.SECRETARY -> "Secretaria"
            User.UserType.TECHNICIAN -> "Técnico"
            User.UserType.SALES -> "Ventas"
            User.UserType.LOGISTIC -> "Logística"
            User.UserType.CLIENT -> "Cliente"
            null -> ""
        }
    }
}
