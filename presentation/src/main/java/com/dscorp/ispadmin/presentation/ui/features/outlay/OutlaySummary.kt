package com.dscorp.ispadmin.presentation.ui.features.outlay

import com.dscorp.ispadmin.data.datasource.remote.OutlayApi

enum class LimitLevel { InRange, NearLimit, Reached }

/** Month figures of the Core: the cap of variable outlays, what is left, and the fixed outlays. */
data class OutlaySummary(
    val spent: Double,
    val limit: Double,
    val available: Double,
    val threshold: Double,
    val fixedTotal: Double,
    val previousFixedTotal: Double,
) {
    val usedFraction: Double get() = if (limit <= 0.0) 0.0 else (spent / limit).coerceIn(0.0, 1.0)

    val level: LimitLevel
        get() = when {
            limit > 0.0 && spent >= limit -> LimitLevel.Reached
            spent >= threshold -> LimitLevel.NearLimit
            else -> LimitLevel.InRange
        }

    /** Soles of difference between this month's fixed outlays and last month's. */
    val fixedChange: Double get() = fixedTotal - previousFixedTotal
}

interface OutlaySummaryGateway {
    suspend fun summary(): OutlaySummary
}

class ApiOutlaySummaryGateway(private val api: OutlayApi) : OutlaySummaryGateway {
    override suspend fun summary(): OutlaySummary = api.variableSummary().let {
        OutlaySummary(it.spent, it.limit, it.available, it.threshold, it.fixedTotal, it.previousFixedTotal)
    }
}
