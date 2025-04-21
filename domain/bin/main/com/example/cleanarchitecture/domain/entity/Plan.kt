package com.example.cleanarchitecture.domain.entity

/**
 * Created by Sergio Carrillo Diestra on 23/11/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
data class Plan(

    val id: String? =null,
    val name:String="",
    val price: Double = 0.0,
    val downloadSpeed: String="",
    val uploadSpeed: String="",

    ):java.io.Serializable
{

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
    return id == (other as Plan).id
    }
}