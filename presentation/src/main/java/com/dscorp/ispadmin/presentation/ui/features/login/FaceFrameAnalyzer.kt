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

/**
 * Analiza los fotogramas de CameraX antes de tomar la foto.
 *
 * Esta clase NO reconoce personas y NO llama al backend.
 * Solamente autoriza la captura cuando existe un único rostro
 * centrado, suficientemente grande y estable.
 */
internal class FaceFrameAnalyzer(
    private val onStableFaceDetected: () -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        // Evita tomar una foto por una detección accidental de un solo fotograma.
        private const val REQUIRED_STABLE_FRAMES = 3

        // El rostro debe ocupar al menos 25% del ancho de la imagen.
        private const val MIN_FACE_WIDTH_RATIO = 0.25f

        // Permite una pequeña separación respecto al centro de la cámara.
        private const val MAX_CENTER_OFFSET_RATIO = 0.18f
    }

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

    // Impide procesar varios fotogramas al mismo tiempo.
    private val isProcessing = AtomicBoolean(false)

    // Cuenta cuántos fotogramas válidos consecutivos fueron detectados.
    private var stableFrames = 0

    // Evita ejecutar la captura más de una vez por intento.
    private var captureRequested = false

    /**
     * Recibe un fotograma de CameraX y lo entrega a ML Kit.
     * Siempre libera el ImageProxy al finalizar para evitar bloquear la cámara.
     */
    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (captureRequested || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            finishFrame(imageProxy)
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val validFace = faces.singleOrNull()?.let { face ->
                    val bounds = face.boundingBox
                    val imageWidth = inputImage.width.toFloat()
                    val imageHeight = inputImage.height.toFloat()

                    val faceWidthRatio = bounds.width() / imageWidth
                    val faceCenterX = bounds.centerX()
                    val faceCenterY = bounds.centerY()
                    val imageCenterX = imageWidth / 2f
                    val imageCenterY = imageHeight / 2f

                    val horizontalOffset = abs(faceCenterX - imageCenterX) / imageWidth
                    val verticalOffset = abs(faceCenterY - imageCenterY) / imageHeight

                    faceWidthRatio >= MIN_FACE_WIDTH_RATIO &&
                            horizontalOffset <= MAX_CENTER_OFFSET_RATIO &&
                            verticalOffset <= MAX_CENTER_OFFSET_RATIO
                } == true

                stableFrames = if (validFace) stableFrames + 1 else 0

                if (stableFrames >= REQUIRED_STABLE_FRAMES && !captureRequested) {
                    captureRequested = true
                    onStableFaceDetected()
                }
            }
            .addOnFailureListener {
                stableFrames = 0
            }
            .addOnCompleteListener {
                finishFrame(imageProxy)
            }
    }

    /**
     * Reinicia el analizador cuando el backend rechaza la foto
     * y el usuario desea realizar otro intento.
     */
    fun reset() {
        stableFrames = 0
        captureRequested = false
    }

    /**
     * Libera el detector de ML Kit al salir de la pantalla.
     */
    fun close() {
        detector.close()
    }

    /**
     * Libera el fotograma actual y permite analizar el siguiente.
     */
    private fun finishFrame(imageProxy: ImageProxy) {
        imageProxy.close()
        isProcessing.set(false)
    }
}