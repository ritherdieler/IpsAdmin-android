package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.User

class GetUserSessionUseCase(private val repository: IRepository) {
    operator fun invoke(): Result<User?> = runCatching {
        repository.getUserSession()
    }
}
