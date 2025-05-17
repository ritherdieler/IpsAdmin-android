package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.lifecycle.MutableLiveData
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel
import com.example.data2.data.extensions.encryptWithSHA384
import com.example.data2.data.repository.IRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DashBoardViewModel : BaseViewModel<DashBoardDataUiState>(), KoinComponent {
    private val repository: IRepository by inject()
    val userSession = repository.getUserSession()
    val showDashBoardShimmerLiveData = MutableLiveData(true)

    init {
        getDashBoardData()
    }

    fun getDashBoardData() = executeNoProgress (doFinally = {
        showDashBoardShimmerLiveData.value = false
    }) {
        val response = repository.getDashBoardData()
        uiState.value = BaseUiState(DashBoardDataUiState.DashBoardData(response))
    }

}