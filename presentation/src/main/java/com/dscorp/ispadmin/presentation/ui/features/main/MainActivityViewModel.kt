package com.dscorp.ispadmin.presentation.ui.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.User
import com.example.data2.data.repository.IRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent

class MainActivityViewModel : ViewModel() {
    private val repository: IRepository by KoinJavaComponent.inject(IRepository::class.java)

    val user = repository.getUserSession()

    fun getCurrentUser(): User? {
        return repository.getUserSession()
    }
}
