package com.dscorp.ispadmin.presentation.ui.features.login

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import androidx.camera.core.ImageAnalysis

private const val FACE_LOGIN_TIMEOUT_MS = 20_000L
private const val FACE_PHOTO_MAX_SIZE = 480
private const val FACE_PHOTO_JPEG_QUALITY = 72

@Composable
fun FaceLoginScreen(
    onFacePhotoCaptured: (File) -> Unit,
    onCancel: () -> Unit,
    retryTrigger: Int = 0
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var faceFrameAnalyzer by remember { mutableStateOf<FaceFrameAnalyzer?>(null) }
    val faceAnalysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var isCapturing by remember { mutableStateOf(false) }
    var loginStarted by remember { mutableStateOf(false) }
    var localRetryTrigger by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("Preparando camara...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            errorMessage = "Debes permitir el uso de la camara para iniciar sesion con rostro."
        }
    }

    // Pide permiso de camara al entrar si el usuario aun no lo concedio.
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Reinicia el intento cuando el backend devuelve error y el usuario acepta el dialogo.
    LaunchedEffect(retryTrigger, localRetryTrigger) {
        loginStarted = false
        isCapturing = false
        faceFrameAnalyzer?.reset()
        if (hasCameraPermission) {
            statusText = "Coloca tu rostro frente a la camara"
        }
    }

    // Libera la camara cuando la pantalla sale de composicion.
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            imageCapture = null
            faceFrameAnalyzer?.close()
            faceFrameAnalyzer = null
            faceAnalysisExecutor.shutdown()
        }
    }

    // Evita que el reconocimiento se quede esperando indefinidamente.
    LaunchedEffect(hasCameraPermission, imageCapture, retryTrigger, localRetryTrigger) {
        if (!hasCameraPermission || imageCapture == null) return@LaunchedEffect

        delay(FACE_LOGIN_TIMEOUT_MS)

        if (!loginStarted) {
            isCapturing = false
            errorMessage = "No se pudo validar el rostro. Intenta nuevamente."
            statusText = "Tiempo de espera agotado"
        }
    }

    // Captura una foto nativa con CameraX y la entrega al ViewModel para enviarla al backend.
    fun capturePhotoForBackend(capture: ImageCapture, executor: Executor) {
        if (isCapturing || loginStarted || errorMessage != null) return

        isCapturing = true
        statusText = "Capturando rostro..."

        val file = File.createTempFile("face_login_", ".jpg", context.cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    compressPhotoForBackend(file)
                    loginStarted = true
                    isCapturing = false
                    statusText = "Rostro capturado. Iniciando sesion..."
                    onFacePhotoCaptured(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    isCapturing = false
                    statusText = "No se pudo capturar la imagen. Intenta nuevamente."
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Preview nativo con CameraX: Android solo muestra camara y toma foto.
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FIT_CENTER

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener(
                            {
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also { previewUseCase ->
                                    previewUseCase.setSurfaceProvider(surfaceProvider)
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                val analyzer = FaceFrameAnalyzer(
                                    onStableFaceDetected = {
                                        mainExecutor.execute {
                                            capturePhotoForBackend(capture, mainExecutor)
                                        }
                                    }
                                )

                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { imageAnalysis ->
                                        imageAnalysis.setAnalyzer(faceAnalysisExecutor, analyzer)
                                    }
                                faceFrameAnalyzer = analyzer

                                runCatching {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_FRONT_CAMERA,
                                        preview,
                                        capture,
                                        analysis
                                    )
                                    cameraProvider = provider
                                    imageCapture = capture
                                    statusText = "Coloca tu rostro frente a la camara"
                                }.onFailure {
                                    errorMessage = "No se pudo iniciar la camara frontal."
                                }
                            },
                            mainExecutor
                        )
                    }
                }
            )
        }

        // Overlay visual fijo: solo guia al usuario; el backend hace todo el reconocimiento.
        FaceRecognitionOverlay()

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(16.dp)
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    cameraProvider?.unbindAll()
                    onCancel()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Cancelar reconocimiento facial")
            }
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                errorMessage = null
                localRetryTrigger++
            },
            title = {
                Text("Reconocimiento facial")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                Button(
                    onClick = {
                        errorMessage = null
                        localRetryTrigger++
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}

// Dibuja un overlay fijo y liviano; la validacion facial real ocurre solo en el backend.
@Composable
private fun FaceRecognitionOverlay() {
    val primaryColor = Color(0xFF8ED4FF)
    val transition = rememberInfiniteTransition(label = "face-overlay")
    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan-progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameWidth = size.width * 0.56f
        val frameHeight = frameWidth * 1.2f
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f
        val scanY = top + frameHeight * scanProgress
        val sideTick = 18.dp.toPx()

        // Linea principal de escaneo minimalista.
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    primaryColor.copy(alpha = 0.45f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(left + 16.dp.toPx(), scanY),
            size = Size(frameWidth - 32.dp.toPx(), 2.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        // Ticks laterales finos para dar sensacion de tracking sin recargar la camara.
        drawLine(
            color = primaryColor.copy(alpha = 0.34f),
            start = Offset(left - 10.dp.toPx(), top + frameHeight * 0.36f),
            end = Offset(left - 10.dp.toPx(), top + frameHeight * 0.36f + sideTick),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.34f),
            start = Offset(left + frameWidth + 10.dp.toPx(), top + frameHeight * 0.36f),
            end = Offset(left + frameWidth + 10.dp.toPx(), top + frameHeight * 0.36f + sideTick),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.24f),
            start = Offset(left - 6.dp.toPx(), top + frameHeight * 0.58f),
            end = Offset(left - 6.dp.toPx(), top + frameHeight * 0.58f + sideTick * 0.7f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.24f),
            start = Offset(left + frameWidth + 6.dp.toPx(), top + frameHeight * 0.58f),
            end = Offset(left + frameWidth + 6.dp.toPx(), top + frameHeight * 0.58f + sideTick * 0.7f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCornerLines(
            color = primaryColor.copy(alpha = 0.74f),
            left = left,
            top = top,
            width = frameWidth,
            height = frameHeight,
            length = 36.dp.toPx(),
            stroke = 1.5.dp.toPx()
        )
    }
}

// Dibuja solo las esquinas finas del marco para mantener un estilo limpio y profesional.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerLines(
    color: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    length: Float,
    stroke: Float
) {
    val right = left + width
    val bottom = top + height

    drawLine(color, Offset(left, top), Offset(left + length, top), stroke, StrokeCap.Round)
    drawLine(color, Offset(left, top), Offset(left, top + length), stroke, StrokeCap.Round)
    drawLine(color, Offset(right, top), Offset(right - length, top), stroke, StrokeCap.Round)
    drawLine(color, Offset(right, top), Offset(right, top + length), stroke, StrokeCap.Round)
    drawLine(color, Offset(left, bottom), Offset(left + length, bottom), stroke, StrokeCap.Round)
    drawLine(color, Offset(left, bottom), Offset(left, bottom - length), stroke, StrokeCap.Round)
    drawLine(color, Offset(right, bottom), Offset(right - length, bottom), stroke, StrokeCap.Round)
    drawLine(color, Offset(right, bottom), Offset(right, bottom - length), stroke, StrokeCap.Round)
}

// Corrige orientacion EXIF, reduce la foto y conserva calidad suficiente para el backend facial.
private fun compressPhotoForBackend(file: File) {
    val original = BitmapFactory.decodeFile(file.absolutePath) ?: return
    val oriented = rotateBitmapFromExif(file, original)
    val resized = resizeKeepingAspectRatio(oriented, FACE_PHOTO_MAX_SIZE)

    file.outputStream().use { output ->
        resized.compress(Bitmap.CompressFormat.JPEG, FACE_PHOTO_JPEG_QUALITY, output)
    }

    if (oriented !== original) {
        original.recycle()
    }

    if (resized !== oriented) {
        oriented.recycle()
    }

    resized.recycle()
}

// CameraX puede guardar la foto con rotacion EXIF; el backend necesita pixeles ya orientados.
private fun rotateBitmapFromExif(file: File, bitmap: Bitmap): Bitmap {
    val orientation = ExifInterface(file.absolutePath).getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
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

// Mantiene la proporcion de la imagen y limita su lado mayor para conservar calidad suficiente.
private fun resizeKeepingAspectRatio(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val largestSide = maxOf(width, height)

    if (largestSide <= maxSize) return bitmap

    val scale = maxSize.toFloat() / largestSide.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)

    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}
