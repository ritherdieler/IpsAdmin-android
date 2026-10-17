# Registro de suscripción: estado explícito, recuperación y privacidad (2026-10-01)

Contrato completo y backend: `ispadmin-backend/.agent-docs/registro-suscripcion-robustez.md`.

## Estado de envío

`RegisterSubscriptionState.submission: SubmissionState` (`Idle`, `Submitting`, `Reconciling`, `Uncertain`, `Provisioning`, `Completed`, `NeedsAttention`, `ProvisioningUnknown`). La pantalla de resultado lee `submission.resultSubscription()`; `UiEvent.Success` se sigue emitiendo pero ya no gobierna la UI (antes se perdía en segundo plano o al rotar). `DismissRegistrationResult` vuelve a `Idle`.

## Recuperación

- `SubmitAndTrackRegistrationUseCase`: clasifica el fallo (IO o `REGISTRATION_ALREADY_EXISTS` = incierto) y reconcilia con `GET onu-registration-operations/{id}/outcome`.
- Reapertura con operación vinculada: reanuda `pollRegistrationProgress` (job propio, se cancela al confirmar la cancelación).
- `PollRegistrationProgressUseCase`: tolera 3 errores seguidos, intervalo 2 → 5 s, propaga `CancellationException`.
- Preautorización asíncrona: si `start` devuelve `OLT_AUTHORIZATION` `PENDING`/`RUNNING`, el VM sondea `get` cada 3 s.
- Wireless: `clientRequestId` estable en `SavedStateHandle` (`register_subscription_client_request_id`).
- `BackHandler` con confirmación mientras `isRegistering`; «Ver progreso» navega a `Subscription.Provisioning(id)`.
- Aviso no bloqueante de DNI con `GET subscription/dni-check` (`SubscriptionActionsRepository.checkDni`).
- Contraseña WiFi 5 GHz: `resolvedWifiPassword5()` (5 GHz si existe, si no 2.4) en borrador y envío. Borrador cada 2 s.

## Observabilidad

Workflow `registro_suscripcion` con `workflowId = operationId`; `ObservabilityHttpInterceptor` envía `X-Operation-Id`. Eventos `subscription.register_reconciled`, `register_result_uncertain`, `register_abandoned_during_submit`.

## Privacidad

- `HttpLoggingInterceptor.BODY`, Chucker (`devImplementation`) y Stetho solo en flavor `dev`. Staging/prod borran `chucker.db` heredado al arrancar.
- `sanitizePayloads` por flavor; `ObsSanitize` por patrón (`password|token|dni|phone|…`) y recursivo; aplicado a tags, user y breadcrumbs.
- Replay suprimido en la pantalla de alta (`ObsReplayPrivacy`).
- El catálogo de cores ya no persiste usuario/contraseña del MikroTik en Room.

## E2E staging

`FiberRegisterFirstOnuE2ETest` ya no espera el paso «Serial:»: el asistente autoriza la ONU con el primer clic y la prueba espera directamente el formulario del cliente. El e2e sigue sin pasar: con instrumentación, tras el login la app queda en «Panel / Cargando…» aunque el backend responde 200. Se reproduce igual en `HEAD` sin estos cambios. Detalle en `ispadmin-backend/.agent-docs/registro-suscripcion-robustez.md` (sección E2E staging).

## Pantalla de éxito

- `RegistrationProgress.toSubscriptionOrNull(registration = …)` conserva la contraseña PPPoE de la respuesta del alta; el progreso nunca la trae.
- Nombre del suscriptor y plan en `colorScheme.primary` (antes `onPrimaryContainer` sobre `surfaceContainerHigh`, ilegible).
- Tag `RegisterSubscriptionTestTags.SUCCESS_SECTION_WIFI` en la tarjeta WiFi. El e2e valida SSID y clave ahí, porque ya no hay tarjeta de estado OLT/TR-069.

## Pendiente de release

Subir `versionCode`/`versionName` en `presentation/build.gradle` justo antes de la próxima subida a App Distribution.
