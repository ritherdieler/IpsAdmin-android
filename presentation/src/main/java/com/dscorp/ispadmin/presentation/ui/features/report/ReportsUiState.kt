package com.dscorp.ispadmin.presentation.ui.features.report

import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse

sealed class ReportsUiState {
    class DocumentReady(val document: DownloadDocumentResponse) : ReportsUiState()
}
