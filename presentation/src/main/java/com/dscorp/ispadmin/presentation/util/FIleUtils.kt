package com.dscorp.ispadmin.presentation.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun getFileFromUri(context: Context, fileUri: Uri): File? {
    var inputStream: InputStream? = null
    var outputStream: FileOutputStream? = null
    val tempFile: File
    try {
        tempFile = File.createTempFile("tempFile", null, context.cacheDir)
        inputStream = context.contentResolver.openInputStream(fileUri)
        outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        inputStream?.close()
        outputStream?.close()
    }
    return tempFile
}