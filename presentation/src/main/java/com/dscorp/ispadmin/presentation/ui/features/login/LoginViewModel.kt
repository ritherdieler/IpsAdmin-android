package com.dscorp.ispadmin.presentation.ui.features.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.data.extensions.encryptWithSHA384
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Loging
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.login.compose.LoginForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Empty : LoginState()
    object Loading : LoginState()
    data class Error(val message: String) : LoginState()
    data class LoginSuccess(val data: User) : LoginState()
}

sealed class CheckVersionState {
    object Loading : CheckVersionState()
    data class Error(val message: String) : CheckVersionState()
    data class CheckVersionSuccess(val forceUpdate: Boolean) : CheckVersionState()
}

class LoginViewModel(private val repository: IRepository) : ViewModel() {

    val loginRequestFlow = MutableStateFlow<LoginState>(LoginState.Empty)
    val checkVersionFlow = MutableStateFlow<CheckVersionState>(CheckVersionState.Loading)
    fun checkSessionStatus(): Pair<Boolean, User?> {
        val status = repository.getRememberSessionCheckBoxStatus()
        if (status) {
            repository.getUserSession()?.let {
                return Pair(true, it)
            }
            return Pair(false, null)
        }
        return Pair(false, null)
    }

    fun doLogin(loginData: LoginForm) = viewModelScope.launch {
        try {
            if (!loginData.isValid()) return@launch
            loginRequestFlow.value = LoginState.Loading
            val login = Loging(
                loginData.username,
                loginData.password.encryptWithSHA384(),
                loginData.checkedState
            )
            val response = repository.doLogin(login)
            loginRequestFlow.value = LoginState.LoginSuccess(response)

        } catch (e: Exception) {
            e.printStackTrace()
            loginRequestFlow.value = LoginState.Error(e.message ?: "Error desconocido")
        }
    }

    fun checkAppVersion() = viewModelScope.launch {
        try {
            val response = repository.getRemoteAppVersion()
            if (response.versionCode > BuildConfig.VERSION_CODE) {
                checkVersionFlow.value = CheckVersionState.CheckVersionSuccess(true)
            } else {
                checkVersionFlow.value = CheckVersionState.CheckVersionSuccess(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            checkVersionFlow.value = CheckVersionState.Error(e.message ?: "Error desconocido")
        }
    }

}
