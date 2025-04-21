package com.dscorp.ispadmin.domain.model

data class User(
    val id: Int = -1,
    val name: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String? = null,
    val phone: String? = null,
    val dni: String? = null,
    val type: UserType? = null
) {
    enum class UserType {
        ADMIN, TECHNICIAN, CLIENT, SALES, SECRETARY, ACCOUNTANT
    }
    
    override fun toString(): String = "$name $lastName"
} 