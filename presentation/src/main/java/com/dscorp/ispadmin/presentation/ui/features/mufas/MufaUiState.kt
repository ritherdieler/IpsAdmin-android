package com.dscorp.ispadmin.presentation.ui.features.mufas

import com.dscorp.ispadmin.domain.model.Mufa

sealed class MufaUiState {

    class OnMufasListFound(val mufasList: List<Mufa>) : MufaUiState()
}
