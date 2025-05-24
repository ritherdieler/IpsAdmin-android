package com.dscorp.ispadmin.presentation.ui.features.mufas

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel
import org.koin.core.component.KoinComponent

class MufaViewModel(val repository: IRepository) : BaseViewModel<MufaUiState>(), KoinComponent {


    init {
        getMufas()
    }

    private fun getMufas() = executeWithProgress {
        val mufas = repository.getMufas()
        uiState.value = BaseUiState( MufaUiState.OnMufasListFound(mufas))
    }
}