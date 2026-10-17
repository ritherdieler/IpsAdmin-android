package com.dscorp.ispadmin.presentation.ui.features.report

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import com.dscorp.ispadmin.observability.ObsBreadcrumbCategory
import com.dscorp.ispadmin.observability.ObservabilityClient
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel

class ReportsViewModel(
    private val repository: IRepository,
    private val observabilityClient: ObservabilityClient
) : BaseViewModel<ReportsUiState>() {

    private companion object {
        const val OBS_FEATURE = "report"
        const val OBS_SCREEN = "reports"
    }

    private fun downloadReport(action: String, request: suspend () -> DownloadDocumentResponse) = executeWithProgress {
        observabilityClient.addBreadcrumb(
            category = ObsBreadcrumbCategory.USER_ACTION,
            message = "$OBS_FEATURE.$action",
            data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to action)
        )
        try {
            val downloadedDocument = request()
            uiState.value = BaseUiState(ReportsUiState.DocumentReady(downloadedDocument))
        } catch (e: Exception) {
            observabilityClient.reportError(
                throwable = e,
                message = "Fallo al descargar reporte",
                tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to action)
            )
            throw e
        }
    }

    fun downloadDebtorWithActiveSubscriptionsReport() =
        downloadReport("debtor_active") { repository.downloadDebtorWithActiveSubscriptionsReport() }


    fun downloadCutOffSubscriptionsReport() =
        downloadReport("cut_off") { repository.downloadCutOffSubscriptionsReport() }

    fun downloadPastMonthDebtorsReport() =
        downloadReport("past_month_debtors") { repository.downloadPastMonthDebtorsReport() }

    fun downloadCancelledSubscriptionsFromCurrentMonthReport() =
        downloadReport("cancelled_current_month") { repository.downloadCancelledSubscriptionsFromCurrentMonthReport() }

    fun downloadCancelledSubscriptionsFromPastMonthReport() =
        downloadReport("cancelled_past_month") { repository.downloadCancelledSubscriptionsFromPastMonthReport() }

    fun downloadDebtorsCutOffCandidatesSubscriptionsReport() =
        downloadReport("cut_off_candidates") { repository.downloadDebtorsCutOffCandidatesSubscriptionsReport() }

    fun downloadDebtorWithCancelledSubscriptionsReport() =
        downloadReport("debtor_cancelled") { repository.downloadDebtorWithCancelledSubscriptionsReport() }
}