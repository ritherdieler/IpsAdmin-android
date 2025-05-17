package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.User
import com.example.data2.data.repository.IRepository

class GetUserSessionUseCase(private val repository: IRepository) {
    operator fun invoke(): Result<User?> = runCatching {
        repository.getUserSession()
    }
}
