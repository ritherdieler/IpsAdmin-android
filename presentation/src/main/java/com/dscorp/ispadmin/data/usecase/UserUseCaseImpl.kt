package com.dscorp.ispadmin.data.usecase

import com.dscorp.ispadmin.data.repository.UserRepository
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.UserUseCase

class UserUseCaseImpl(
    private val repository: UserRepository
) : UserUseCase {
    
    override suspend fun getTechnicianUsers(): List<User> {
        return repository.getTechnicianUsers()
    }
    
    override suspend fun getCurrentUser(): User {
        return repository.getCurrentUser()
    }
} 