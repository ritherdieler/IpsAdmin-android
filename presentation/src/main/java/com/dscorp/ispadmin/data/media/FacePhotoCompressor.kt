package com.dscorp.ispadmin.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object FacePhotoCompressor {
    fun compressPhotoForBackend(
        file: File,
        profile: FaceCaptureProfile = FaceCaptureProfile.VERIFICATION,
    ) {
        val settings = FaceCaptureConfig.settings(profile)
        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val oriented = rotateBitmapFromExif(file, original)
        val source = if (oriented !== original) {
            original.recycle()
            oriented
        } else {
            original
        }

        try {
            var maxWidth = settings.maxWidth
            var maxHeight = settings.maxHeight
            var quality = settings.jpegQuality

            for (iteration in 0..FaceCaptureConfig.MAX_ADAPTIVE_ITERATIONS) {
                val resized = resizeToMaxBounds(source, maxWidth, maxHeight)
                val shouldRecycleResized = resized !== source

                file.outputStream().use { output ->
                    resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
                }

                if (file.length() <= settings.targetSizeBytes) {
                    if (shouldRecycleResized) resized.recycle()
                    return
                }

                if (quality - FaceCaptureConfig.JPEG_QUALITY_STEP >= FaceCaptureConfig.MIN_JPEG_QUALITY) {
                    quality -= FaceCaptureConfig.JPEG_QUALITY_STEP
                    if (shouldRecycleResized) resized.recycle()
                    continue
                }

                if (iteration < FaceCaptureConfig.MAX_ADAPTIVE_ITERATIONS) {
                    maxWidth = max(
                        FaceCaptureConfig.MIN_CAPTURE_WIDTH,
                        (maxWidth * FaceCaptureConfig.RESIZE_STEP).roundToInt(),
                    )
                    maxHeight = max(
                        FaceCaptureConfig.MIN_CAPTURE_HEIGHT,
                        (maxHeight * FaceCaptureConfig.RESIZE_STEP).roundToInt(),
                    )
                    quality = settings.jpegQuality
                    if (shouldRecycleResized) resized.recycle()
                    continue
                }

                if (shouldRecycleResized) resized.recycle()
                return
            }
        } finally {
            source.recycle()
        }
    }

    private fun rotateBitmapFromExif(file: File, bitmap: Bitmap): Bitmap {
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun resizeToMaxBounds(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap

        val scale = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height).coerceAtMost(1f)
        if (scale >= 1f) return bitmap

        val targetWidth = max(1, (width * scale).roundToInt())
        val targetHeight = max(1, (height * scale).roundToInt())

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
