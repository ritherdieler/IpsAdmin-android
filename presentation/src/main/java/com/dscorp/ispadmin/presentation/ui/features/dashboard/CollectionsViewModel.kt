package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.dashboard.CollectionSummary
import com.dscorp.ispadmin.domain.repository.CollectionsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectionsUiState(val summary: CollectionSummary? = null, val failed: Boolean = false)

sealed interface CollectionsIntent {
    data object Load : CollectionsIntent
}

/** Collection follow-up shared by the Financial (section) and Clients (debt per delinquent) tabs. */
class CollectionsViewModel(
    private val repository: CollectionsRepository,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val state = MutableStateFlow(CollectionsUiState())
    val uiState = state.asStateFlow()
    private var loadJob: Job? = null

    fun onIntent(intent: CollectionsIntent) {
        when (intent) {
            CollectionsIntent.Load -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(mainImmediate) {
            repository.summary().fold(
                onSuccess = { summary -> state.update { it.copy(summary = summary, failed = false) } },
                onFailure = { state.update { it.copy(failed = true) } },
            )
        }
    }
}
