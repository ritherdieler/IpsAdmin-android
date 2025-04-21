package com.example.cleanarchitecture.domain.entity

import java.util.Date

data class Outlay(
    val amount: Double,
    val description: String,
    val document_code:String,
    val date: Date? = null,
    var responsibleId: Int? = null,
    val responsibleName: String?= null
){
    fun isValid() = amount > 0 && description.isNotEmpty()
}