package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.data.datasource.remote.RestApiServices

class UserRepositoryImpl(
    private val repository: IRepository,
    private val restApiServices: RestApiServices
) : UserRepository {

    override suspend fun getTechnicianUsers(): List<User> {
       return repository.getTechnicians()
    }

    override suspend fun getCurrentUser(): User {
        return repository.getUserSession() ?: throw IllegalStateException("No hay usuario activo")
    }

} 