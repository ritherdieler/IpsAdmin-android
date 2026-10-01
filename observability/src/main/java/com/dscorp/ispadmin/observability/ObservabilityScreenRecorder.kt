package com.dscorp.ispadmin.observability

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.view.PixelCopy
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.util.ArrayDeque

data class ObsReplayFrame(val t: Long, val img: String)

data class ObsReplaySnapshot(
    val width: Int,
    val height: Int,
    val frames: List<ObsReplayFrame>,
    val durationMs: Long
)

class ObservabilityScreenRecorder(
    private val config: ObservabilityReplayConfig
) {

    private class BufferedFrame(val timestamp: Long, val bytes: ByteArray)

    private val bufferLock = Any()
    private val buffer = ArrayDeque<BufferedFrame>()

    @Volatile
    private var activityRef: WeakReference<Activity>? = null

    @Volatile
    private var running = false

    @Volatile
    private var frameWidth = 0

    @Volatile
    private var frameHeight = 0

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val captureRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            runCatching { captureFrame() }
            handler?.postDelayed(this, config.captureIntervalMs)
        }
    }

    fun attach(activity: Activity) {
        if (!config.enableReplay) return
        activityRef = WeakReference(activity)
        startLoop()
    }

    fun detach() {
        stopLoop()
        activityRef = null
    }

    fun clear() {
        synchronized(bufferLock) { buffer.clear() }
    }

    fun snapshot(): ObsReplaySnapshot? {
        if (ObsReplayPrivacy.isSuppressed()) return null
        val frames = synchronized(bufferLock) { buffer.toList() }
        if (frames.isEmpty()) return null
        val start = frames.first().timestamp
        val entries = frames.map {
            ObsReplayFrame(
                t = it.timestamp - start,
                img = Base64.encodeToString(it.bytes, Base64.NO_WRAP)
            )
        }
        val durationMs = frames.last().timestamp - start
        return ObsReplaySnapshot(
            width = frameWidth,
            height = frameHeight,
            frames = entries,
            durationMs = durationMs
        )
    }

    private fun startLoop() {
        if (running) return
        running = true
        val thread = HandlerThread("obs-replay").also { it.start() }
        handlerThread = thread
        val loopHandler = Handler(thread.looper)
        handler = loopHandler
        loopHandler.post(captureRunnable)
    }

    private fun stopLoop() {
        running = false
        handler?.removeCallbacks(captureRunnable)
        handler = null
        handlerThread?.quitSafely()
        handlerThread = null
    }

    private fun captureFrame() {
        if (ObsReplayPrivacy.isSuppressed()) {
            clear()
            return
        }
        val activity = activityRef?.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        val window = activity.window ?: return
        val decorView = window.decorView
        val width = decorView.width
        val height = decorView.height
        if (width <= 0 || height <= 0) return

        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val captureHandler = handler ?: return
        runCatching {
            PixelCopy.request(
                window,
                source,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        runCatching { processBitmap(source) }
                    }
                    source.recycle()
                },
                captureHandler
            )
        }
    }

    private fun processBitmap(source: Bitmap) {
        val targetWidth = config.replayWidthPx.coerceAtMost(source.width).coerceAtLeast(1)
        val scale = targetWidth.toFloat() / source.width.toFloat()
        val targetHeight = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)

        val outputStream = ByteArrayOutputStream()
        scaled.compress(webpFormat(), WEBP_QUALITY, outputStream)
        val bytes = outputStream.toByteArray()
        frameWidth = scaled.width
        frameHeight = scaled.height
        scaled.recycle()

        addFrame(BufferedFrame(System.currentTimeMillis(), bytes))
    }

    private fun addFrame(frame: BufferedFrame) {
        synchronized(bufferLock) {
            buffer.addLast(frame)
            val threshold = frame.timestamp - config.replayWindowMs
            while (buffer.isNotEmpty() && buffer.first().timestamp < threshold) {
                buffer.removeFirst()
            }
        }
    }

    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

    companion object {
        private const val WEBP_QUALITY = 50
    }
}
