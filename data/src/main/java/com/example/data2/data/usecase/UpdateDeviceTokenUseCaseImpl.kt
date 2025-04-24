package com.example.data2.data.usecase

import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.UpdateDeviceTokenUseCase
import com.example.data2.data.repository.IRepository

class UpdateDeviceTokenUseCaseImpl(
    private val repository: IRepository
) : UpdateDeviceTokenUseCase {
    
    override suspend operator fun invoke(userId: Int, token: String): Result<User> = runCatching {
        repository.updateDeviceToken(userId, token)
    }
} 