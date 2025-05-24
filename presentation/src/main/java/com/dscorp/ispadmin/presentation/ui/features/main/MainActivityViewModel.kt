package com.dscorp.ispadmin.presentation.ui.features.main

import androidx.lifecycle.ViewModel
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.User
import org.koin.java.KoinJavaComponent

class MainActivityViewModel : ViewModel() {
    private val repository: IRepository by KoinJavaComponent.inject(IRepository::class.java)

    val user = repository.getUserSession()

    fun getCurrentUser(): User? {
        return repository.getUserSession()
    }
}
