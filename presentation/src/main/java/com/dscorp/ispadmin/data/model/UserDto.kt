package com.dscorp.ispadmin.data.model

import com.dscorp.ispadmin.domain.model.User

data class UserDto(
    val id: Int? = null,
    val name: String? = null,
    val lastName: String? = null,
    val type: User.UserType? = null,
    val username: String? = null,
    val verified: Boolean? = null,
    val email: String? = null,
    val phone: String? = null,
    val dni: String? = null,
) {
    internal fun toDomain(): User? {
        return if (id != null && name != null && lastName != null && type != null) {
            User(
                id = id,
                name = name,
                lastName = lastName,
                type = type,
                username = username ?: "",
                verified = verified ?: false,
                email = email ?: "",
                phone = phone ?: "",
                dni = dni ?: ""
            )
        } else {
            null
        }
    }
}