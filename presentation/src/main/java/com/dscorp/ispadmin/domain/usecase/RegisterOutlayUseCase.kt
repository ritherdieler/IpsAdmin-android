package com.dscorp.ispadmin.domain.usecase

import android.app.Application
import android.net.Uri
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.presentation.util.compressImage
import com.dscorp.ispadmin.presentation.util.getFileFromUri
import com.dscorp.ispadmin.presentation.util.rotateImageIfNeeded
import java.io.File
import java.io.FileOutputStream

class RegisterOutlayUseCase(
    private val repository: IRepository,
    private val application: Application
) {
    
    suspend operator fun invoke(
        outlay: Outlay,
        photoUriList: List<Uri>
    ): Result<Unit> = runCatching {
        if (!outlay.isValid()) {
            throw IllegalArgumentException("Datos incompletos")
        }
        val outlayWithResponsible = outlay.apply {
            responsibleId = repository.getUserSession()?.id 
        }
        val fileList = photoUriList.map { photoUri ->
            val file = getFileFromUri(application, photoUri)
                ?: throw IllegalStateException("File not found")
            rotateImageIfNeeded(application, file, photoUri)
                ?.compressImage(50)
                ?: throw IllegalStateException("Error processing image")
        }

        repository.saveOutLay(outlayWithResponsible, fileList)
    }
    
    private fun uriToFile(uri: Uri): File {
        val inputStream = application.contentResolver.openInputStream(uri)
        val file = File(application.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        
        return file
    }
}
