# Observabilidad — Fase 3 (Android: iniciar la traza)

**Fecha**: 2026-07-11
**Módulo**: `:presentation` — paquete `com.dscorp.ispadmin.observability`

Implementación del trazado distribuido en el cliente Android: la app se vuelve el
origen (span raíz `CLIENT`) de cada traza W3C Trace Context. Cada petición HTTP
saliente propaga `traceparent` + `X-Obs-Session-Id` y encola un span que se envía por
lotes a `POST /observability/spans`. Complementa el envío de eventos de la fase 4
(errores/crashes/logs) descrito en `observabilidad-fase4-android.md`.

## Contrato del span (`POST /observability/spans`, batch/array)

Body: array JSON de `ObsSpanDto`. Header obligatorio `X-Obs-Api-Key` (protegido por
`ObservabilityApiKeyFilter` del backend). Campos:

`traceId` (32 hex), `spanId` (16 hex), `parentSpanId` (null en el span raíz Android),
`name` (`METHOD ruta`), `kind` (`CLIENT`), `platform` (`android`), `sessionId`,
`startEpochMs`, `durationMs`, `status` (`OK|ERROR`), `httpMethod`, `httpRoute`,
`httpStatus`, `dbStatement` (null en Android), `tagsJson` (null), `environment`,
`release`.

Gson omite nulos, por lo que solo viajan los campos con valor.

## W3C Trace Context y sesión

- `traceparent`: `00-<traceId>-<spanId>-01` (versión 00, sampled). El interceptor lo
  genera por cada petición; el backend (`TraceContextFilter`) crea el span SERVER hijo.
- `X-Correlation-Id = traceId`: el correlation id ahora vale el `traceId`, enlazando los
  `ObsEvent` de errores con su traza sin migración.
- `X-Obs-Session-Id`: el `sessionId` vigente del `ObservabilityClient`, para que el
  backend asocie la sesión al span SERVER y a sus `ReportedEvent`.

## Renovación de sesión por inactividad

`ObservabilityClient.sessionId` deja de ser un `val` fijo:

- `sessionIdRef` (`AtomicReference<String>`), inicial `UUID.randomUUID()`.
- Observador sobre `ProcessLifecycleOwner`: `onStop` guarda el instante de background;
  `onStart` regenera el `sessionId` si el tiempo en background superó **30 min**
  (estándar Sentry/Datadog). El formato UUID convive con hex (columna length 100).
- `currentSessionId()` es la fuente única leída por el interceptor, el tracer y los
  eventos (`buildEvent`).

## Flush por conexión y entrega garantizada

- **`ObservabilityConnectivityMonitor`**: `registerDefaultNetworkCallback()`; en
  `onAvailable` dispara `ObservabilityClient.flush()` (eventos) y
  `ObservabilityTracer.flush()` (spans). Cierra el hueco de que la cola solo se vaciaba
  al encolar o en `start()`. Sin dependencias nuevas.
- **`WorkManager` (`androidx.work`)**: `OneTimeWorkRequest` con
  `Constraints(NetworkType.CONNECTED)`, backoff exponencial, trabajo único
  (`ExistingWorkPolicy.KEEP`). Reintenta **solo el flush de errores/crashes** aunque la
  app esté cerrada o tras reiniciar el equipo. **Los spans NO se reintentan por
  WorkManager** (vida corta; reenviarlos ensuciaría las trazas): para spans basta el
  flush por conexión + `start()`.
  - `ObservabilityWorkScheduler.scheduleEventFlush()` se llama desde `reportError`,
    `reportHttpError` y `recordCrash`.
  - `ObservabilityFlushWorker` (`CoroutineWorker` + `KoinComponent`) reconstruye desde
    Koin `ObservabilityApi`, la `ObservabilityQueue` de eventos (`events-queue.jsonl`) y
    el `Gson` de observabilidad, y usa `ObservabilityEventSender.flush(...)`.
    `Result.retry()` si no se entregó.

## Exclusión del propio cliente de observabilidad

El interceptor solo se registra en el OkHttp principal de la app (`RetrofitModule`). El
Retrofit/OkHttp de observabilidad (qualifier `named("observability")`) es separado y no
lo incluye, por lo que sus llamadas (`/observability/events`, `/observability/spans`) no
generan spans ni bucles.

## Archivos nuevos

Paquete `com.dscorp.ispadmin.observability`:

- `ObservabilitySpanContract.kt` — `ObsSpanDto` (contrato del span).
- `ObservabilitySpanApi.kt` — Retrofit `POST observability/spans` (batch/array) con
  header `X-Obs-Api-Key`.
- `ObservabilityTracer.kt` — genera `traceId`/`spanId` con `SecureRandom`, construye el
  span CLIENT, lo encola en `spans-queue.jsonl` y hace flush por lotes de 100 con guardia
  `AtomicBoolean`. Helper `traceparent(traceId, spanId)`.
- `ObservabilityEventSender.kt` — lógica de flush de **eventos** compartida por
  `ObservabilityClient` y `ObservabilityFlushWorker`.
- `ObservabilityWorkScheduler.kt` — encola el `OneTimeWorkRequest` de flush de eventos.
- `ObservabilityFlushWorker.kt` — `CoroutineWorker` que reintenta el flush de eventos.
- `ObservabilityConnectivityMonitor.kt` — `NetworkCallback` que dispara flush de eventos
  y spans al recuperar red.

## Archivos modificados

- `versions.gradle` — `workVersion = '2.9.1'`.
- `presentation/build.gradle` — `androidx.lifecycle:lifecycle-process` y
  `androidx.work:work-runtime-ktx`.
- `observability/ObservabilityQueue.kt` — parámetro `fileName` (default
  `events-queue.jsonl`); la cola de spans usa `spans-queue.jsonl`.
- `observability/ObservabilityClient.kt` — `sessionId` mutable con renovación por
  inactividad (`ProcessLifecycleOwner`); `currentSessionId()`; `flushInternal` delega en
  `ObservabilityEventSender`; agenda WorkManager en `reportError`/`reportHttpError`/
  `recordCrash`; nuevo parámetro de constructor `workScheduler`.
- `observability/ObservabilityHttpInterceptor.kt` — crea span CLIENT por petición
  (siempre), inyecta `traceparent` y `X-Obs-Session-Id`, mantiene
  `X-Correlation-Id = traceId`; nuevo parámetro `tracerProvider`.
- `di/ObservabilityModule.kt` — providers de `ObservabilitySpanApi`, `ObservabilityTracer`,
  `ObservabilityWorkScheduler`, `ObservabilityConnectivityMonitor` y la cola de spans
  (qualifier `named("observability-spans-queue")`); constructores actualizados.
- `di/KoinApplication.kt` — `setupObservability` arranca flush de spans
  (`ObservabilityTracer.flush()`) y registra `ObservabilityConnectivityMonitor`.

## Wiring de Koin

- `ObservabilityQueue` de eventos: `single { ObservabilityQueue(androidContext()) }`
  (default, sin qualifier — lo consume el worker vía `by inject()`).
- `ObservabilityQueue` de spans: `single(named("observability-spans-queue")) { ... }`.
- `ObservabilityClient` y `ObservabilityTracer` siguen siendo `single`; el worker no
  depende de esas instancias, reconstruye `ObservabilityApi`/`ObservabilityQueue`/`Gson`
  desde Koin (proceso reiniciado ⇒ `Application.onCreate` reinicia Koin antes de correr).

## Validación

- `./gradlew :presentation:compileDevDebugKotlin` → `BUILD SUCCESSFUL`.
- `./gradlew :presentation:testDevDebugUnitTest --tests RegisterSubscriptionComposeViewModelTest`
  → `BUILD SUCCESSFUL` (usa `mockk(relaxed = true)` para el cliente; el cambio de
  constructor no lo afecta).
- Prueba end-to-end en dispositivo físico: a cargo de una fase posterior.
