package com.dscorp.ispadmin.observability

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObservabilityClientWorkflowTest {

    private lateinit var store: InMemoryEventStore
    private lateinit var client: ObservabilityClient
    private val gson = Gson()

    @Before
    fun setUp() {
        store = InMemoryEventStore()
        val appInfo = object : ObsAppInfo {
            override fun environment() = "dev"
            override fun release() = "1.0.0 (1)"
            override fun versionName() = "1.0.0"
            override fun versionCode() = 1
            override fun flavor() = "dev"
        }
        client = ObservabilityClient(
            api = mockk(relaxed = true),
            queue = store,
            contextProvider = ObservabilityContextProvider(appInfo, ObsUserProvider { null }),
            gson = gson,
            apiKey = "",
            workScheduler = ObservabilityFlushScheduler { },
            config = ObservabilityConfig(apiKey = "", sanitizePayloads = false),
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher())
        )
    }

    @Test
    fun `startWorkflow returns id and emits workflow_start with tags`() {
        val id = client.startWorkflow(name = "login", category = "auth")

        assertThat(id).isNotEmpty()
        assertThat(client.currentWorkflowId()).isEqualTo(id)

        val event = parseEvents().single()
        assertThat(event.eventType).isEqualTo("workflow_start")
        assertThat(event.severity).isEqualTo("info")
        assertThat(event.tags?.get(ObsWorkflowTags.ID)).isEqualTo(id)
        assertThat(event.tags?.get(ObsWorkflowTags.NAME)).isEqualTo("login")
        assertThat(event.tags?.get(ObsWorkflowTags.CATEGORY)).isEqualTo("auth")
        assertThat(event.context?.get("id")).isEqualTo(id)
        assertThat(event.context?.get("name")).isEqualTo("login")
    }

    @Test
    fun `workflowStep adds breadcrumb without workflow_end event`() {
        val id = client.startWorkflow("login", "auth")
        store.clear()

        client.workflowStep("credentials_submitted", mapOf("hasPassword" to true))
        client.reportLog("probe")

        val events = parseEvents()
        assertThat(events.any { it.eventType == "workflow_end" }).isFalse()
        val logEvent = events.single { it.eventType == "log" }
        val crumb = logEvent.breadcrumbs!!.last()
        assertThat(crumb["category"]).isEqualTo(ObsBreadcrumbCategory.WORKFLOW)
        assertThat(crumb["message"]).isEqualTo("credentials_submitted")
        @Suppress("UNCHECKED_CAST")
        val data = crumb["data"] as Map<String, Any?>
        assertThat(data[ObsWorkflowTags.ID]).isEqualTo(id)
        assertThat(data["hasPassword"]).isEqualTo(true)
    }

    @Test
    fun `endWorkflow emits workflow_end with status and clears active`() {
        val id = client.startWorkflow("login", "auth")
        store.clear()

        client.endWorkflow(WorkflowStatus.SUCCESS, reason = "ok")

        assertThat(client.currentWorkflowId()).isNull()
        val event = parseEvents().single()
        assertThat(event.eventType).isEqualTo("workflow_end")
        assertThat(event.severity).isEqualTo("info")
        assertThat(event.tags?.get(ObsWorkflowTags.ID)).isEqualTo(id)
        assertThat(event.tags?.get(ObsWorkflowTags.STATUS)).isEqualTo("success")
        assertThat(event.message).isEqualTo("ok")
    }

    @Test
    fun `starting a second workflow interrupts the previous one`() {
        val first = client.startWorkflow("login", "auth")
        val second = client.startWorkflow("payment", "billing")

        assertThat(client.currentWorkflowId()).isEqualTo(second)
        val events = parseEvents()
        assertThat(events).hasSize(3)
        assertThat(events[0].eventType).isEqualTo("workflow_start")
        assertThat(events[0].tags?.get(ObsWorkflowTags.ID)).isEqualTo(first)
        assertThat(events[1].eventType).isEqualTo("workflow_end")
        assertThat(events[1].tags?.get(ObsWorkflowTags.ID)).isEqualTo(first)
        assertThat(events[1].tags?.get(ObsWorkflowTags.STATUS)).isEqualTo("interrupted")
        assertThat(events[2].eventType).isEqualTo("workflow_start")
        assertThat(events[2].tags?.get(ObsWorkflowTags.ID)).isEqualTo(second)
        assertThat(events[2].tags?.get(ObsWorkflowTags.NAME)).isEqualTo("payment")
    }

    @Test
    fun `reportLog merges active workflow tags`() {
        val id = client.startWorkflow("login", "auth")
        store.clear()

        client.reportLog("checkpoint")

        val event = parseEvents().single()
        assertThat(event.tags?.get(ObsWorkflowTags.ID)).isEqualTo(id)
        assertThat(event.tags?.get(ObsWorkflowTags.NAME)).isEqualTo("login")
        assertThat(event.context?.get("id")).isEqualTo(id)
    }

    @Test
    fun `startWorkflow reuses a business operation id when given`() {
        val id = client.startWorkflow("registro_suscripcion", "registration", workflowId = "op-123")

        assertThat(id).isEqualTo("op-123")
        assertThat(client.currentWorkflowId()).isEqualTo("op-123")
    }

    @Test
    fun `endWorkflow without active workflow is a no-op`() {
        client.endWorkflow(WorkflowStatus.FAILED)
        assertThat(parseEvents()).isEmpty()
        assertThat(client.currentWorkflowId()).isNull()
    }

    private fun parseEvents(): List<ObsEventDto> =
        store.readAll().map { gson.fromJson(it, ObsEventDto::class.java) }
}

class InMemoryEventStore : ObservabilityEventStore {
    private val lines = mutableListOf<String>()

    override fun append(json: String) {
        lines += json
    }

    override fun readAll(): List<String> = lines.toList()

    override fun removeFirst(count: Int) {
        repeat(count.coerceAtMost(lines.size)) { lines.removeAt(0) }
    }

    fun clear() = lines.clear()
}
