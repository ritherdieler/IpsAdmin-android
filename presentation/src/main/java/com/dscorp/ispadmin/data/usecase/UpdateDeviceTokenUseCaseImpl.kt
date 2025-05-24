package com.dscorp.ispadmin.data.usecase

import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.UpdateDeviceTokenUseCase
import com.dscorp.ispadmin.data.repository.IRepository

class UpdateDeviceTokenUseCaseImpl(
    private val repository: IRepository
) : UpdateDeviceTokenUseCase {
    
    override suspend operator fun invoke(userId: Int, token: String): Result<User> = runCatching {
        repository.updateDeviceToken(userId, token)
    }
} 