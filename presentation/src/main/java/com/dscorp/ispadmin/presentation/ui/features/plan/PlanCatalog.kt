package com.dscorp.ispadmin.presentation.ui.features.plan

import com.dscorp.ispadmin.data.datasource.remote.PlanCountResponse
import com.dscorp.ispadmin.data.datasource.remote.PlanSaveRequest
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.planLabel

data class PlanRow(val id: Int, val name: String, val price: String, val detail: String)
data class PlanGroup(val title: String, val rows: List<PlanRow>)

private val TYPE_ORDER = listOf(InstallationType.FIBER, InstallationType.WIRELESS, InstallationType.ONLY_TV_FIBER)

internal fun InstallationType.planTypeLabel(): String = when (this) {
    InstallationType.FIBER -> "Fibra"
    InstallationType.WIRELESS -> "Inalámbrico"
    InstallationType.ONLY_TV_FIBER -> "Solo TV"
}

/** a-mas-03: the plan catalog with how many active subscriptions use each plan. */
data class PlanCatalog(private val plans: List<PlanCountResponse>) {
    val activeCount: Int get() = plans.count { it.isActive }
    val retiredCount: Int get() = plans.count { !it.isActive }

    fun plan(id: Int): PlanCountResponse? = plans.firstOrNull { it.id == id }

    fun groups(retired: Boolean): List<PlanGroup> = plans.filter { it.id != null && it.isActive != retired && it.type != null }
        .groupBy { it.type!! }
        .toSortedMap(compareBy { TYPE_ORDER.indexOf(it) })
        .map { (type, list) ->
            PlanGroup(
                title = "${type.planTypeLabel()} · ${list.size}",
                rows = list.sortedByDescending { it.activeSubscriptionsCount }.map { plan ->
                    val count = plan.activeSubscriptionsCount
                    val speeds = if (type == InstallationType.ONLY_TV_FIBER) "TV cable, sin internet"
                    else "${plan.downloadSpeed ?: 0}/${plan.uploadSpeed ?: 0} Mbps"
                    PlanRow(plan.id!!, planLabel(plan.name.orEmpty()), formatSoles(plan.price ?: 0.0),
                        "$speeds · $count ${if (count == 1) "abonado" else "abonados"}")
                },
            )
        }

    companion object {
        fun from(plans: List<PlanCountResponse>) = PlanCatalog(plans)
    }
}

/** a-mas-04: one plan being edited or created. */
data class PlanEditor(
    val id: Int?,
    val name: String,
    val type: InstallationType,
    val price: String,
    val download: String,
    val upload: String,
    val available: Boolean,
    val subscribers: Int,
    private val original: PlanEditor? = null,
) {
    val isNew: Boolean get() = id == null
    val typeLocked: Boolean get() = !isNew
    val typeNote: String? get() = when {
        isNew -> null
        subscribers > 0 -> "No se puede cambiar: tiene abonados"
        else -> "El tipo no se cambia después de crear el plan"
    }
    val warning: String? get() = subscribers.takeIf { it > 0 }?.let {
        "$it ${if (it == 1) "abonado usa" else "abonados usan"} este plan. Cambiar el precio cambia su mensualidad y la velocidad actualiza su cola en el MikroTik."
    }
    val preview: String get() = "Se muestra como “${planLabel(name.trim())}”"
    private val isTv: Boolean get() = type == InstallationType.ONLY_TV_FIBER

    val blockingReason: String? get() = when {
        name.isBlank() -> "Escribe el nombre"
        (price.toMoney() ?: 0.0) <= 0.0 -> "Escribe el precio"
        !isTv && ((download.toIntOrNull() ?: 0) <= 0 || (upload.toIntOrNull() ?: 0) <= 0) -> "La bajada y la subida deben ser mayores a 0"
        else -> null
    }

    val changed: Boolean get() = original == null || commercialChanged || availabilityChanged
    val commercialChanged: Boolean
        get() = original == null || name.trim() != original.name || price.toMoney() != original.price.toMoney() ||
            download.toIntOrNull() != original.download.toIntOrNull() || upload.toIntOrNull() != original.upload.toIntOrNull()
    val availabilityChanged: Boolean get() = original != null && available != original.available

    fun toRequest() = PlanSaveRequest(
        id = id,
        name = name.trim(),
        price = price.toMoney() ?: 0.0,
        downloadSpeed = download.toIntOrNull() ?: 0,
        uploadSpeed = upload.toIntOrNull() ?: 0,
        type = type,
    )

    companion object {
        fun edit(plan: PlanCountResponse): PlanEditor {
            val base = PlanEditor(
                id = plan.id,
                name = plan.name.orEmpty(),
                type = plan.type ?: InstallationType.FIBER,
                price = plan.price?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty(),
                download = plan.downloadSpeed?.toString().orEmpty(),
                upload = plan.uploadSpeed?.toString().orEmpty(),
                available = plan.isActive,
                subscribers = plan.activeSubscriptionsCount,
            )
            return base.copy(original = base)
        }

        fun new(type: InstallationType = InstallationType.FIBER) =
            PlanEditor(id = null, name = "", type = type, price = "", download = "", upload = "", available = true, subscribers = 0)
    }
}

private fun String.toMoney(): Double? = trim().replace(",", ".").toDoubleOrNull()
