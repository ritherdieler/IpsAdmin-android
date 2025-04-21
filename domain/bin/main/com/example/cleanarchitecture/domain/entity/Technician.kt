package com.example.cleanarchitecture.domain.entity

/**
 * Created by Sergio Carrillo Diestra on 26/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
data class Technician(
    val id: Int? = null,
    val name: String="",
    val lastName: String="",
    val dni: String="",
    val type: String="",
    val username: String="",
    val password: String="",
    val verified: Boolean = false,
    val address: String="",
    val phone: String="",
    val birthday: Long=0
):java.io.Serializable {
    override fun toString() = "$name $lastName"
}