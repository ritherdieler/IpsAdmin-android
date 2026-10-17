# Fase 9 — App Android: PPPoE en detalle y migración con progreso

Fecha: 2026-09-11  
Repo: `IpsAdmin-android app`  
Backend: no se tocó. El cliente queda listo contra el contrato de la Fase 6.

## Qué ve el operador

- Detalle de suscripción: si `accessMode` es PPPoE (`PPPOE_DYNAMIC` / `PPPOE_FIXED`) muestra **Usuario PPPoE** en vez de IP vacía. Si es `STATIC_IP` (o el campo aún no llega), sigue mostrando la IP.
- En FIBER estático aparece el botón **Migrar a PPPoE**. El progreso se poll-ea igual que `registration-progress`.
- Alta exitosa: la fila de red usa `Subscription.networkAccessLabel()` / `networkAccessValue()`.

## Contrato Retrofit (`AccessMigrationApiService`)

Base URL del flavor (`BuildConfig.BASE_URL`), paths relativos al Core:

| Método | Path | Body | Respuesta |
|--------|------|------|-----------|
| `POST` | `subscription/{id}/access-migration` | vacío | `AccessMigrationProgressDto` (rápida, stage actual) |
| `GET` | `subscription/{id}/access-migration` | — | `AccessMigrationProgressDto` |
| `GET` | `subscription/access-migration/eligible` | — | `List<AccessMigrationEligibleDto>` (cliente listo; **sin UI** en esta tanda) |

`AccessMigrationProgressDto` (todos los campos nullable para Gson backward-compatible):

```json
{
  "subscriptionId": 10,
  "stage": "OLT_READY",
  "failureReason": null,
  "quarantineUntil": null,
  "pppoeUsername": "gf-10",
  "attempt": 1,
  "done": false,
  "message": "Service-port VLAN 100 verificado"
}
```

Stages: `ELIGIBLE`, `OLT_READY`, `SECRET_READY`, `CPE_APPLIED`, `VERIFIED`, `QUEUE_CLEARED`, `QUARANTINE`, `DONE`, `FAILED_REVERTED`, `FAILED_STRANDED`.

El poll se detiene cuando `done == true` o el stage es terminal (`QUARANTINE`, `DONE`, `FAILED_REVERTED`, `FAILED_STRANDED`).

## `GET /subscription/{id}` — campos nuevos

`SubscriptionResponse` lee, todos opcionales:

- `accessMode`
- `pppoeUsername`
- `accessMigrationStage`
- `accessMigrationFailureReason`
- `accessMigrationQuarantineUntil`
- `accessMigration` (objeto anidado, misma forma que el DTO de progreso)

Si el backend aún no los envía, Gson los deja en `null` y la UI se comporta como hoy (IP si existe, sin botón de migrar salvo FIBER + stage ausente/`ELIGIBLE`).

## Gaps (backend Fase 6 aún no desplegada)

- `SubscriptionDto` en Core **no** tiene `accessMode` / `pppoeUsername` / estado de migración.
- `SubscriptionController` **no** expone `POST/GET …/access-migration`.
- Hasta que existan: el detalle sigue mostrando IP; `POST` devolverá HTTP de error y el ViewModel lo muestra en `errorMessage`. No se hinchó `IRepository` ni `RestApiServices`.

## Archivos principales

- API: `presentation/.../data/datasource/remote/AccessMigrationApiService.kt`
- Repo: `AccessMigrationRepository` + `AccessMigrationRepositoryImpl`
- Poll: `PollAccessMigrationUseCase` (mismo patrón que `PollRegistrationProgressUseCase`)
- UDF: `AccessMigrationViewModel` + `AccessMigrationContent` embebido en `SubscriptionDetailScreen` (detalle Compose legado, cambio mínimo)
- DI: `apiModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`

## Tests (GREEN)

```bash
./gradlew :presentation:testDevDebugUnitTest \
  --tests "com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.accessmigration.AccessMigrationViewModelTest" \
  --tests "com.dscorp.ispadmin.domain.usecase.subscription.PollAccessMigrationUseCaseTest" \
  --tests "com.dscorp.ispadmin.domain.model.SubscriptionResponseAccessFieldsTest"
```

- ViewModel: PPPoE vs IP, lanza migración + poll hasta `DONE`, error `FAILED_STRANDED`
- Gson: JSON legado sin los campos nuevos sigue parseando
