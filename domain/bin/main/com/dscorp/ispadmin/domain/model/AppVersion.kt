package com.dscorp.ispadmin.domain.model

data class AppVersion(
    val id: Int,
    val versionCode: Int,
    val versionName: String?=null,
    val releaseDate: String,
    val description: String?=null,
    val downloadUrl: String?=null,
)