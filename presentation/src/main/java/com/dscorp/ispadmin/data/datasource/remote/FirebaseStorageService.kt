package com.dscorp.ispadmin.data.datasource.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface FirebaseStorageService {

    @Multipart
    @POST("o/")
    suspend fun uploadImage(
        @Part("uploadType") uploadType: RequestBody?,
        @Part("name") fileName: RequestBody?,
        @Part file: MultipartBody.Part?
    ): Response<Response<Any>>

}