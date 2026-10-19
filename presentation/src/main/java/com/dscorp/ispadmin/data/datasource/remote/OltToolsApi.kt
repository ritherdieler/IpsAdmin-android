package com.dscorp.ispadmin.data.datasource.remote

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Core «Herramientas OLT» (admins): find an ONU, read it live through the Gateway, delete it. */
interface OltToolsApi {
    @GET("onu/lookup")
    suspend fun lookup(@Query("sn") sn: String): Response<OnuLookupResponse>

    @GET("onu/configured/{externalId}/status")
    suspend fun liveStatus(@Path("externalId") externalId: String): OnuLiveStatusResponse

    /** Irreversible: removes the ONU configuration from the OLT. */
    @DELETE("onu")
    suspend fun delete(@Query("onuExternalId") externalId: String): Response<Unit>
}

data class OnuLinkedSubscriptionResponse(val id: Int = 0, val name: String = "", val status: String = "")
data class OnuLookupResponse(
    val sn: String = "",
    val externalId: String = "",
    val model: String? = null,
    val oltName: String? = null,
    val slot: String? = null,
    val onuId: String? = null,
    val authorizedAt: String? = null,
    val subscription: OnuLinkedSubscriptionResponse? = null,
)
data class OnuLiveStatusResponse(val runState: String? = null, val onuRxDbm: Double? = null)
