package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.CpeReading
import retrofit2.http.GET
import retrofit2.http.Path

/** Reads the subscription ficha needs beyond the classic endpoints. */
interface FichaApi {
    /** Core service-health reading of the subscriber's ONU (ADMIN/TECHNICIAN). */
    @GET("subscription/{id}/cpe-status")
    suspend fun cpeStatus(@Path("id") subscriptionId: Int): CpeStatusResponse

    @GET("subscription/{id}/history")
    suspend fun history(@Path("id") subscriptionId: Int): List<com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.HistoryLog>

    @GET("assistanceTicket/bySubscription/{id}")
    suspend fun ticketsBySubscription(@Path("id") subscriptionId: Int): List<com.dscorp.ispadmin.data.response.AssistanceTicketResponse>
}

data class CpeStatusResponse(
    val online: Boolean = false,
    val rxDbm: String? = null,
    val lastInform: String? = null,
    val sn: String? = null,
    val capabilities: CpeCapabilitiesResponse? = null,
) {
    fun toReading(): CpeReading = CpeReading(
        online = online,
        rxDbm = rxDbm?.toDoubleOrNull(),
        lastInform = lastInform?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() },
        model = listOfNotNull(capabilities?.vendor, capabilities?.model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifEmpty { null },
    )
}

data class CpeCapabilitiesResponse(val vendor: String? = null, val model: String? = null)
