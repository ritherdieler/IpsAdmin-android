# Observabilidad — Fase 1: Workflow Core

**Fecha**: 2026-07-14  
**Módulo**: `:observability`

API pública de **Workflow** sobre `ObservabilityClient`, sin instrumentar pantallas aún (eso es Fase 2).

## Concepto

- **Session** (`sessionId`): sesión de app (sin cambios).
- **Workflow**: scope de negocio; **uno activo** a la vez dentro de la sesión.

## API

```kotlin
client.startWorkflow(name = "login", category = "auth", context = mapOf(...)): String
client.workflowStep(message = "credentials_submitted", data = mapOf(...))
client.endWorkflow(WorkflowStatus.SUCCESS, reason = "ok")
client.currentWorkflowId(): String?
```

`WorkflowStatus`: `SUCCESS` / `FAILED` / `INTERRUPTED` (`success` | `failed` | `interrupted` en wire).

## Contrato de tags

| Tag | Cuándo |
|-----|--------|
| `workflowId` | Mientras activo / en eventos del workflow |
| `workflowName` | Idem |
| `workflowCategory` | Idem |
| `workflowStatus` | Solo en cierre (`workflow_end`) |

`eventType`:

- `workflow_start` / `workflow_end` — severity `info`
- Pasos: breadcrumb `category = workflow` (vía `workflowStep`)

`context` del evento: snapshot `{ id, name, category, startedAt, …context }`.

## Comportamiento

1. `startWorkflow` con workflow activo → cierra el anterior como `interrupted`.
2. Todo `buildEvent` / `addBreadcrumb` mezcla tags del workflow activo.
3. `endWorkflow` sin activo → no-op.

## Tipos auxiliares

- `ObsWorkflow`, `ObsWorkflowTags`, `WorkflowStatus`
- `ObservabilityEventStore` / `ObservabilityFlushScheduler` (testabilidad; `ObservabilityQueue` y `ObservabilityWorkScheduler` los implementan)

## Tests

- `ObsWorkflowTest`
- `ObservabilityClientWorkflowTest` (cola in-memory + dispatcher unconfined)

## Fuera de esta fase

- HttpInterceptor / CrashHandler / timeout de sesión → interrupted  
- `runWorkflow` / piloto Login  
- Backend / web
