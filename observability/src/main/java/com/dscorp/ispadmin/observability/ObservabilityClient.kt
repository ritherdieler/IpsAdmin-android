package com.dscorp.ispadmin.observability

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.PrintWriter
import java.io.StringWriter
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class ObservabilityClient(
    private val api: ObservabilityApi,
    private val queue: ObservabilityEventStore,
    private val contextProvider: ObservabilityContextProvider,
    private val gson: Gson,
    private val apiKey: String,
    private val workScheduler: ObservabilityFlushScheduler,
    private val replaySender: ObservabilityReplaySender? = null,
    private val crashReporter: ObsCrashReporter? = null,
    private val config: ObservabilityConfig = ObservabilityConfig(apiKey = apiKey),
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val sessionIdRef = AtomicReference(UUID.randomUUID().toString())
    private val lastBackgroundAt = AtomicLong(0L)
    private val sessionTimeoutMs = TimeUnit.MINUTES.toMillis(30)
    private val breadcrumbs = ArrayDeque<Map<String, Any?>>()
    private val breadcrumbLock = Any()
    private val maxBreadcrumbs = 50
    private val flushing = AtomicBoolean(false)
    private val batchSize = 100
    private val activeWorkflow = AtomicReference<ObsWorkflow?>(null)
    private val workflowLock = Any()

    fun currentSessionId(): String = sessionIdRef.get()

    fun currentWorkflowId(): String? = activeWorkflow.get()?.id

    fun currentWorkflowTags(): Map<String, Any?>? =
        activeWorkflow.get()?.let { ObsWorkflowTags.active(it) }

    fun interruptActiveWorkflow(reason: String = "interrupted") {
        endWorkflow(WorkflowStatus.INTERRUPTED, reason = reason)
    }

    fun start() {
        registerLifecycleObserver()
        flush()
    }

    fun startWorkflow(
        name: String,
        category: String,
        context: Map<String, Any?> = emptyMap(),
        workflowId: String? = null,
    ): String = synchronized(workflowLock) {
        if (activeWorkflow.get() != null) {
            endWorkflowLocked(WorkflowStatus.INTERRUPTED, reason = "replaced_by_new_workflow")
        }
        val workflow = ObsWorkflow(
            id = workflowId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            name = name,
            category = category,
            context = ObsSanitize.sanitizeMap(context, config.sanitizePayloads) ?: emptyMap(),
            startedAt = System.currentTimeMillis()
        )
        activeWorkflow.set(workflow)
        addBreadcrumb(
            category = ObsBreadcrumbCategory.WORKFLOW,
            message = "workflow_start:$name",
            data = ObsWorkflowTags.active(workflow) + context
        )
        enqueue(
            buildEvent(
                eventType = "workflow_start",
                severity = "info",
                message = name,
                tags = ObsWorkflowTags.active(workflow)
            )
        )
        workflow.id
    }

    fun workflowStep(message: String, data: Map<String, Any?> = emptyMap()) {
        val workflow = activeWorkflow.get() ?: return
        val sanitized = ObsSanitize.sanitizeMap(data, config.sanitizePayloads) ?: emptyMap()
        addBreadcrumb(
            category = ObsBreadcrumbCategory.WORKFLOW,
            message = message,
            data = ObsWorkflowTags.active(workflow) + sanitized
        )
    }

    fun endWorkflow(
        status: WorkflowStatus,
        reason: String? = null,
        data: Map<String, Any?> = emptyMap()
    ) {
        synchronized(workflowLock) {
            endWorkflowLocked(status, reason, data)
        }
    }

    private fun endWorkflowLocked(
        status: WorkflowStatus,
        reason: String? = null,
        data: Map<String, Any?> = emptyMap()
    ) {
        val workflow = activeWorkflow.get() ?: return
        val closedTags = ObsWorkflowTags.closed(workflow, status) + data
        addBreadcrumb(
            category = ObsBreadcrumbCategory.WORKFLOW,
            message = "workflow_end:${status.wireValue}",
            data = closedTags
        )
        activeWorkflow.set(null)
        enqueue(
            buildEvent(
                eventType = "workflow_end",
                severity = "info",
                message = reason ?: status.wireValue,
                tags = closedTags,
                workflowOverride = workflow
            )
        )
    }

    private fun registerLifecycleObserver() {
        runCatching {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        lastBackgroundAt.set(System.currentTimeMillis())
                    }

                    override fun onStart(owner: LifecycleOwner) {
                        val backgroundedAt = lastBackgroundAt.get()
                        if (backgroundedAt > 0 &&
                            System.currentTimeMillis() - backgroundedAt > sessionTimeoutMs
                        ) {
                            interruptActiveWorkflow("session_timeout")
                            sessionIdRef.set(UUID.randomUUID().toString())
                        }
                    }
                }
            )
        }
    }

    fun addBreadcrumb(category: String, message: String, data: Map<String, Any?>? = null) {
        val mergedData = ObsSanitize.sanitizeMap(mergeWorkflowIntoData(data), config.sanitizePayloads)
        synchronized(breadcrumbLock) {
            breadcrumbs.addLast(
                mapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "category" to category,
                    "message" to message,
                    "data" to mergedData
                )
            )
            while (breadcrumbs.size > maxBreadcrumbs) breadcrumbs.removeFirst()
        }
    }

    fun reportError(
        throwable: Throwable,
        message: String? = null,
        severity: String = "error",
        tags: Map<String, Any?>? = null
    ) {
        runCatching { crashReporter?.recordException(throwable) }
        val workflowId = currentWorkflowId()
        coroutineScope.launch {
            val replayId = runCatching {
                replaySender?.captureAndUpload(currentSessionId(), workflowId = workflowId)
            }.getOrNull()
            val event = buildEvent(
                eventType = "error",
                severity = severity,
                message = message ?: throwable.message,
                errorType = throwable.javaClass.name,
                stacktrace = stackTraceToString(throwable),
                tags = tags,
                replayId = replayId
            )
            runCatching { queue.append(gson.toJson(event)) }
            flushInternal()
        }
        workScheduler.scheduleEventFlush()
    }

    fun reportLog(message: String, severity: String = "info", tags: Map<String, Any?>? = null) {
        enqueue(
            buildEvent(
                eventType = "log",
                severity = severity,
                message = message,
                tags = tags
            )
        )
    }

    fun reportHttpError(
        url: String,
        httpMethod: String,
        httpStatus: Int,
        durationMs: Long,
        correlationId: String,
        message: String? = null,
        stacktrace: String? = null
    ) {
        enqueue(
            buildEvent(
                eventType = "http_error",
                severity = if (httpStatus >= 500) "error" else "warning",
                message = message ?: "HTTP $httpStatus $httpMethod $url",
                errorType = "HttpError",
                stacktrace = stacktrace,
                url = url,
                httpMethod = httpMethod,
                httpStatus = httpStatus,
                durationMs = durationMs,
                correlationId = correlationId
            )
        )
        workScheduler.scheduleEventFlush()
    }

    fun recordCrash(throwable: Throwable) {
        val workflowId = currentWorkflowId()
        interruptActiveWorkflow("crash")
        val replayId = runCatching {
            runBlocking {
                withTimeoutOrNull(CRASH_REPLAY_TIMEOUT_MS) {
                    replaySender?.captureAndUpload(currentSessionId(), workflowId = workflowId)
                }
            }
        }.getOrNull()
        val event = buildEvent(
            eventType = "crash",
            severity = "fatal",
            message = throwable.message ?: throwable.javaClass.name,
            errorType = throwable.javaClass.name,
            stacktrace = stackTraceToString(throwable),
            replayId = replayId
        )
        runCatching { queue.append(gson.toJson(event)) }
        runCatching { workScheduler.scheduleEventFlush() }
    }

    fun flush() {
        coroutineScope.launch { flushInternal() }
    }

    private fun enqueue(event: ObsEventDto) {
        coroutineScope.launch {
            runCatching { queue.append(gson.toJson(event)) }
            flushInternal()
        }
    }

    private suspend fun flushInternal() {
        if (apiKey.isBlank()) return
        if (!flushing.compareAndSet(false, true)) return
        try {
            ObservabilityEventSender.flush(api, queue, gson, apiKey, batchSize)
        } finally {
            flushing.set(false)
        }
    }

    private fun buildEvent(
        eventType: String,
        severity: String,
        message: String? = null,
        errorType: String? = null,
        stacktrace: String? = null,
        url: String? = null,
        httpMethod: String? = null,
        httpStatus: Int? = null,
        durationMs: Long? = null,
        correlationId: String? = null,
        tags: Map<String, Any?>? = null,
        replayId: Long? = null,
        workflowOverride: ObsWorkflow? = null
    ): ObsEventDto {
        val workflow = workflowOverride ?: activeWorkflow.get()
        val mergedTags = mergeWorkflowTags(tags, workflow)
        val context = workflow?.let {
            ObsSanitize.sanitizeMap(ObsWorkflowTags.contextSnapshot(it), config.sanitizePayloads)
        }
        return ObsEventDto(
            eventType = eventType,
            severity = severity,
            message = message,
            errorType = errorType,
            stacktrace = stacktrace,
            environment = contextProvider.environment(),
            release = contextProvider.release(),
            correlationId = correlationId,
            sessionId = currentSessionId(),
            url = url,
            httpMethod = httpMethod,
            httpStatus = httpStatus,
            durationMs = durationMs,
            userAgent = contextProvider.userAgent(),
            user = ObsSanitize.sanitizeMap(contextProvider.user(), config.sanitizePayloads),
            device = contextProvider.device(),
            breadcrumbs = currentBreadcrumbs(),
            tags = ObsSanitize.sanitizeMap(mergedTags, config.sanitizePayloads),
            context = context,
            replayId = replayId,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun mergeWorkflowTags(
        tags: Map<String, Any?>?,
        workflow: ObsWorkflow?
    ): Map<String, Any?>? {
        if (workflow == null) return tags
        val base = ObsWorkflowTags.active(workflow)
        if (tags.isNullOrEmpty()) return base
        return base + tags
    }

    private fun mergeWorkflowIntoData(data: Map<String, Any?>?): Map<String, Any?>? {
        val workflow = activeWorkflow.get() ?: return data
        val workflowTags = ObsWorkflowTags.active(workflow)
        if (data.isNullOrEmpty()) return workflowTags
        return workflowTags + data
    }

    private fun currentBreadcrumbs(): List<Map<String, Any?>> =
        synchronized(breadcrumbLock) { breadcrumbs.toList() }

    private fun stackTraceToString(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    companion object {
        private const val CRASH_REPLAY_TIMEOUT_MS = 2500L
    }
}
