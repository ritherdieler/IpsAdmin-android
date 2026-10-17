package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.dashboard.CollectionSummary

interface CollectionsRepository {
    suspend fun summary(): Result<CollectionSummary>
}
