package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.dashboard.ClientsDashboard

interface ClientsDashboardRepository {
    /** Fails only when every section fails; otherwise failed sections are null. */
    suspend fun load(): Result<ClientsDashboard>
}
