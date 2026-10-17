# Reintento TR-069 post-registro (Android)

Fecha: 2026-09-21

## Cableado

```text
RegisterSuccessFullScreen (btn_retry_tr069)
  → RegisterSubscriptionIntent.RetryTr069(subscription)
  → RetryTr069ProvisioningUseCase
  → POST /subscription/{id}/acs/retry-tr069
  → si PENDING: PollRegistrationProgressUseCase (fromRetry=true)
  → Success / Error
```

## Feedback de reintento

- Al pulsar: `tr069RetryLoading=true` + mensaje `Reintentando aprovisionamiento TR-069…`.
- Card `tr069_retry_feedback` dentro del diálogo de éxito (el overlay global queda detrás del Dialog).
- Botón muestra spinner + texto `Reintentando…`; Continuar deshabilitado.
- Durante el poll post-retry se mantiene `tr069RetryLoading` (no `isLoading`) y se actualiza el mensaje con el progreso ACS.
- Al terminar: se limpia loading y se emite Success/Error.

## Overlay al abrir el formulario

`isLoading` (catálogo) y `isRegistering` (POST + poll) están separados. Al entrar a la vista solo se muestra `CatalogLoadingOverlay` (“Cargando…”). El overlay “Registrando…” / “hasta 2 minutos” aparece únicamente cuando `isRegistering=true`.

## Comportamiento

- Botón visible si `tr069ProvisionStatus` es `PENDING`, `MANUAL_REQUIRED` o `FAILED` y hay `subscriptionId`.
- Al reintentar se limpia el diálogo de error previo.
- Se conservan SSIDs/passwords WiFi del alta mientras se polla.

## Endpoint backend

`POST /subscription/{subscriptionId}/acs/retry-tr069` — admite `PENDING`, `MANUAL_REQUIRED` y `FAILED` con OLT `COMPLETE`.

Con `service-health` activo, `LegacyTechnicalActionFilter` exige headers:
- `X-Confirm-Action: true`
- `Idempotency-Key` (UUID, 8–128 chars `[A-Za-z0-9_-]`)

Sin esos headers el API responde **400** `"Confirme la acción"`. Android los envía en reintento TR-069 y reinicio ONU (mismo contrato que el backoffice).

## Listado de suscripciones

El buscador (`GET /subscription/search`) ya trae `tr069ProvisionStatus` en el DTO. `SubscriptionResponse.toDomain()` lo copia a `SubscriptionResume`.

Visible (botón en la tarjeta y opción de menú) solo si la instalación es fibra o TV+fibra, el servicio no está cancelado, y el estado es `PENDING`, `MANUAL_REQUIRED` o `FAILED`. `COMPLETE`, `NA` y `null` no muestran reintento.

```text
btn_list_retry_tr069_{id} / menú RETRY_TR069
  → diálogo de confirmación (spinner "Reintentando...")
  → RetryTr069ProvisioningUseCase
  → snackbar con el mensaje de resultado
  → la tarjeta actualiza tr069ProvisionStatus (si queda COMPLETE, el botón desaparece)
```

No hay poll largo en el listado: el resultado es el de la llamada. Si sigue pendiente, el snackbar muestra `tr069Message` y el botón permanece. Si el backend responde 400 (OLT no COMPLETE), el snackbar muestra ese texto.
