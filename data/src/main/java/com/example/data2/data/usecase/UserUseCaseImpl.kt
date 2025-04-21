package com.example.data2.data.usecase

import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import com.example.data2.data.repository.UserRepository

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