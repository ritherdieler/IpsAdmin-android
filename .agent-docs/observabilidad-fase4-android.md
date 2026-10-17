# Observabilidad — Fase 4 (Cliente Android)

**Fecha**: 2026-07-11  
**Actualizado**: 2026-07-14 — el cliente vive en el módulo library `:observability` (ver `observabilidad-fase0-modulo-sdk.md`). Wiring Koin y adapters de app en `:presentation`.

**Módulo**: `:observability` — paquete `com.dscorp.ispadmin.observability`  
**App**: `:presentation` — `di/ObservabilityModule.kt` + adapters `AppObs*`

Implementación del cliente Android de la plataforma de observabilidad in-house.
Envía errores, crashes, errores HTTP, logs y breadcrumbs al hub del backend
(`POST /observability/events`) mediante un Retrofit propio, con cola offline
persistida en archivo y reintentos, más instrumentación de Firebase Crashlytics.

## Contrato del backend (respetado al 100%)

- Base: `OBS_BASE_URL` (hoy el backend, context-path `/ispadmin/`), endpoints bajo `/observability`.
- Header obligatorio `X-Obs-Api-Key` en todo `/observability/**`. Header opcional `X-Correlation-Id`. `Content-Type: application/json` (lo pone Retrofit/Gson).
- `POST /observability/events` (batch): body `{ "events": [ ObsEventDto... ] }`.
- Respuesta `202` `{ accepted, rejected, issueIds }`. Ante `401/429/503` u otros fallos, los eventos se conservan en la cola y se reintentan.

### Campos del evento (`ObsEventDto`)

`eventType` (`error|crash|http_error|log|custom`), `platform` (`android`),
`severity` (`fatal|error|warning|info`), `message`, `errorType`, `stacktrace`,
`environment` (`prod|dev|local`), `release`, `correlationId`, `sessionId`, `url`,
`httpMethod`, `httpStatus`, `durationMs`, `userAgent`, `user{}`, `device{}`,
`breadcrumbs[]`, `tags{}`, `context{}`, `timestamp` (ms, Long).

Gson omite nulos por defecto, por lo que solo se envían los campos con valor.

## BuildConfig por flavor (`presentation/build.gradle`)

Se agregaron dos `buildConfigField` por flavor (variable de URL separada de `BASE_URL`
para sobrevivir a la futura extracción del hub):

| Campo | dev | prod |
|-------|-----|------|
| `OBS_BASE_URL` | `http://192.168.1.39:8080/ispadmin/` | `https://api.gigafiberperu.tech/ispadmin/` |
| `OBS_API_KEY` | `dev-obs-android-key` | desde `local.properties` (`OBS_API_KEY`) o env `OBS_API_KEY` |

`OBS_BASE_URL` hoy apunta al mismo host que `BASE_URL`; al extraer el hub a un
servicio aparte solo cambia ese `buildConfigField`.

Para producción, definir la clave real en `local.properties`:

```properties
OBS_API_KEY=<clave-android-de-produccion>
```

Si `OBS_API_KEY` queda vacío, el cliente no intenta enviar (degradación silenciosa)
pero sigue encolando en disco.

## Archivos nuevos

Paquete `com.dscorp.ispadmin.observability`:

- `ObservabilityContract.kt` — `ObsEventDto`, `ObsBatchRequest`, `ObsBatchResponse`.
- `ObservabilityApi.kt` — interfaz Retrofit `POST observability/events` con headers `X-Obs-Api-Key` y `X-Correlation-Id`.
- `ObservabilityQueue.kt` — cola offline persistida en `filesDir/observability/events-queue.jsonl` (JSON por línea), con límite de 500 eventos (descarta los más antiguos) y operaciones sincronizadas.
- `ObservabilityContextProvider.kt` — contexto desde `Repository.getUserSession()`, `BuildConfig` (flavor/versión) y `Build` (dispositivo). Expone `environment/release/userAgent/user/device`.
- `ObservabilityClient.kt` — orquestador. `sessionId` por proceso, breadcrumbs en memoria (máx. 50), envío por lotes de 100 en `Dispatchers.IO`, reintentos y `flushInternal` con guardia `AtomicBoolean`. API pública: `start()`, `addBreadcrumb()`, `reportError()`, `reportLog()`, `reportHttpError()`, `recordCrash()`, `flush()`. `reportError()` además llama a `FirebaseCrashlytics.recordException()` (no-fatales).
- `ObservabilityCrashHandler.kt` — `Thread.UncaughtExceptionHandler` que encola el crash (escritura síncrona a disco) y delega en el handler previo (Crashlytics se mantiene).
- `ObservabilityHttpInterceptor.kt` — interceptor OkHttp: genera/propaga `X-Correlation-Id`, reporta errores HTTP `>= 400` y fallos de red a observabilidad.

Paquete `com.dscorp.ispadmin.di`:

- `ObservabilityModule.kt` — módulo Koin. Retrofit/OkHttp/Gson propios con qualifier `named("observability")` (sin el interceptor de observabilidad, para evitar bucles), constante `OBS_BASE_URL` y providers de `ObservabilityApi`, `ObservabilityQueue`, `ObservabilityContextProvider`, `ObservabilityClient` e `ObservabilityHttpInterceptor` (este con `lazy { get<ObservabilityClient>() }` para romper el ciclo de inicialización con el OkHttp principal).

## Archivos modificados

- `presentation/build.gradle` — `def obsApiKey` y los `buildConfigField` `OBS_BASE_URL`/`OBS_API_KEY` por flavor.
- `di/RetrofitModule.kt` — `provideHttpClient` ahora recibe `ObservabilityHttpInterceptor` y lo agrega como primer interceptor del cliente OkHttp principal (el de la app, no el de observabilidad).
- `di/KoinApplication.kt` — registra `observabilityModule`, fija la property `OBS_BASE_URL`, instrumenta Crashlytics (`setUserId` con el id de sesión, custom keys `flavor/versionName/versionCode/environment`), instala el `ObservabilityCrashHandler` (delegando en el handler previo) y arranca el cliente (`start()` dispara el flush de la cola persistida).
- `navigation/IpsAdminNavHost.kt` — `LaunchedEffect` sobre `navController.currentBackStackEntryFlow` que registra un breadcrumb `navigation` por cada cambio de destino; el `ObservabilityClient` se obtiene con `koinInject()`.

## Flujo

1. Firebase inicializa (ContentProvider) e instala su `UncaughtExceptionHandler`.
2. En `onCreate` se arranca Koin, se instrumenta Crashlytics y se envuelve el handler de crashes delegando en el de Crashlytics.
3. Al arrancar, `ObservabilityClient.start()` intenta vaciar la cola persistida (crashes del arranque anterior, eventos pendientes por falta de red).
4. Cada `reportX` serializa el evento, lo agrega a la cola en disco y dispara un flush en `Dispatchers.IO`.
5. El interceptor OkHttp del cliente principal añade `X-Correlation-Id` y reporta HTTP `>= 400` / fallos de red.
6. Los breadcrumbs de navegación se adjuntan a cada evento.

## Camino de extracción del hub (lado Android)

Al mover el hub a un servicio aparte, **solo** cambia el `buildConfigField`
`OBS_BASE_URL` (y, si aplica, la clave `OBS_API_KEY`) en `presentation/build.gradle`.
El resto del cliente no requiere cambios.

## Instrumentación por pantalla

- Registro de suscripción: ver `observabilidad-registro-suscripcion.md`
  (breadcrumbs de acciones y `reportError`/`reportLog` de fallos de dominio en
  `RegisterSubscriptionComposeViewModel`).

## Trazado distribuido (Fase 3)

El cliente de eventos descrito aquí se complementa con el trazado W3C Trace Context
(spans CLIENT, `traceparent`, `X-Obs-Session-Id`, renovación de sesión, flush por
conexión y WorkManager). Ver `observabilidad-fase3-android-trazas.md`.

Cambios relevantes para este documento:

- `ObservabilityClient` ahora recibe `workScheduler: ObservabilityWorkScheduler` y su
  `sessionId` es mutable (renovación por inactividad >30 min vía `ProcessLifecycleOwner`);
  `buildEvent` usa `currentSessionId()`.
- El flush de eventos se extrajo a `ObservabilityEventSender` (reusado por el `Worker`).
- `ObservabilityHttpInterceptor` recibe además `tracerProvider` y crea un span CLIENT por
  petición, manteniendo el reporte de errores HTTP.

Para el flujo end-to-end completo (Android → backend → SQL → waterfall web), el contrato
compartido y el estado de validación, ver el documento consolidado a nivel de workspace:
`../../.agent-docs/observabilidad-waterfall-trazas-distribuidas-e2e-2026-07-11.md`.

## Validación

- `./gradlew :presentation:compileDevDebugKotlin` → `BUILD SUCCESSFUL`.
- Pruebas end-to-end quedan a cargo de otro agente (no incluidas aquí).
