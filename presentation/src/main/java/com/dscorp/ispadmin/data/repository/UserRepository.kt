package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.domain.model.User

interface UserRepository {
    suspend fun getTechnicianUsers(): List<User>
    suspend fun getCurrentUser(): User
} 