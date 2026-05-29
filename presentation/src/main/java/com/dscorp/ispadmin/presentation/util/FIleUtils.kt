package com.dscorp.ispadmin.presentation.util

import android.content.Context
import android.net.Uri
import com.dscorp.ispadmin.data.media.getFileFromUri as getFileFromUriData
import java.io.File

fun getFileFromUri(context: Context, fileUri: Uri): File? =
    getFileFromUriData(context, fileUri)
