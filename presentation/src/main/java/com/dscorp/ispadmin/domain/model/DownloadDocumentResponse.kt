package com.dscorp.ispadmin.domain.model

data class DownloadDocumentResponse(
    val name: String,
    val type: String,
    val base64: String,
){
    fun getNameWithExtension():String = "$name.$type"
}