---
name: android-feature
description: Orchestrates an end-to-end IpsAdmin feature across layers (Compose/XML + ViewModel + domain/data + Koin + Gradle). Use when the user asks to create/add a feature, flow, or screen that needs more than UI polish. Do not use for crash/bug fixes, behavior-preserving refactors, PR review, or deciding test types.
---

# android-feature (IpsAdmin)

Pantalla Compose: orquesta este skill **y** `compose-feature`. Ruta nueva inexistente: también `create-compose-screen`. Lógica testeable: `android-tdd`.

## Checklist

```
- [ ] Discovery
- [ ] Arquitectura del rincón
- [ ] Casos / edges
- [ ] Diseño mínimo
- [ ] RED tests
- [ ] GREEN implementación
- [ ] Refactor
- [ ] Build + tests
- [ ] Revisión
```

## 1. Discovery

Leer `AGENTS.md` y 2–3 archivos de la feature vecina.

¿La zona es Compose UDF o Fragment/`IRepository`? No mezclar.

## 2. Arquitectura

| Si el vecino usa… | Tú usas… |
|---|---|
| UseCases + puerto `:domain` | Lo mismo. Canónico: pending subscriptions |
| `IRepository` en VM | `IRepository` (no extraer capas “por si acaso”) |
| Fragment XML | XML/ViewBinding |

Diseño mínimo: Screen + ViewModel basta si no hay regla de negocio. UseCase solo si combina repos, hay invariante, o el módulo ya los tiene.

Flujo mental: Input → Intent → Presentation → Domain → Data → State → UI. Una sola source of truth.

## 3. Edges

Loading / empty / error / offline / cancelación / API 401 / lista grande. minSdk 26.

## 4. TDD

Skill `android-tdd`. Test primero para VM/UseCase/mappers. Characterization si tocas legado.

## 5. Implementación

Koin: registrar VM/UseCase/repo en el módulo correcto. Rutas nuevas en `NavRoutes.kt` + graph. `testTag` en interactivos.

No: Hilt, DataStore, Turbine, migrar XML, hinchar `Repository.kt`.

## 6. Verify

```bat
gradlew.bat :presentation:compileDevDebugKotlin
gradlew.bat :presentation:testDevDebugUnitTest --tests "Fqcn"
```

Más `:domain:test` / `:data:test` si tocaste esos módulos.
