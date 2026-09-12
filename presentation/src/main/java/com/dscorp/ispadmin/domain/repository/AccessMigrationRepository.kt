package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.AccessMigrationProgress

interface AccessMigrationRepository {
    suspend fun startMigration(subscriptionId: Int): AccessMigrationProgress

    suspend fun getProgress(subscriptionId: Int): AccessMigrationProgress
}
