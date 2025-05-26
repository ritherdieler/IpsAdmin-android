package com.dscorp.ispadmin.domain.model


data class User(
    val id: Int? = null,
    val name: String = "",
    val lastName: String = "",
    val type: UserType? = null,
    val username: String = "",
    var password: String = "",
    val verified: Boolean = false,
    val dni: String = "",
    val email: String = "",
    val phone: String = "",
) {
    enum class UserType(val value: String) {
        ADMIN("Administrador"),
        SALES("Ventas"),
        SECRETARY("Secretario"),
        ACCOUNTANT("Contador"),
        LOGISTIC("Logistica"),
        TECHNICIAN("Tecnico"),
        CLIENT("Cliente"),
    }

    fun typeAsString() = type?.value

    override fun toString(): String {
        return "${name.capitalize()} ${lastName.capitalize()}"
    }

}