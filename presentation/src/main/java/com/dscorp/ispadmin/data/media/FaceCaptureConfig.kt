package com.dscorp.ispadmin.data.media

enum class FaceCaptureProfile {
    ENROLLMENT,
    VERIFICATION,
    EVIDENCE,
}

data class FaceCaptureSettings(
    val maxWidth: Int,
    val maxHeight: Int,
    val jpegQuality: Int,
    val targetSizeBytes: Int,
)

object FaceCaptureConfig {
    const val CAMERA_TARGET_WIDTH = 1280
    const val CAMERA_TARGET_HEIGHT = 720
    const val MIN_JPEG_QUALITY = 70
    const val JPEG_QUALITY_STEP = 5
    const val RESIZE_STEP = 0.9f
    const val MAX_ADAPTIVE_ITERATIONS = 3
    const val MIN_CAPTURE_WIDTH = 320
    const val MIN_CAPTURE_HEIGHT = 240

    fun settings(profile: FaceCaptureProfile): FaceCaptureSettings = when (profile) {
        FaceCaptureProfile.ENROLLMENT -> FaceCaptureSettings(
            maxWidth = 1280,
            maxHeight = 720,
            jpegQuality = 90,
            targetSizeBytes = 500 * 1024,
        )
        FaceCaptureProfile.VERIFICATION -> FaceCaptureSettings(
            maxWidth = 1280,
            maxHeight = 720,
            jpegQuality = 90,
            targetSizeBytes = 500 * 1024,
        )
        FaceCaptureProfile.EVIDENCE -> FaceCaptureSettings(
            maxWidth = 640,
            maxHeight = 480,
            jpegQuality = 80,
            targetSizeBytes = 200 * 1024,
        )
    }
}
