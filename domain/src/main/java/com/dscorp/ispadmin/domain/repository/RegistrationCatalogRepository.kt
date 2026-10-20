package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.RegistrationCatalog

interface RegistrationCatalogRepository {
    suspend fun fetchCatalog(): Result<RegistrationCatalog>
}
