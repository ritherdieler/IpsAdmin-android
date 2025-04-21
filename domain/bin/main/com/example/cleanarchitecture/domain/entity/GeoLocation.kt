package com.example.cleanarchitecture.domain.entity

/**
 * Created by Sergio Carrillo Diestra on 15/01/2023.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
data class GeoLocation(val latitude: Double =0.0, val longitude: Double=0.0):java.io.Serializable{
    override fun toString(): String {
        return "$longitude´'+$latitude"
    }
}