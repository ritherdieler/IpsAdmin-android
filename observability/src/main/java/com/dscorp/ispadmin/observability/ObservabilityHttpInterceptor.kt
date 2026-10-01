package com.dscorp.ispadmin.observability

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class ObservabilityHttpInterceptor(
    private val clientProvider: Lazy<ObservabilityClient>,
    private val tracerProvider: Lazy<ObservabilityTracer>
) : Interceptor {

    companion object {
        private const val CORRELATION_HEADER = "X-Correlation-Id"
        private const val TRACEPARENT_HEADER = "traceparent"
        private const val SESSION_HEADER = "X-Obs-Session-Id"
        private const val OPERATION_HEADER = "X-Operation-Id"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val tracer = tracerProvider.value
        val client = clientProvider.value
        val original = chain.request()
        val traceId = tracer.newTraceId()
        val spanId = tracer.newSpanId()
        val sessionId = client.currentSessionId()
        val route = original.url.encodedPath
        val spanName = "${original.method} $route"

        val request = original.newBuilder()
            .header(CORRELATION_HEADER, traceId)
            .header(TRACEPARENT_HEADER, tracer.traceparent(traceId, spanId))
            .header(SESSION_HEADER, sessionId)
            .apply { client.currentWorkflowId()?.takeIf { it.isNotBlank() }?.let { header(OPERATION_HEADER, it) } }
            .build()

        val startedAt = System.currentTimeMillis()
        val response: Response = try {
            chain.proceed(request)
        } catch (error: IOException) {
            val durationMs = System.currentTimeMillis() - startedAt
            runCatching {
                tracer.recordClientSpan(
                    traceId = traceId,
                    spanId = spanId,
                    name = spanName,
                    httpMethod = request.method,
                    httpRoute = route,
                    httpStatus = null,
                    startEpochMs = startedAt,
                    durationMs = durationMs,
                    status = "ERROR",
                    sessionId = sessionId
                )
            }
            runCatching {
                client.reportHttpError(
                    url = request.url.toString(),
                    httpMethod = request.method,
                    httpStatus = 0,
                    durationMs = durationMs,
                    correlationId = traceId,
                    message = error.message ?: "Network failure",
                    stacktrace = error.stackTraceToString()
                )
            }
            throw error
        }

        val durationMs = System.currentTimeMillis() - startedAt
        runCatching {
            tracer.recordClientSpan(
                traceId = traceId,
                spanId = spanId,
                name = spanName,
                httpMethod = request.method,
                httpRoute = route,
                httpStatus = response.code,
                startEpochMs = startedAt,
                durationMs = durationMs,
                status = if (response.code >= 400) "ERROR" else "OK",
                sessionId = sessionId
            )
        }

        if (response.code >= 400) {
            runCatching {
                client.reportHttpError(
                    url = request.url.toString(),
                    httpMethod = request.method,
                    httpStatus = response.code,
                    durationMs = durationMs,
                    correlationId = response.header(CORRELATION_HEADER) ?: traceId,
                    message = "HTTP ${response.code} ${response.message}"
                )
            }
        }

        return response
    }
}

