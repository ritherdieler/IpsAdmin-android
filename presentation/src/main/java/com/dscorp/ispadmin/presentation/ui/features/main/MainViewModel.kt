package com.dscorp.ispadmin.presentation.ui.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import com.dscorp.ispadmin.presentation.navigation.DrawerItem
import com.dscorp.ispadmin.presentation.navigation.DrawerNavigation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class MainEvent {
    object LoadCurrentUser : MainEvent()
}

data class MainUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    fun getDrawerItems(): List<DrawerItem> =
        currentUser?.type?.let { DrawerNavigation.getDrawerItemsForUser(it) } ?: emptyList()
}

class MainViewModel(
    private val userUseCase: UserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.LoadCurrentUser -> loadCurrentUser()
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val user = userUseCase.getCurrentUser()
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar el usuario"
                    )
                }
            }
        }
    }
} 