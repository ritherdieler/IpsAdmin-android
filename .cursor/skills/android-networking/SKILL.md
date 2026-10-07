---
name: android-networking
description: Adds or changes IpsAdmin Retrofit/Gson APIs (DTO, mapping, interceptors, HTTP errors). Use when the work is the network contract. Do not use as the orchestrator of a full UI feature (android-feature).
---

# android-networking (IpsAdmin)

## Stack

Retrofit + OkHttp + Gson. Cliente en `RetrofitModule.kt`. No Ktor, no Moshi.

## Workflow

1. Contrato: path, método, body, query. Alinear nombres camelCase con backend (`Subscription`, `Payment`, no Invoice).
2. DTO en data/presentation data, **no** en `:domain`. Domain models sin anotaciones Gson salvo legado.
3. Service: preferir API de feature (`AuthApiService`) sobre hinchar `RestApiServices.kt`.
4. Registrar en `apiModule` / `retrofitModule`.
5. Mapper DTO → domain. VM nunca recibe DTO Gson nuevo.
6. Errores: `HttpException`, IO, empty body → `Result.failure` / sealed. Nunca `body()!!`.
7. Auth: interceptor existente. No loguear headers.
8. Test: MockK del service o MockWebServer (`SubscriptionSyncRemoteImplTest` style).

## Retry / cancel

Retry solo idempotente (GET, sync diseñado para ello). Cancelación = coroutine del UseCase/VM.

## Flavors

`dev` → `http://127.0.0.1:8080/ispadmin/`. `prod` → `https://api.gigafiberperu.tech/ispadmin/`. No hardcodear otras bases.
