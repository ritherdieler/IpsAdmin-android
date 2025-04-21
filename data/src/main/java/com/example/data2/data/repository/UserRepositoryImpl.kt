package com.example.data2.data.repository

import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.User.UserType
import com.example.cleanarchitecture.domain.entity.User as LegacyUser

class UserRepositoryImpl(private val repository: IRepository) : UserRepository {
    
    override suspend fun getTechnicianUsers(): List<User> {
        // Simulamos obtener técnicos
        return listOf(
            User(
                id = 1,
                name = "Juan",
                lastName = "Pérez",
                username = "juanperez",
                type = UserType.TECHNICIAN
            ),
            User(
                id = 2,
                name = "Carlos",
                lastName = "Gómez",
                username = "carlosgomez",
                type = UserType.TECHNICIAN
            ),
            User(
                id = 3, 
                name = "Luis",
                lastName = "Rodríguez",
                username = "luisrodriguez",
                type = UserType.TECHNICIAN
            )
        )
    }
    
    override suspend fun getCurrentUser(): User {
        val legacyUser = repository.getUserSession() ?: throw IllegalStateException("No hay usuario activo")
        return mapLegacyUserToDomainUser(legacyUser)
    }
    
    private fun mapLegacyUserToDomainUser(legacyUser: LegacyUser): User {
        return User(
            id = legacyUser.id ?: -1,
            name = legacyUser.name,
            lastName = legacyUser.lastName,
            username = legacyUser.username,
            email = legacyUser.email,
            phone = legacyUser.phone,
            dni = legacyUser.dni,
            type = mapLegacyUserTypeToDomainUserType(legacyUser.type)
        )
    }
    
    private fun mapLegacyUserTypeToDomainUserType(legacyUserType: LegacyUser.UserType): UserType {
        return when (legacyUserType) {
            LegacyUser.UserType.ADMIN -> UserType.ADMIN
            LegacyUser.UserType.TECHNICIAN -> UserType.TECHNICIAN
            LegacyUser.UserType.CLIENT -> UserType.CLIENT
            LegacyUser.UserType.SALES -> UserType.SALES
            LegacyUser.UserType.SECRETARY -> UserType.SECRETARY
            LegacyUser.UserType.ACCOUNTANT -> UserType.ACCOUNTANT
            else -> UserType.CLIENT // Por defecto
        }
    }
} 