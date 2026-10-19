package com.dscorp.ispadmin.presentation.ui.features.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

private val MONTHS = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "setiembre", "octubre", "noviembre", "diciembre")

data class ReportItem(val type: DownloadDocumentType, val title: String, val subtitle: String)

/** a-mas-07: the Excel reports of the Core, in the order collection uses them. */
fun reportItems(today: LocalDate): List<ReportItem> {
    val last = today.minusMonths(1)
    return listOf(
        ReportItem(DownloadDocumentType.DEBTORS_CUT_OFF_CANDIDATES, "Candidatos a corte", "Recibos impagos antes del día 15"),
        ReportItem(DownloadDocumentType.CUT_OFF, "Cortados", "Abonados en la lista de deudores"),
        ReportItem(DownloadDocumentType.DEBTORS_FROM_PAST_MONTH, "Por cobrar del mes anterior", "Recibos de ${MONTHS[last.monthValue - 1]} aún sin pagar"),
        ReportItem(DownloadDocumentType.DEBTORS_WITH_ACTIVE_SERVICE, "Deudores con servicio activo", "Activas y con recibos pendientes"),
        ReportItem(DownloadDocumentType.DEBTORS_WITH_CANCELLED_SERVICE, "Deudores con servicio cancelado", "Canceladas que dejaron deuda"),
        ReportItem(DownloadDocumentType.CANCELLED_SUBSCRIPTIONS_FROM_CURRENT_MONTH, "Suscripciones canceladas de este mes",
            "Bajas de ${MONTHS[today.monthValue - 1]} ${today.year}"),
        ReportItem(DownloadDocumentType.CANCELLED_SUBSCRIPTIONS_FROM_PAST_MONTH, "Suscripciones canceladas del mes pasado",
            "Bajas de ${MONTHS[last.monthValue - 1]} ${last.year}"),
    )
}

interface ReportsGateway {
    suspend fun download(type: DownloadDocumentType): DownloadDocumentResponse
}

class RepositoryReportsGateway(private val repository: IRepository) : ReportsGateway {
    override suspend fun download(type: DownloadDocumentType): DownloadDocumentResponse = when (type) {
        DownloadDocumentType.DEBTORS_CUT_OFF_CANDIDATES -> repository.downloadDebtorsCutOffCandidatesSubscriptionsReport()
        DownloadDocumentType.DEBTORS_WITH_ACTIVE_SERVICE -> repository.downloadDebtorWithActiveSubscriptionsReport()
        DownloadDocumentType.DEBTORS_WITH_CANCELLED_SERVICE -> repository.downloadDebtorWithCancelledSubscriptionsReport()
        DownloadDocumentType.CUT_OFF -> repository.downloadCutOffSubscriptionsReport()
        DownloadDocumentType.DEBTORS_FROM_PAST_MONTH -> repository.downloadPastMonthDebtorsReport()
        DownloadDocumentType.CANCELLED_SUBSCRIPTIONS_FROM_CURRENT_MONTH -> repository.downloadCancelledSubscriptionsFromCurrentMonthReport()
        DownloadDocumentType.CANCELLED_SUBSCRIPTIONS_FROM_PAST_MONTH -> repository.downloadCancelledSubscriptionsFromPastMonthReport()
    }
}

data class ReportsUiState(
    val items: List<ReportItem> = emptyList(),
    val generating: Set<DownloadDocumentType> = emptySet(),
    val error: String? = null,
    /** File ready to be saved and opened by the screen. */
    val ready: DownloadDocumentResponse? = null,
)

sealed interface ReportsIntent {
    data class Download(val type: DownloadDocumentType) : ReportsIntent
    data object Opened : ReportsIntent
    data object ErrorShown : ReportsIntent
}

class ReportsViewModel(
    private val gateway: ReportsGateway,
    private val mainImmediate: CoroutineDispatcher,
    today: () -> LocalDate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportsUiState(items = reportItems(today())))
    val uiState: StateFlow<ReportsUiState> = _uiState

    fun onIntent(intent: ReportsIntent) {
        when (intent) {
            is ReportsIntent.Download -> download(intent.type)
            ReportsIntent.Opened -> _uiState.update { it.copy(ready = null) }
            ReportsIntent.ErrorShown -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun download(type: DownloadDocumentType) {
        if (type in _uiState.value.generating) return
        _uiState.update { it.copy(generating = it.generating + type, error = null) }
        viewModelScope.launch(mainImmediate) {
            try {
                val document = gateway.download(type)
                _uiState.update { it.copy(generating = it.generating - type, ready = document) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val title = _uiState.value.items.firstOrNull { it.type == type }?.title.orEmpty()
                _uiState.update {
                    it.copy(generating = it.generating - type, error = "No se pudo generar «$title». Revisa tu conexión y vuelve a intentar.")
                }
            }
        }
    }
}
