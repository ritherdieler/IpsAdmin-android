# Observabilidad — Hardening ingest, UI y replay/workflow

**Fecha**: 2026-07-14  
**Repos**: Android (`:observability` + presentation) + backend  
**Alcance**: sin flujos de negocio nuevos (login ya instrumentado)

## Resumen

1. **Fix ingest 401** — el filtro de API key no resolvía el path cuando `servletPath` venía vacío (Kotlin `?:` no cae al `requestURI`). Ahora se normaliza path con context-path. Además el OkHttp de observability inyecta `X-Obs-Api-Key` por interceptor.
2. **Timestamp flexible** — `EventIngestRequest.timestamp` acepta epoch millis (número o string) e ISO-8601 via `FlexibleEpochMillisDeserializer`.
3. **HTTP desacoplado de workflow** — `ObservabilityHttpInterceptor` ya no llama `currentWorkflowTags()`. El enrich vive en `ObservabilityTracer(tagsProvider)`.
4. **Replay ↔ workflow** — `captureAndUpload(sessionId, workflowId)` en error/crash; backend persiste `obs_replay.workflow_id` y lo expone en upload response / `ReplaySummaryDto`.
5. **UI captura amplia** — `ObservabilityUiCapture` + `ObservabilityUiTracker` (Window.Callback + focus + `TextWatcher` en `EditText`) en toda activity; breadcrumbs `category=ui`; **prod, sin mask, sin sampling**. Impacto: más volumen en cola/batería al interactuar.
6. **Compose `text_change`** — `ObservabilityAccessibilityText` + `AccessibilityDelegate` en el decorView (requiere a11y service activo para Compose puro) + bridge `ObservabilityComposeText` en pantallas Compose instrumentadas (login: Usuario/Contraseña, `source=compose`).

## Verificación

- Unit tests módulo `:observability` en verde (API key, interceptor, tracer enrich, replay workflowId, UI capture).
- **E2e device (2026-07-14)**:
  - Causa raíz del 401: secrets/`local.properties` sobreescribía `OBS_API_KEY` con una key distinta a `dev-obs-android-key`. Corregido con `resolveObservabilityApiKey("dev", …)` → siempre `dev-obs-android-key` en flavor `dev`.
  - Login `dscorp` → Panel de Control.
  - `POST …/observability/events` y `…/spans` → **202** (sin flush manual).
  - DB `obs_event`: `workflow_start` + `workflow_end` (`workflow_name=login`, `workflow_status=success`).
  - Session: `5417cc1d-e0dd-4421-a498-93db00ade7df` / workflow `0a3f325a-8dcf-43f8-91f6-d7a4ede4bcf3`.
  - Span `POST /users/login` con tags `workflowId/Name/Category`.
  - Breadcrumbs `category=ui` presentes en el evento de cierre del workflow.

- **E2e device (2026-07-14, Compose text + localhost)**:
  - App `BASE_URL`/`OBS_BASE_URL` → `http://127.0.0.1:8080/ispadmin/` + `adb reverse tcp:8080 tcp:8080` (LAN `192.168.1.11` no alcanzable).
  - Login `dscorp` → Panel de Control.
  - Session `3f5b640f-957f-4127-90a3-4d27e8325073`: `workflow_start` + `workflow_end(success)` login; ingest events/spans **202**.
  - UI crumbs: `click`, `focus`, **`text_change` ×32** con `source=compose` (targets Usuario/Contraseña).
  - Sin replay (esperado: solo error/crash).

## Debounce text_change

- `ObservabilityUiCapture` agrupa `text_change` por `target` con debounce de **1000 ms** (solo el último valor).
- Click/focus/u otro evento UI hace **flush** de pendientes (p. ej. al pulsar login).

## Archivos clave (Android)

- `ObservabilityApiKeyInterceptor.kt`
- `ObservabilityHttpInterceptor.kt` / `ObservabilityTracer.kt`
- `ObservabilityReplaySender.kt` / `ObservabilityClient` (error/crash)
- `ObservabilityUiCapture.kt` / `ObservabilityUiTracker.kt` / `ObservabilityAccessibilityText.kt` / `ObservabilityComposeText.kt` / `ObservabilityActivityTracker.kt`
- `di/ObservabilityModule.kt`
