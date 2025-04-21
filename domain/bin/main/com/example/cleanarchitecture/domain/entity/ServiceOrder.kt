package com.example.cleanarchitecture.domain.entity

/**
 * Created by Sergio Carrillo Diestra on 27/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
data class ServiceOrder(
    val id: Int?=null,
    val issue: String,
    val subscriptionId: Int?=null,
    var userId:Int?=null,
    val additionalDetails : String?=null,
    val priority:Int?=null,
    val createdByUserId: Int?=null
)
