package com.dscorp.ispadmin.presentation.ui.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.subscription.pending.PendingSubscriptionsTelemetry
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent

class MainActivityViewModel : ViewModel() {
    private val repository: IRepository by KoinJavaComponent.inject(IRepository::class.java)
    private val pendingTelemetry: PendingSubscriptionsTelemetry by KoinJavaComponent.inject(PendingSubscriptionsTelemetry::class.java)

    val user = repository.getUserSession()

    init {
        if (getCurrentUser() != null) {
            viewModelScope.launch { pendingTelemetry.reportOncePerProcess() }
        }
    }

    fun getCurrentUser(): User? {
        return repository.getUserSession()
    }
}
