package com.dscorp.ispadmin.data.datasource.remote

import android.net.Uri

interface FileStoreDataSource {

    suspend fun uploadImage(imageUri: Uri)
    suspend fun deleteImage(imageUrl: String)

}