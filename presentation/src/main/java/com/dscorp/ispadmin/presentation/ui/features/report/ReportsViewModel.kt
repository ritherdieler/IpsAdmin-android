package com.dscorp.ispadmin.presentation.ui.features.report

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel

class ReportsViewModel(private val repository: IRepository) : BaseViewModel<ReportsUiState>() {

    fun downloadDebtorWithActiveSubscriptionsReport() = executeWithProgress {
        val downloadedDocument = repository.downloadDebtorWithActiveSubscriptionsReport()
        uiState.value =
            BaseUiState(ReportsUiState.DocumentReady(downloadedDocument))
    }

    fun downloadPaymentCommitmentSubscriptionsReport() = executeWithProgress {
        val downloadedDocument = repository.downloadPaymentCommitmentSubscriptionsReport()
        uiState.value =
            BaseUiState(ReportsUiState.DocumentReady(downloadedDocument))
    }

    fun downloadSuspendedSubscriptionsReport() = executeWithProgress {
        val downloadedDocument = repository.downloadSuspendedSubscriptionsReport()
        uiState.value = BaseUiState(ReportsUiState.DocumentReady(downloadedDocument))
    }

    fun downloadCutOffSubscriptionsReport() = executeWithProgress {
        val downloadedDocument = repository.downloadCutOffSubscriptionsReport()
        uiState.value =
            BaseUiState(ReportsUiState.DocumentReady(downloadedDocument))
    }

    fun downloadPastMonthDebtorsReport() = executeWithProgress {
        val downloadedDocument = repository.downloadPastMonthDebtorsReport()
        uiState.value =
            BaseUiState(ReportsUiState.DocumentReady(downloadedDocument))
    }

    fun downloadCancelledSubscriptionsFromCurrentMonthReport() = executeWithProgress {
        val downloadedDocument = repository.downloadCancelledSubscriptionsFromCurrentMonthReport()
        uiState.value =
            BaseUiState(ReportsUiState.DocumentReady(downloadedDocument))
    }

    fun downloadCancelledSubscriptionsFromPastMonthReport() = executeWithProgress {
        val downloadedDocument = repository.downloadCancelledSubscriptionsFromPastMonthReport()
        uiState.value =
            BaseUiState(ReportsUiState.DocumentReady(downloadedDocument))
    }

    fun downloadDebtorsCutOffCandidatesSubscriptionsReport() = executeWithProgress {
        val downloadedDocument = repository.downloadDebtorsCutOffCandidatesSubscriptionsReport()
        uiState.value =
            BaseUiState(ReportsUiState.DocumentReady(downloadedDocument))
    }

    fun downloadDebtorWithCancelledSubscriptionsReport() = executeWithProgress {
        val downloadedDocument = repository.downloadDebtorWithCancelledSubscriptionsReport()
        uiState.value =
            BaseUiState(ReportsUiState.DocumentReady(downloadedDocument))
    }
}