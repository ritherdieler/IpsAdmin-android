package com.dscorp.ispadmin.presentation.ui.features.networkmap

import com.dscorp.ispadmin.data.datasource.remote.NapDetailResponse
import com.dscorp.ispadmin.data.datasource.remote.NapMapResponse
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.badge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.planLabel
import java.util.Locale

private const val ROOMY_FROM = 5

/** ≥5 free ports, 1–4, or full: the colors of the map legend. */
enum class NapLevel(val label: String) { ROOMY("≥5 libres"), FEW("1–4"), FULL("Llena") }

data class NapPin(val id: Int, val code: String, val latitude: Double, val longitude: Double, val free: Int, val level: NapLevel)
data class MufaPin(val id: Int, val label: String, val latitude: Double, val longitude: Double)

/** a-mas-05: the boxes of one place by how full they are, and their mufas. */
data class NetworkMap(
    val placeId: Int?,
    val placeName: String,
    val places: List<Pair<Int, String>>,
    val pins: List<NapPin>,
    val mufas: List<MufaPin>,
) {
    companion object {
        fun from(response: NapMapResponse) = NetworkMap(
            placeId = response.placeId,
            placeName = response.placeName.orEmpty().toTitleCase(),
            places = response.places.map { it.id to it.name.toTitleCase() },
            pins = response.boxes.map { box ->
                val free = (box.totalPorts - box.taken).coerceAtLeast(0)
                NapPin(box.id, box.code, box.latitude, box.longitude, free, when {
                    free >= ROOMY_FROM -> NapLevel.ROOMY
                    free > 0 -> NapLevel.FEW
                    else -> NapLevel.FULL
                })
            },
            mufas = response.mufas.map { MufaPin(it.id, it.reference?.takeIf(String::isNotBlank) ?: "Mufa ${it.id}", it.latitude, it.longitude) },
        )
    }
}

data class NapPort(val number: Int, val initials: String?)
data class NapHolder(val subscriptionId: Int, val initials: String, val name: String, val detail: String, val status: StatusBadge, val debt: String?)

/** a-mas-06: one box with its ports and the subscriptions connected to it. */
data class NapDetail(
    val id: Int,
    val code: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val freeLabel: String,
    val free: Int,
    val network: String,
    val rows: List<Pair<String, String>>,
    val ports: List<NapPort>,
    val holders: List<NapHolder>,
) {
    val holdersLabel: String get() = when (holders.size) {
        0 -> "Sin abonados"
        1 -> "Ver 1 abonado"
        else -> "Ver ${holders.size} abonados"
    }

    companion object {
        fun from(response: NapDetailResponse): NapDetail {
            val byPort = response.subscribers.filter { it.port != null }.associateBy { it.port!! }
            val taken = byPort.keys.count { it in 1..response.totalPorts }
            val free = (response.totalPorts - taken).coerceAtLeast(0)
            val olt = if (response.oltId != null) listOfNotNull("OLT ${response.oltId}", response.oltSlot).joinToString(" · ") else null
            val mufa = response.mufa?.takeIf { it.isNotBlank() }
            return NapDetail(
                id = response.id,
                code = response.code,
                address = response.address,
                latitude = response.latitude,
                longitude = response.longitude,
                freeLabel = "$free libres de ${response.totalPorts}",
                free = free,
                network = listOfNotNull(mufa?.let { "Mufa $it" }, olt).joinToString(" · "),
                rows = listOfNotNull(
                    response.address?.let { "Referencia" to it },
                    response.placeName?.let { "Lugar" to it.toTitleCase() },
                    mufa?.let { "Mufa" to listOfNotNull(it, response.mufaThreads?.let { t -> "$t hilos" }).joinToString(" · ") },
                    olt?.let { "OLT" to it },
                    if (response.latitude != null && response.longitude != null) {
                        "Coordenadas" to String.format(Locale.US, "%.4f, %.4f", response.latitude, response.longitude)
                    } else null,
                ),
                ports = (1..response.totalPorts).map { port -> NapPort(port, byPort[port]?.name?.let(::initials)) },
                holders = response.subscribers.map { sub ->
                    NapHolder(
                        subscriptionId = sub.subscriptionId,
                        initials = initials(sub.name),
                        name = sub.name.toTitleCase(),
                        detail = listOfNotNull(sub.port?.let { "Puerto $it" } ?: "Sin puerto", sub.plan?.let(::planLabel)).joinToString(" · "),
                        status = runCatching { ServiceStatus.valueOf(sub.status) }.getOrNull()?.badge() ?: StatusBadge(sub.status, com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone.NEUTRAL),
                        debt = sub.debt?.takeIf { it > 0 }?.let(::formatSoles),
                    )
                },
            )
        }

        private fun initials(name: String): String =
            name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
    }
}
