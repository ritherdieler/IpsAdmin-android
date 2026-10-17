package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.datasource.remote.dashboard.ClientsDashboardApi
import com.dscorp.ispadmin.domain.model.dashboard.ClientsDashboard
import com.dscorp.ispadmin.domain.repository.ClientsDashboardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import retrofit2.Response

/** History window of the Clients tab: tickets, zone incidents and cancellations. Open tickets are always current. */
const val CLIENTS_WINDOW_MONTHS = 6

class ClientsDashboardRepositoryImpl(private val api: ClientsDashboardApi) : ClientsDashboardRepository {
    override suspend fun load(): Result<ClientsDashboard> = coroutineScope {
        val plans = async { section { api.planAnalysis() }?.toDomain() }
        val tickets = async { section { api.assistanceTickets(CLIENTS_WINDOW_MONTHS) }?.toDomain() }
        val quality = async { section { api.clientQuality() }?.toDomain() }
        val zones = async { section { api.geographicPerformance(CLIENTS_WINDOW_MONTHS) }?.toDomain() }
        val lifecycle = async { section { api.clientLifecycle(CLIENTS_WINDOW_MONTHS) }?.toDomain() }
        val dashboard = ClientsDashboard(plans.await(), tickets.await(), quality.await(), zones.await(), lifecycle.await())
        val allFailed = listOf(dashboard.plans, dashboard.tickets, dashboard.quality, dashboard.zones, dashboard.lifecycle).all { it == null }
        if (allFailed) Result.failure(IllegalStateException("Clients dashboard unavailable")) else Result.success(dashboard)
    }

    /** One section; null on any failure except cancellation. */
    private suspend fun <T> section(call: suspend () -> Response<T>): T? = try {
        val response = call()
        if (!response.isSuccessful) throw HttpException(response)
        response.body()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
