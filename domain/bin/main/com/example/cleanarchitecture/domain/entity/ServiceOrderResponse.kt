package com.example.cleanarchitecture.domain.entity

/**
 * Created by Sergio Carrillo Diestra on 27/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
data class ServiceOrderResponse(
    val id: Int? = null,
    val issue: String? = null,
    val subscriptionId: Int? = null,
    var userId:Int?=null,
    val additionalDetails : String?=null,
    var priority:Int?=null
):java.io.Serializable
{
    override fun equals(other: Any?): Boolean {
        return id == (other as ServiceOrderResponse).id
    }
}