package com.dscorp.ispadmin.presentation.ui.features.dashboard

import com.dscorp.ispadmin.domain.model.DashBoardDataResponse

sealed class DashBoardDataUiState {
    object CutServiceSuccess : DashBoardDataUiState()
    class DashBoardData(val response: DashBoardDataResponse) : DashBoardDataUiState()

    class InvalidPasswordError(val error: String) : DashBoardDataUiState()
}
