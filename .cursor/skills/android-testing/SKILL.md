---
name: android-testing
description: Chooses IpsAdmin test type and doubles (unit vs Robolectric vs skip; MockK vs Fake). Use when the user asks what to test, coverage, or Fake vs Mock. Do not write ViewModelTest boilerplate (test-viewmodel).
---

# android-testing (IpsAdmin)

## Pirámide real de este repo

| Tipo | Dónde | Qué |
|---|---|---|
| Unit | `src/test` | UseCase, VM, mapper, validator |
| Compose + Robolectric | `presentation/src/test` | Screen crítica con `testTag` |
| Room | `:data/src/test` | DAO con `room-testing` |
| MockWebServer | tests data/sync | HTTP contract |
| Instrumentados | **no hay** | no crear `androidTest` salvo pedido |

## Sí merece test

Regla de negocio, branches de error, sync FIFO/conflictos, VM intents, mapeo DTO↔domain.

## No merece test

Koin wiring, getters, colores, “que el botón exista” sin comportamiento.

## Doubles

- **MockK** — puertos/UseCases (dominante).
- **Fake** — repo in-memory si el test lee/escribe secuencia (pending list).
- No mockear Retrofit interfaces si un Fake/MockWebServer es más claro.
- No mockear `UiState` data classes.

## ViewModel

`StandardTestDispatcher` + `Dispatchers.setMain` + dispatcher inyectado. Sin `Thread.sleep`. Assert `uiState.value` y, si aplica, eventos. Ver skill `test-viewmodel`.

JUnit 4. No Turbine por defecto.
