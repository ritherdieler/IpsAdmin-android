package com.example.cleanarchitecture.domain.entity

data class DownloadDocumentResponse(
    val name: String,
    val type: String,
    val base64: String,
){
    fun getNameWithExtension():String = "$name.$type"
}