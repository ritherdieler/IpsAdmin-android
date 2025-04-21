package com.dscorp.ispadmin.domain.usecase

import com.dscorp.ispadmin.domain.model.User

interface UserUseCase {
    suspend fun getTechnicianUsers(): List<User>
    suspend fun getCurrentUser(): User
} 