package com.dscorp.ispadmin.data.datasource.remote.logging

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

/** Logs request timing and endpoint status without recording headers, query values, or bodies. */
class HttpRequestTimingInterceptor(
    private val logger: (String) -> Unit = { Log.i(TAG, it) },
) : Interceptor {

    private val requestSequence = AtomicLong()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestId = requestSequence.incrementAndGet()
        val method = request.method
        val path = sanitizePath(request.url.encodedPath)
        val startedAt = System.nanoTime()
        logger("HTTP[$requestId] start method=$method path=$path")

        return try {
            val response = chain.proceed(request)
            logger(
                "HTTP[$requestId] complete method=$method path=$path " +
                    "status=${response.code} duration_ms=${elapsedMillis(startedAt)}",
            )
            response
        } catch (error: IOException) {
            logger(
                "HTTP[$requestId] failed method=$method path=$path " +
                    "error=${error.javaClass.simpleName} duration_ms=${elapsedMillis(startedAt)}",
            )
            throw error
        }
    }

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt).coerceAtLeast(0) / NANOS_PER_MILLISECOND

    private fun sanitizePath(path: String): String = path.split('/').joinToString("/") { segment ->
        when {
            segment.isNotEmpty() && segment.all(Char::isDigit) -> ":id"
            UUID_SEGMENT.matches(segment) -> ":id"
            HEX_IDENTIFIER_SEGMENT.matches(segment) -> ":id"
            else -> segment
        }
    }

    private companion object {
        const val TAG = "HttpRequestTiming"
        const val NANOS_PER_MILLISECOND = 1_000_000L
        val UUID_SEGMENT = Regex("(?i)[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")
        val HEX_IDENTIFIER_SEGMENT = Regex("(?i)[0-9a-f]{12,32}")
    }
}
