# Observabilidad — Session Replay Android por fotogramas (Cliente)

**Fecha**: 2026-07-11
**Módulo**: `:presentation` — paquete `com.dscorp.ispadmin.observability`
**Plan**: `session_replay_android_por_fotogramas` (Fase 1)

Android nativo no tiene DOM, por lo que no puede usar rrweb. La estrategia es
capturar **fotogramas** (screenshots periódicos) del window de la Activity en
foco, mantener un buffer en anillo con los últimos ~N segundos y, al ocurrir un
error/crash, empaquetar esos fotogramas y subirlos como un manifiesto JSON gzip,
enlazándolo al evento por `replayId`.

> **Sin enmascarado (2026-07-12):** el session replay NO enmascara ningún campo.
> Los fotogramas se suben tal cual (contenido completo visible). Ver
> `observabilidad-replay-sin-enmascarado.md`.

## Componentes

### Ciclo de vida y acceso a la Activity (`ObservabilityActivityTracker.kt`)
- Implementa `Application.ActivityLifecycleCallbacks`; mantiene un
  `WeakReference<Activity>` a la Activity en foreground.
- En `onActivityResumed` llama `recorder.attach(activity)` (arranca la captura);
  en `onActivityPaused` llama `recorder.detach()` (la detiene).
- Se registra en `di/KoinApplication.kt` con
  `registerActivityLifecycleCallbacks(getKoin().get<ObservabilityActivityTracker>())`.

### Grabador (`ObservabilityScreenRecorder.kt`)
- Buffer en anillo (`ArrayDeque<BufferedFrame>`) que retiene los últimos
  `replayWindowMs` (10 s por defecto).
- Captura con `PixelCopy.request(window, bitmap, callback, handler)` en un
  `HandlerThread` propio a baja cadencia (`captureIntervalMs = 1000/replayFps`,
  ~2 fps).
- Cada frame: se hace downscale a `replayWidthPx` (400) directamente desde el
  bitmap capturado y se comprime a WebP (`WEBP_LOSSY`, calidad 50). Se guarda
  `{timestamp, bytes}`. No se aplica ningún enmascarado.
- `snapshot()` devuelve `ObsReplaySnapshot(width, height, frames[], durationMs)`
  con cada frame como `{ t = timestamp - start, img = base64(bytes) }`.

### Enmascarado de PII — ELIMINADO (2026-07-12)
El mecanismo de enmascarado (`ObservabilityMask.kt`, `Modifier.obsMask()`,
`View.obsMask()` y el pintado de rects negros en el grabador) se eliminó por
completo. Los fotogramas ahora capturan todo el contenido sin ocultar campos.
Ver `observabilidad-replay-sin-enmascarado.md`.

### Envío (`ObservabilityReplaySender.kt` + `ObservabilityApi.kt`)
- `ObservabilityApi.uploadReplay(apiKey, contentEncoding="gzip", format="frames",
  sessionId, durationMs, body: RequestBody)` →
  `@POST("observability/replays")` con query `?format=frames&sessionId=...&durationMs=...`
  y headers `X-Obs-Api-Key`, `Content-Encoding: gzip`.
- `captureAndUpload(sessionId)`: toma `recorder.snapshot()`, construye
  `ObsReplayManifest(format="frames", width, height, frames, durationMs)`,
  lo serializa con Gson, lo **gzipea** y lo sube como `application/json`.
  Devuelve el `id` del replay creado (o `null` si no aplica/falla).
- Topes respetados (`ObservabilityReplayConfig`): `maxReplaysPerSession` (10),
  `replayMinIntervalMs` (10 s), `replayMaxBytes` (15 MB). Si el payload excede
  el tope se descarta.

### Enlace con el evento (`ObservabilityClient.kt`)
- `ObsEventDto.replayId: Long?` nuevo campo.
- `reportError(...)`: en su coroutine llama `replaySender?.captureAndUpload(...)`
  y setea `event.replayId` antes de encolar.
- `recordCrash(...)` (usado por `ObservabilityCrashHandler`): igual, con
  `withTimeoutOrNull(CRASH_REPLAY_TIMEOUT_MS = 2500)` para no bloquear el crash.
- **Importante**: solo `reportError` y `recordCrash` capturan replay.
  `reportHttpError` y `reportLog` NO (por diseño, para acotar volumen).

## Configuración (`ObservabilityReplayConfig.kt`)
`enableReplay=true`, `replayFps=2`, `replayWindowMs=10000`,
`maxReplaysPerSession=10`, `replayMinIntervalMs=10_000`,
`replayMaxBytes=15MB`, `replayWidthPx=400`.
Cableado en `di/ObservabilityModule.kt` (recorder, sender, activity tracker).

## Limitaciones
- No es pixel-perfect ni continuo: reconstrucción por fotogramas a ~2 fps.
- `PixelCopy` no captura diálogos de sistema ni superficies `FLAG_SECURE`.
- No hay enmascarado: los fotogramas muestran el contenido completo (incluida
  cualquier PII visible en pantalla).

## Validación E2E (2026-07-11)
Ver el resultado completo en el doc consolidado
[`/Users/sergiocarrillo/gigafiber/.agent-docs/observabilidad-session-replay-android-frames-e2e-2026-07-11.md`].
Resumen: en dispositivo físico (flavor dev) se generó el replay id=3
(`format=frames`, 13 frames WebP, 5981 ms) enlazado al evento id=47.

> Nota (2026-07-12): esa validación reflejaba el comportamiento con enmascarado,
> ya eliminado. A partir de ahora los frames muestran el contenido completo sin
> rectángulos negros.
