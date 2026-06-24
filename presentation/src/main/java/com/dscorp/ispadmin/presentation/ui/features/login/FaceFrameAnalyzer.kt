package com.dscorp.ispadmin.presentation.ui.features.login

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

data class FaceCaptureStatus(
    val message: String,
    val faceWidthPercent: Int?,
    val ready: Boolean
)

internal class FaceFrameAnalyzer(
    private val onStatusUpdate: (FaceCaptureStatus) -> Unit,
    private val onStableFaceDetected: () -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val REQUIRED_STABLE_FRAMES = 3
        private const val MIN_FACE_WIDTH_RATIO = 0.25f
        private const val MAX_FACE_WIDTH_RATIO = 0.72f
        private const val MAX_CENTER_OFFSET_RATIO = 0.18f
        // Reto activo de liveness: secuencia de giro de cabeza (yaw) que una foto estatica no reproduce.
        private const val CHALLENGE_CENTER_YAW_DEGREES = 9f
        private const val CHALLENGE_TURN_YAW_DEGREES = 18f
        private const val REQUIRED_CENTER_HITS = 2
        private const val REQUIRED_TURN_HITS = 1
        private const val REQUIRED_RECENTER_HITS = 2
    }

    private enum class ChallengeStage { CENTER, TURN, RECENTER, PASSED }

    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .enableTracking()
            .setMinFaceSize(MIN_FACE_WIDTH_RATIO)
            .build()
    )

    private val isProcessing = AtomicBoolean(false)
    private var stableFrames = 0
    private var captureRequested = false
    private var challengeStage = ChallengeStage.CENTER
    private var centerHits = 0
    private var turnHits = 0
    private var recenterHits = 0

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (captureRequested || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            onStatusUpdate(
                FaceCaptureStatus(
                    message = "Coloca tu rostro dentro del ovalo",
                    faceWidthPercent = null,
                    ready = false
                )
            )
            finishFrame(imageProxy)
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    stableFrames = 0
                    onStatusUpdate(
                        FaceCaptureStatus(
                            message = "No se detecta rostro. Centra tu cara en el ovalo",
                            faceWidthPercent = null,
                            ready = false
                        )
                    )
                    return@addOnSuccessListener
                }

                if (faces.size > 1) {
                    stableFrames = 0
                    onStatusUpdate(
                        FaceCaptureStatus(
                            message = "Solo debe aparecer una persona",
                            faceWidthPercent = null,
                            ready = false
                        )
                    )
                    return@addOnSuccessListener
                }

                val face = faces.first()

                // Antes de habilitar la captura, exige el reto de giro de cabeza (liveness activo).
                if (challengeStage != ChallengeStage.PASSED) {
                    stableFrames = 0
                    val challengeMessage = advanceChallenge(face.headEulerAngleY)
                    onStatusUpdate(
                        FaceCaptureStatus(
                            message = challengeMessage,
                            faceWidthPercent = null,
                            ready = false
                        )
                    )
                    return@addOnSuccessListener
                }

                val bounds = face.boundingBox
                val imageWidth = inputImage.width.toFloat()
                val imageHeight = inputImage.height.toFloat()
                val faceWidthRatio = bounds.width() / imageWidth
                val faceWidthPercent = (faceWidthRatio * 100).toInt()
                val faceCenterX = bounds.centerX()
                val faceCenterY = bounds.centerY()
                val imageCenterX = imageWidth / 2f
                val imageCenterY = imageHeight / 2f
                val horizontalOffset = abs(faceCenterX - imageCenterX) / imageWidth
                val verticalOffset = abs(faceCenterY - imageCenterY) / imageHeight

                val validFace = faceWidthRatio in MIN_FACE_WIDTH_RATIO..MAX_FACE_WIDTH_RATIO &&
                    horizontalOffset <= MAX_CENTER_OFFSET_RATIO &&
                    verticalOffset <= MAX_CENTER_OFFSET_RATIO

                val status = when {
                    faceWidthRatio < MIN_FACE_WIDTH_RATIO -> FaceCaptureStatus(
                        message = "Acercate un poco (${faceWidthPercent}% / min 25%)",
                        faceWidthPercent = faceWidthPercent,
                        ready = false
                    )
                    faceWidthRatio > MAX_FACE_WIDTH_RATIO -> FaceCaptureStatus(
                        message = "Alejate un poco (${faceWidthPercent}%)",
                        faceWidthPercent = faceWidthPercent,
                        ready = false
                    )
                    horizontalOffset > MAX_CENTER_OFFSET_RATIO || verticalOffset > MAX_CENTER_OFFSET_RATIO -> FaceCaptureStatus(
                        message = "Centra tu rostro en el ovalo",
                        faceWidthPercent = faceWidthPercent,
                        ready = false
                    )
                    stableFrames + 1 < REQUIRED_STABLE_FRAMES -> FaceCaptureStatus(
                        message = "Mantente quieto (${faceWidthPercent}%)",
                        faceWidthPercent = faceWidthPercent,
                        ready = false
                    )
                    else -> FaceCaptureStatus(
                        message = "Rostro listo, capturando...",
                        faceWidthPercent = faceWidthPercent,
                        ready = true
                    )
                }

                onStatusUpdate(status)
                stableFrames = if (validFace) stableFrames + 1 else 0

                if (stableFrames >= REQUIRED_STABLE_FRAMES && !captureRequested) {
                    captureRequested = true
                    onStableFaceDetected()
                }
            }
            .addOnFailureListener {
                stableFrames = 0
                onStatusUpdate(
                    FaceCaptureStatus(
                        message = "No se pudo analizar la camara",
                        faceWidthPercent = null,
                        ready = false
                    )
                )
            }
            .addOnCompleteListener {
                finishFrame(imageProxy)
            }
    }

    // Avanza la maquina de estados del reto usando el angulo yaw (giro horizontal) que entrega ML Kit.
    // Se acepta el giro a cualquier lado para evitar rechazos por el espejo de la camara frontal.
    private fun advanceChallenge(yaw: Float): String {
        val absYaw = abs(yaw)
        when (challengeStage) {
            ChallengeStage.CENTER -> {
                centerHits = if (absYaw < CHALLENGE_CENTER_YAW_DEGREES) centerHits + 1 else 0
                if (centerHits >= REQUIRED_CENTER_HITS) challengeStage = ChallengeStage.TURN
            }
            ChallengeStage.TURN -> {
                turnHits = if (absYaw > CHALLENGE_TURN_YAW_DEGREES) turnHits + 1 else 0
                if (turnHits >= REQUIRED_TURN_HITS) challengeStage = ChallengeStage.RECENTER
            }
            ChallengeStage.RECENTER -> {
                recenterHits = if (absYaw < CHALLENGE_CENTER_YAW_DEGREES) recenterHits + 1 else 0
                if (recenterHits >= REQUIRED_RECENTER_HITS) challengeStage = ChallengeStage.PASSED
            }
            ChallengeStage.PASSED -> {}
        }

        return when (challengeStage) {
            ChallengeStage.CENTER -> "Mira de frente a la camara"
            ChallengeStage.TURN -> "Gira lentamente la cabeza a un lado"
            ChallengeStage.RECENTER -> "Ahora vuelve al centro"
            ChallengeStage.PASSED -> "Reto completado, manten la posicion..."
        }
    }

    fun reset() {
        stableFrames = 0
        captureRequested = false
        challengeStage = ChallengeStage.CENTER
        centerHits = 0
        turnHits = 0
        recenterHits = 0
    }

    fun close() {
        detector.close()
    }

    private fun finishFrame(imageProxy: ImageProxy) {
        imageProxy.close()
        isProcessing.set(false)
    }
}
