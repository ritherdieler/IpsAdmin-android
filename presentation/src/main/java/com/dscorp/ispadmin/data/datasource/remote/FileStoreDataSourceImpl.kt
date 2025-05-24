package com.dscorp.ispadmin.data.datasource.remote

import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class FileStoreDataSourceImpl(
    private val firebaseStorageService: FirebaseStorageService
) : FileStoreDataSource {

    override suspend fun uploadImage(imageUri: Uri) {

        val file = imageUri.path?.let { File(it) }
        if (file == null) throw Exception("File not found")

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val uploadType = "media".toMediaTypeOrNull()?.let { it ->
            "media".toRequestBody(it)
        }
        val name = file.name.toMediaTypeOrNull()?.let { it ->
            file.name.toRequestBody(it)
        }

        firebaseStorageService.uploadImage(
            uploadType = uploadType,
            fileName = name,
            file = body
        )

    }

    override suspend fun deleteImage(imageUrl: String) {

    }
}