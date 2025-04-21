package com.dscorp.ispadmin.domain.model


data class User(
    val id: Int? = null,
    val name: String,
    val lastName: String,
    val type: UserType,
    val username: String,
    var password: String,
    val verified: Boolean,
    val dni: String,
    val email: String,
    val phone: String,
) {
    enum class UserType(val value: String) {
        ADMIN("Administrador"),
        TECHNICIAN("Tecnico"),
        CLIENT("Cliente"),
        LOGISTIC("Logistica"),
        SALES("Ventas"),
        SECRETARY("Secretario"),
        ACCOUNTANT("Contador"),
    }

    fun typeAsString() = type.value

    override fun toString(): String {
        return "${name.capitalize()} ${lastName.capitalize()}"
    }

}