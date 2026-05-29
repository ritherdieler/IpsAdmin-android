package com.dscorp.ispadmin.presentation.util

import android.content.Context
import android.net.Uri
import com.dscorp.ispadmin.data.media.compressJpegQuality
import com.dscorp.ispadmin.data.media.rotateImageIfNeeded as rotateImageIfNeededData
import java.io.File

fun File.compressImage(quality: Int): File = compressJpegQuality(this, quality)

fun rotateImageIfNeeded(context: Context, imageFile: File, imageUri: Uri): File? =
    rotateImageIfNeededData(context, imageFile, imageUri)
