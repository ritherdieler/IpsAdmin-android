# Observabilidad — Fase 0: módulo library `:observability`

**Fecha**: 2026-07-14  
**Repos**: Android (`IpsAdmin-android app`)

Extracción del SDK de telemetría/diagnóstico a un módulo Gradle independiente dentro del mismo proyecto, sin cambiar el comportamiento funcional de envío de eventos, spans ni replay.

## Objetivo

Aislar el SDK de la app (`:presentation`) para poder evolucionar Workflow (fases siguientes) y, a futuro, publicar el módulo fuera del monorepo.

## Estructura

```
settings.gradle
  include ':presentation'
  include ':observability'

observability/                 # com.android.library
  src/main/java/.../observability/
    ObservabilityClient, Queue, Tracer, HTTP, Replay, …
    ObsAppInfo, ObsUserProvider, ObsCrashReporter, ObservabilityConfig
  src/test/…

presentation/
  implementation project(':observability')
  di/ObservabilityModule.kt    # wiring Koin + BuildConfig
  observability/
    AppObsAppInfo.kt
    AppObsUserProvider.kt
    FirebaseObsCrashReporter.kt
```

Package público sin cambios: `com.dscorp.ispadmin.observability` (imports de ViewModels intactos).

## Frontera de dependencias

El library **no** depende de:

- `BuildConfig` de la app
- `IRepository` / capa de datos
- Firebase Crashlytics de forma directa

Contratos inyectados desde la app:

| Contrato | Implementación en app |
|----------|------------------------|
| `ObsAppInfo` | `AppObsAppInfo` (flavor, versión, environment) |
| `ObsUserProvider` | `AppObsUserProvider` (sesión vía `IRepository`) |
| `ObsCrashReporter` | `FirebaseObsCrashReporter` |
| `ObservabilityConfig` | `apiKey` desde `BuildConfig.OBS_API_KEY` |

`ObservabilityFlushWorker` usa `ObservabilityConfig` + `ObservabilityQualifiers.gson` (Koin) en lugar de `BuildConfig` / qualifier del módulo di de la app.

## Criterio de hecho (cumplido)

- `:observability` compila como library.
- `:presentation` depende de él y `compileDevDebugSources` OK.
- Código núcleo de obs ya no vive en `presentation/.../observability/` (solo adapters).
- Test unitario `ObservabilityContextProviderTest` verifica inyección de app info / user.

## Siguiente fase

Fase 1: API Workflow (`startWorkflow` / `workflowStep` / `endWorkflow`) dentro de `:observability` con TDD.
