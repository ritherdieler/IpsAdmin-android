package com.dscorp.ispadmin.data.repository

import android.content.Context
import android.net.Uri
import com.dscorp.ispadmin.data.media.compressJpegQuality
import com.dscorp.ispadmin.data.media.getFileFromUri
import com.dscorp.ispadmin.data.media.rotateImageIfNeeded
import com.dscorp.ispadmin.domain.repository.OutlayReceiptPreparer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OutlayReceiptPreparerImpl(
    private val context: Context
) : OutlayReceiptPreparer {

    override suspend fun prepareCompressedReceipts(contentUriStrings: List<String>): List<File> =
        withContext(Dispatchers.IO) {
            contentUriStrings.map { uriString ->
                val uri = Uri.parse(uriString)
                val file = getFileFromUri(context, uri)
                    ?: throw IllegalStateException("File not found")
                rotateImageIfNeeded(context, file, uri)
                    ?.let { compressJpegQuality(it, 50) }
                    ?: throw IllegalStateException("Error processing image")
            }
        }
}
