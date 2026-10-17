# Observabilidad — Workflow (Fases 2–5)

**Fecha**: 2026-07-14

Resumen de la entrega en paralelo tras Fase 0 (módulo) y Fase 1 (API Workflow).

## Fase 2 — Android enrichment + Login

Módulo `:observability` + app:

- `interruptActiveWorkflow`, cierre en crash y en rotación de sesión (>30 min)
- Spans HTTP con tags de workflow (`ObservabilityHttpInterceptor` + `ObservabilityTracer`)
- `ObsSanitize` + `ObservabilityConfig.sanitizePayloads` (`!DEBUG` en app)
- `runWorkflow` y `runTracked` (steps si hay workflow activo)
- Piloto: `LoginViewModel.doLogin` usa `runWorkflow("login", "auth")`

Docs relacionados: `observabilidad-fase1-workflow-core.md`

## Fase 3 — Backend ingest

- Columnas en `ObsEvent`: `workflow_id`, `workflow_name`, `workflow_category`, `workflow_status` (+ index)
- Promoción desde tags en `ObsIngestionService` (patrón feature/action)
- Eventos `workflow_*` / `log` con severity info **no crean issues**
- `EventDto` expone los 4 campos

## Fase 4 — Backend query

- `SessionDetailDto.workflows` vía `ObsWorkflowAggregation`
- Filtro `workflowId` en `GET /observability/sessions/{id}`
- LLM context línea Workflow
- `GET /observability/workflows` (agregación paginada)

## Fase 5 — Web

- Types con campos workflow + `workflows[]`
- `SessionDetailPage`: bloque “Workflows en esta sesión” + badge en timeline
- Test: `SessionDetailPage.workflows.test.tsx`

## Verificación sugerida

1. Login en app Android → events `workflow_start`/`workflow_end` en hub
2. Abrir Session Detail → ver workflows y badges
3. Opcional: `GET /observability/workflows?from=…&to=…`
