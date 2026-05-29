package com.dscorp.ispadmin.domain.repository

import java.io.File

interface OutlayReceiptPreparer {
    suspend fun prepareCompressedReceipts(contentUriStrings: List<String>): List<File>
}
