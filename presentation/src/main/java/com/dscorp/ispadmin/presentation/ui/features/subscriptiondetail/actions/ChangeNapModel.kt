package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import com.dscorp.ispadmin.data.datasource.remote.NapOptionResponse
import com.dscorp.ispadmin.data.datasource.remote.NapOptionsResponse
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.distanceLabel
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase

enum class NapSort(val label: String) { NEAREST("Más cercanas"), FREEST("Más libres"), SAME_MUFA("Misma mufa") }
enum class PortState { TAKEN, FREE, CHOSEN }

data class NapRow(val id: Int, val code: String, val address: String?, val distance: String?, val freeLabel: String, val enabled: Boolean)
data class PortCell(val number: Int, val state: PortState)

/** a-sus-10: boxes of the place the subscription can move to, and their ports. */
data class ChangeNap(
    val current: String,
    val currentAddress: String?,
    val header: String,
    val releaseNote: String?,
    private val options: List<NapOptionResponse>,
    private val currentMufa: Int?,
) {
    fun rows(sort: NapSort): List<NapRow> {
        val sorted = when (sort) {
            NapSort.NEAREST -> options
            NapSort.FREEST -> options.sortedByDescending { it.free() }
            NapSort.SAME_MUFA -> options.filter { currentMufa != null && it.mufaId == currentMufa }
        }
        return sorted.map { box ->
            val free = box.free()
            NapRow(
                id = box.id,
                code = box.code,
                address = box.address,
                distance = box.distanceMeters?.let { distanceLabel(it.toDouble()) },
                freeLabel = if (free == 0) "Sin puertos" else "$free ${if (free == 1) "libre" else "libres"}",
                enabled = free > 0,
            )
        }
    }

    fun ports(napId: Int, chosen: Int?): List<PortCell> {
        val box = box(napId) ?: return emptyList()
        return (1..box.totalPorts).map { port ->
            PortCell(port, when { port in box.occupied -> PortState.TAKEN; port == chosen -> PortState.CHOSEN; else -> PortState.FREE })
        }
    }

    fun firstFree(napId: Int): Int? = box(napId)?.let { box -> (1..box.totalPorts).firstOrNull { it !in box.occupied } }
    fun isFree(napId: Int, port: Int): Boolean = box(napId)?.let { port in 1..it.totalPorts && port !in it.occupied } ?: false
    fun freeOf(napId: Int): String = box(napId)?.let { "${it.free()} libres de ${it.totalPorts}" }.orEmpty()
    fun code(napId: Int): String = box(napId)?.code.orEmpty()

    private fun box(napId: Int) = options.firstOrNull { it.id == napId }

    companion object {
        fun from(response: NapOptionsResponse): ChangeNap {
            val current = response.options.firstOrNull { it.id == response.currentNapId }
            val currentLabel = listOfNotNull(current?.code, response.currentPort?.let { "puerto $it" }).joinToString(" · ")
            return ChangeNap(
                current = currentLabel.ifBlank { "Sin caja NAP" },
                currentAddress = current?.address,
                header = listOfNotNull(
                    response.placeName?.takeIf { it.isNotBlank() }?.toTitleCase(),
                    "${response.options.size} cajas",
                    "distancia desde la ubicación del titular",
                ).joinToString(" · "),
                releaseNote = if (current != null && response.currentPort != null) "Se libera el puerto ${response.currentPort} de ${current.code}." else null,
                options = response.options.filter { it.id != response.currentNapId },
                currentMufa = current?.mufaId,
            )
        }

        private fun NapOptionResponse.free() = (totalPorts - occupied.count { it in 1..totalPorts }).coerceAtLeast(0)
    }
}
