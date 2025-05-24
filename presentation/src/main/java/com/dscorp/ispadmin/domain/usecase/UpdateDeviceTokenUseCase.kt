package com.dscorp.ispadmin.domain.usecase

import com.dscorp.ispadmin.domain.model.User

interface UpdateDeviceTokenUseCase {
    suspend operator fun invoke(userId: Int, token: String): Result<User>
} 