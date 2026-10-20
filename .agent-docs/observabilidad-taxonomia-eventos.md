# Observabilidad — Taxonomía de Eventos y Tracking Consistente

**Fecha**: 2026-07-12
**Módulo**: `:presentation` — `com.dscorp.ispadmin.observability`
**Alcance actual**: Fase 1 (piloto) — `login`, `subscription/register`

Estándar transversal para instrumentar features sobre el cliente in-house
(`ObservabilityClient`). No se agregan librerías nuevas: se reutilizan
`reportError()`, `reportLog()` y `addBreadcrumb()`, más el helper `runTracked {}`.

## 1. Nomenclatura

Los eventos y breadcrumbs siguen el patrón `feature.action`:

- `feature`: dominio funcional en minúsculas (`login`, `subscription`, `payment`).
- `action`: verbo o interacción específica en `snake_case` (`submit`, `save`,
  `check_version`, `refresh_onu_list`).
- Ejemplos: `login.submit`, `subscription.save`, `login.check_version`.

## 2. Tags obligatorios

Todo `reportError` / `reportLog` de una feature debe incluir:

| Tag | Obligatorio | Descripción |
|-----|-------------|-------------|
| `feature` | Sí | Dominio funcional (`login`, `subscription`, ...) |
| `screen` | Sí | Pantalla donde ocurre (`login`, `register_subscription`) |
| `action` | Sí | Acción específica (`submit`, `save`, ...) |
| `entityId` | Cuando aplique | Id de la entidad afectada (orden, suscripción, pago) |

El helper `obsTags(feature, screen, action, entityId, extra)` construye este mapa
en orden estable y permite agregar campos adicionales de contexto (`extra`).

## 3. Severidades permitidas

| Severidad | Cuándo usarla |
|-----------|---------------|
| `info` | Flujo normal, acción de usuario completada con éxito |
| `warning` | Validación bloqueada, estado degradado, camino alterno esperado |
| `error` | Excepción capturada, fallo de dominio, HTTP 4xx |
| `fatal` | Crash no controlado (lo emite el crash handler, no las features) |

Notas:
- HTTP `>= 500` se reporta como `error`; `4xx` como `warning` (lo hace el interceptor).
- Casos esperados de negocio (p. ej. "rostro no reconocido" que ofrece registro
  facial) NO se reportan como `error` para no generar ruido en el fingerprint.

## 4. Categorías de breadcrumb estandarizadas

| Categoría | Uso |
|-----------|-----|
| `navigation` | Entrada a pantalla, carga inicial de datos de una vista |
| `user_action` | Acción explícita del usuario (submit, save, delete, selección) |
| `network` | Llamadas de red (cubiertas automáticamente por el interceptor OkHttp) |
| `state` | Transiciones relevantes de la UI (éxito de operación, cambio de estado) |

El interceptor `ObservabilityHttpInterceptor` ya registra automáticamente las
llamadas de red (spans + `reportHttpError`), por lo que las features NO deben
duplicar breadcrumbs de red manuales.

## 5. Capa fina reutilizable

Archivo: `observability/ObservabilityExtensions.kt`.

- `runTracked(client, feature, screen, action, entityId, extra, ...) { block }`:
  envuelve una operación suspend. Registra breadcrumb `user_action` de inicio,
  ejecuta el bloque, registra breadcrumb `state` de éxito, y ante excepción llama
  a `reportError` con los tags estándar. Reexpone `CancellationException`.
  Devuelve `Result<T>` para que la feature decida el estado de UI.
- `trackScreen(client, feature, screen, data)`: breadcrumb `navigation` de entrada.
- `trackUserAction(client, feature, screen, action, data)`: breadcrumb `user_action`
  suelto (sin envolver una operación).
- `obsTags(...)`: builder de tags obligatorios.

Objetivo: reducir el `try/catch` repetido por feature y garantizar tags consistentes.

## 6. Manejo de PII / datos sensibles

Por decisión del proyecto, **no se aplica enmascaramiento**. Los datos se envían
tal como están (usuario, contraseñas ya cifradas con SHA-384 en origen,
coordenadas, ids). El pipeline de replay tampoco enmascara
(ver `observabilidad-replay-sin-enmascarado.md`).

## 7. Catálogo de acciones por feature (Fase 1)

### `login` (screen `login`)

| action | Tipo | Contexto |
|--------|------|----------|
| `submit` | `runTracked` | `checkedState` (recordar sesión) |
| `check_version` | `runTracked` | — |
| `biometric` | `runTracked` | reinicio de sesión guardada tras huella |
| `face_login` | breadcrumb + `reportError` | login facial; "rostro no reconocido" no se reporta |
| `face_enroll` | breadcrumb + `reportError` | registro de rostro del usuario logueado |

### `subscription` (screen `register_subscription`)

| action | Tipo | Contexto |
|--------|------|----------|
| `load_screen_data` | breadcrumb (`navigation`) | `orderId` |
| `load_initial_catalog` | `reportError` | `orderId` |
| `merge_installation_order` | `reportError` | `orderId` |
| `refresh_onu_list` | `reportError` | — |
| `resolve_place_from_location` | `reportError` | `latitude`, `longitude` |
| `fetch_nearby_nap_boxes` | `reportError` | `latitude`, `longitude` |
| `plan_selected` | breadcrumb (`user_action`) | `planId` |
| `place_selected` | breadcrumb (`user_action`) | `placeId` |
| `register_click` | breadcrumb (`user_action`) | `installationType`, `hasFacadePhoto`, `hasOrder` |
| `save_subscription_validation` | `reportLog` (`warning`) | `invalidFields[]` |
| `build_subscription` | `reportError` | — |
| `register_success` | breadcrumb (`state`) | `orderId` |
| `save_subscription` | `reportError` | `orderId`, `installationType`, `planId`, `placeId`, `hasNapBox`, `hasOnu`, `hasFacadePhoto` |
| `close_installation_order` | `reportError` | `orderId` |

### `edit_subscription` (screen `edit_subscription`)

| action | Tipo | Contexto |
|--------|------|----------|
| `load_form_data` | breadcrumb (`navigation`) + `reportError` | `entityId` = subscriptionId |
| `save_edit` | breadcrumb (`user_action`) + `reportError` + breadcrumb éxito (`state`) | `entityId` = subscriptionId |

### `subscription_detail` (screen `subscription_detail`)

| action | Tipo | Contexto |
|--------|------|----------|
| `load_detail` | breadcrumb (`navigation`) + `reportError` | `entityId` = subscriptionId |
| `update_facade_photo` | breadcrumb (`user_action`) + `reportError` + breadcrumb éxito (`state`) | `entityId` = subscriptionId |

### `subscription_finder` (screen `subscription_finder`)

| action | Tipo | Contexto |
|--------|------|----------|
| `find` | breadcrumb (`user_action`) + `reportError` en catch nuevo | `filterType` (tipo de filtro) |
| `cancel` | breadcrumb (`user_action`) + `reportError` en catch existente | `entityId` = subscriptionId |
| `reactivate` | `reportError` en `onFailure` existente | `entityId` = subscriptionId |
| `reboot_onu` | `reportError` en `onFailure` existente | `entityId` = subscriptionId |
| `load_nap_boxes` | breadcrumb (`user_action`) + `reportError` en catch existente | — |
| `change_nap_box` | breadcrumb (`user_action`) + `reportError` en catch existente | `entityId` = request.subscriptionId |
| `save_customer` | breadcrumb (`user_action`) + `reportError` en catch existente | `entityId` = subscriptionId |
| `update_location` | breadcrumb (`user_action`) + `reportError` en catch existente | `entityId` = subscriptionId |
| `load_places` | `reportError` en catch existente (método privado `getPlaces`) | — |

Notas de diseño para `subscription_finder`:
- Los 17 métodos que solo actualizan estado local (`showReactivateDialog`, `updateCustomerFormField`, etc.) no se instrumentan para no saturar la cola de breadcrumbs (máx. 50).
- `findSubscription` no tenía manejo de error en las ramas BY_DATE/BY_DOCUMENT/BY_NAME/BY_IP; se añade un `try/catch` envolvente en el cuerpo del `collect` que captura y reporta sin cambiar el tipo de retorno.
- La instrumentación es conservadora: no se usa `runTracked`, no cambian tipos de retorno ni flujos de control existentes.

### Fase 2 — `payment` (feature `payment`)

| screen | action | Tipo |
|--------|--------|------|
| `payment_detail` | `load_detail` | breadcrumb (`navigation`) + `reportError` (`entityId` = paymentId) |
| `payment_history` | `load_history` | breadcrumb (`navigation`) + `reportError` (`entityId` = subscriptionId) |
| `payment_history` | `reactivate` | breadcrumb (`user_action`) + `reportError` en `onFailure` |
| `payment_history` | `restore_internet` | breadcrumb (`user_action`) + `reportError` en `onFailure` |
| `register_payment` | `load_payment` | breadcrumb (`navigation`) + `reportError` (`entityId` = paymentId) |
| `register_payment` | `register` | breadcrumb (`user_action`) + `reportError` (`method`, `hasDiscount`) |
| `payer_finder` | `search_payer` | breadcrumb (`user_action`) + `reportError` |

### Fase 2 — `support_ticket` (feature `support_ticket`)

| screen | action | Tipo |
|--------|--------|------|
| `support_ticket_list` | `load_pending` / `load_in_progress` / `load_closed` | `reportError` en catch |
| `support_ticket_list` | `take` / `close_unattended` / `reschedule` / `close` | breadcrumb (`user_action`) + `reportError` (`entityId` = ticketId) |
| `support_ticket` (legacy) | `load_places` / `load_ticket` / `load_closed` / `load_pending` / `load_taken` | `try/catch` + `reportError` + `throw` dentro de `executeWithProgress` |
| `support_ticket` (legacy) | `take` | breadcrumb (`user_action`) |
| `create_support_ticket` | `load_places` / `find_subscription` | `reportError` en catch |
| `create_support_ticket` | `create` | breadcrumb (`user_action`) + `reportError` (`category`, `isClient`) |

### Fase 2 — `installation_order` (feature `installation_order`)

| screen | action | Tipo |
|--------|--------|------|
| `installation_order_list` | `load_orders` / `load_technicians` | `reportError` en catch |
| `installation_order_list` | `assign` / `transfer` | breadcrumb (`user_action`) + `reportError` (`entityId` = orderId, `technicianId`) |
| `create_installation_order` | `load_places` | `reportError` en catch |
| `create_installation_order` | `create` | breadcrumb (`user_action`) + `reportError` |

### Fase 2 — `dashboard` (feature `dashboard`)

| screen | action | Tipo |
|--------|--------|------|
| `dashboard` | `load_data` | breadcrumb (`navigation`) + `reportError` en `onFailure` |

### Fase 3 — resto de features

| feature | screen | action | Tipo |
|---------|--------|--------|------|
| `plan` | `plan_list` | `load_plans` | breadcrumb (`navigation`) + `reportError` en `onFailure` |
| `plan` | `plan_list` | `update` | breadcrumb (`user_action`) + `reportError` (`entityId` = planId) |
| `outlay` | `register_outlay` | `register` | breadcrumb (`user_action`) + `reportError` en `onFailure` |
| `report` | `reports` | `debtor_active`, `payment_commitment`, `suspended`, `cut_off`, `past_month_debtors`, `cancelled_current_month`, `cancelled_past_month`, `cut_off_candidates`, `debtor_cancelled` | helper `downloadReport`: breadcrumb (`user_action`) + `reportError` + `throw` |
| `user_registration` | `user_registration` | `submit` | breadcrumb (`user_action`) + `reportError` |
| `profile` | `profile` | `load_profile` | `reportError` en catch |
| `profile` | `profile` | `logout` | breadcrumb (`user_action`) + `reportError` |
| `olt` | `olt_administration` | `get_onu` | breadcrumb (`user_action`) + `reportError` |
| `olt` | `olt_administration` | `delete_onu` | breadcrumb (`user_action`) + `reportError` (`entityId`) |
| `nap_box` | `nap_box` | `register` / `edit` | breadcrumb (`user_action`) + `reportError` |
| `nap_box` | `nap_box` | `load_mufas` | `reportError` en catch |
| `mufa` | `mufa` | `load_mufas` | `try/catch` + `reportError` + `throw` dentro de `executeWithProgress` |
| `migration` | `migration` | `migrate` | breadcrumb (`user_action`) + `reportError` |
| `migration` | `migration` | `load_form_data` / `refresh_onus` | breadcrumb (`navigation`) + `reportError` |
| `main` | `main` | `load_current_user` | `reportError` en catch |
| `fixed_cost` | `fixed_cost` | `save` | breadcrumb (`user_action`) + `reportError` |
| `fixed_cost` | `fixed_cost` | `load_all` | `reportError` en catch |

Notas de diseño Fase 2 y 3:
- Inyección de `ObservabilityClient` por constructor (Koin) salvo `DashBoardViewModel`, `ProfileViewModel` y `MainActivityViewModel`, que usan `by inject()` / `KoinJavaComponent.inject` coherente con su patrón existente.
- ViewModels basados en `BaseViewModel` (`executeWithProgress`) usan el patrón `try/catch { reportError(...); throw }` para reportar sin alterar el manejo de estado/loaders del `BaseViewModel`.
- `ReportsViewModel` usa un helper privado `downloadReport(action) { ... }` para evitar duplicar la instrumentación en sus 9 descargas.
- No se instrumentó `FindSubscriptionViewModel` (código completamente comentado / muerto). `MainActivityViewModel` no instrumenta acciones de pantalla; solo reporta el evento de inventario `pending_subscriptions.count` (ver «Telemetría de registros pendientes»).
- Se mantiene el enfoque conservador: sin `runTracked`, sin cambios de tipos de retorno ni de flujo de control.

### Telemetría de registros pendientes (offline legado)

| message | Severidad | Tag | Cuándo se envía |
|---------|-----------|-----|-----------------|
| `pending_subscriptions.count` | `info` | `count` (entero ≥ 0) | Al iniciar la actividad con una sesión existente (una vez por proceso; `MainActivityViewModel`) |

- Emisor: `PendingSubscriptionsTelemetry.reportOncePerProcess()` (`reportLog` directo, sin `obsTags`: es un dato de inventario, no una acción de pantalla).
- `count` es la cantidad de registros de suscripción que siguen encolados en el teléfono.
- Motivo: decidir cuándo se puede eliminar la cola offline (entrega 2 de «eliminar soporte offline de registro»). El dato `count = 0` en toda la flota es lo que habilita ese corte.
- La guarda es por proceso (`AtomicBoolean`); si la lectura falla o se cancela, no se consume y se reintenta en la siguiente apertura.

## 8. Backend — indexado de `feature` y `action`

Para poder agrupar y filtrar por dominio, el backend extrae `feature` y `action`
del `tags_json` y los persiste como columnas indexadas en `obs_event`:

- `feature` (`VARCHAR(80)`, índice `idx_obs_event_feature`).
- `action` (`VARCHAR(120)`, índice `idx_obs_event_action`).

La extracción ocurre en `ObsIngestionService.persistEvent()` y no rompe el pipeline
de fingerprint/issues ni contratos existentes (los tags siguen en `tags_json`).

### Consumo de `feature`/`action` en consultas (2026-07-12)

- **Timeline por sesión filtrable por dominio:**
  `GET /observability/sessions/{sessionId}?feature=<f>&action=<a>` (ambos opcionales).
  Permite reconstruir solo el camino de un dominio dentro de la sesión previa al
  error. Sin parámetros, el comportamiento es idéntico al anterior (todos los
  eventos ordenados por `createdAt DESC`).
- **Desglose por dominio en el overview:**
  `GET /observability/stats/overview` incluye `eventsByFeature` (top 10 features
  por volumen de eventos en las últimas 24h), útil para identificar qué dominio
  genera más ruido/errores.

Ambos cambios son aditivos (parámetros opcionales / campo nuevo en el DTO) y
aprovechan el índice `idx_obs_event_feature`.

## 9. Fases

- **Fase 1 (completada)**: `login`, `subscription/register`.
- **Suscripciones (completada)**: `edit`, `detail`, `finder`.
- **Fase 2 (completada)**: `payment`, `supportTicket`, `installationorders`, `dashboard`.
- **Fase 3 (completada)**: `plan`, `outlay`, `report`, `registration`, `profile`, `oltadministrator`, `napbox`, `mufas`, `migration`, `main`, `fixedCost`.

## 10. Implementación suscripciones — edit + detail + finder (2026-07-12)

### Archivos modificados

| Proyecto | Archivo | Cambio |
|----------|---------|--------|
| Android | `features/subscription/edit/EditSubscriptionViewModel.kt` | Inyecta `ObservabilityClient`; breadcrumb + reportError en `getFormData` y `editSubscription` |
| Android | `features/subscriptiondetail/SubscriptionDetailViewModel.kt` | Inyecta `ObservabilityClient`; breadcrumb + reportError en `getSubscription` y `updateFacadePhoto` |
| Android | `features/subscriptionfinder/compose/SubscriptionFinderViewModel.kt` | Inyecta `ObservabilityClient`; instrumenta 9 métodos con addBreadcrumb + reportError sin cambiar flujo de control |
| Android | `di/ViewModelModule.kt` | `EditSubscriptionViewModel(get(), get())`, `SubscriptionDetailViewModel(get(), get())`, `SubscriptionFinderViewModel(get(), get(), get(), get())` |

### Validación

- `./gradlew :presentation:compileDevDebugKotlin` → **BUILD SUCCESSFUL** (exit 0, 2026-07-12).

## 10b. Implementación Fase 2 y 3 (2026-07-12)

### Archivos modificados

| Fase | Archivo | Cambio |
|------|---------|--------|
| 2 | `features/payment/detail/PaymentDetailViewModel.kt` | Inyecta client; breadcrumb + reportError en `fetchPaymentDetails` |
| 2 | `features/payment/history/PaymentHistoryViewModel.kt` | Inyecta client; instrumenta `getLastPayments`, `reactivateService`, `restoreInternetConnection` |
| 2 | `features/payment/register/RegisterPaymentViewModel.kt` | Inyecta client; instrumenta `LoadPaymentData`, `registerPayment` |
| 2 | `features/payment/payerFinder/PayerFinderViewmodel.kt` | Inyecta client; instrumenta `observeElectronicPayerSearch` |
| 2 | `features/supportTicket/list/compose/SupportTicketListViewModel.kt` | Inyecta client; instrumenta loaders + `take`/`close_unattended`/`reschedule`/`close` |
| 2 | `features/supportTicket/list/SupportTicketViewModel.kt` | Inyecta client; patrón `try/catch+throw` en métodos `executeWithProgress`/`executeNoProgress` + breadcrumb en `takeTicket` |
| 2 | `features/supportTicket/create/CreateSupportTicketViewModel.kt` | Inyecta client; instrumenta `getPlaces`, `findSubscriptionByNames`, `createTicket` |
| 2 | `features/installationorders/InstallationOrderListViewModel.kt` | Inyecta client; instrumenta `loadInstallationOrders`, `onLoadTechnicians`, `handleTechnicianOperation` |
| 2 | `features/installationorders/CreateInstallationOrderViewModel.kt` | Inyecta client; instrumenta `loadPlaces`, `createOrder` |
| 2 | `features/dashboard/DashBoardViewModel.kt` | `by inject()` client; instrumenta `getDashBoardData` |
| 3 | `features/plan/PlanListViewModel.kt` | Inyecta client; instrumenta `loadPlans`, `updatePlan` |
| 3 | `features/outlay/OutLayViewModel.kt` | Inyecta client; instrumenta `registerOutLay` |
| 3 | `features/report/ReportsViewModel.kt` | Inyecta client; helper `downloadReport` para las 9 descargas |
| 3 | `features/registration/RegisterViewModel.kt` | Inyecta client; instrumenta `registerUser` |
| 3 | `features/profile/ProfileViewModel.kt` | `KoinJavaComponent.inject` client; instrumenta `loadUserProfile`, `logOut` |
| 3 | `features/oltadministrator/OltAdministrationViewModel.kt` | Inyecta client; instrumenta `getOnuBySn`, `deleteOnuFromOlt` |
| 3 | `features/napbox/NapBoxViewModel.kt` | Inyecta client; instrumenta `registerNapBox`, `editNapBox`, `getMufas` |
| 3 | `features/mufas/MufaViewModel.kt` | Inyecta client; patrón `try/catch+throw` en `getMufas` |
| 3 | `features/migration/MigrationViewModel.kt` | Inyecta client; instrumenta `doMigration`, `getMigrationFormData`, `refreshOnus` |
| 3 | `features/main/MainViewModel.kt` | Inyecta client; instrumenta `loadCurrentUser` |
| 3 | `features/fixedCost/FixedCostViewModel.kt` | Inyecta client; instrumenta `saveFixedCost`, `getAllFixedCosts` |
| 2/3 | `di/ViewModelModule.kt` | Se añade `get()` extra de `ObservabilityClient` a los ViewModels afectados |

### Validación

- `./gradlew :presentation:compileDevDebugKotlin` → **BUILD SUCCESSFUL** (exit 0, 2026-07-12) para Fase 2 y para Fase 3.

## 11. Implementación Fase 1 (2026-07-12)

### Archivos creados / modificados

| Proyecto | Archivo | Cambio |
|----------|---------|--------|
| Android | `observability/ObservabilityExtensions.kt` | Nuevo: `runTracked`, `trackScreen`, `trackUserAction`, `obsTags`, `ObsBreadcrumbCategory` |
| Android | `features/login/LoginViewModel.kt` | Inyecta `ObservabilityClient`; instrumenta `doLogin` (submit), `checkAppVersion` (check_version), `loginWithSavedSession` (biometric), `doFaceLogin` (face_login), `registerFaceForLoggedUser` (face_enroll) |
| Android | `di/ViewModelModule.kt` | `LoginViewModel(get(), get())` |
| Android | `features/subscription/register/compose/RegisterSubscriptionComposeViewModel.kt` | Constante `OBS_FEATURE`; `feature` en todos los tags; categorías de breadcrumb estandarizadas (`navigation`/`user_action`/`state`); `entityId` en `save_subscription` y `close_installation_order` |
| Backend | `observability/entity/ObsEvent.kt` | Columnas `feature` (`VARCHAR(80)`) y `action` (`VARCHAR(120)`) + índices `idx_obs_event_feature`, `idx_obs_event_action` |
| Backend | `observability/service/ObsIngestionService.kt` | Extrae `feature`/`action` de `tags` al persistir (helper `tagString`) |
| Backend | `observability/dto/QueryDtos.kt` | `EventDto` y `ObsEvent.toDto` exponen `feature`/`action` (timeline por sesión) |

### Validación

- `./gradlew :presentation:compileDevDebugKotlin` → BUILD SUCCESSFUL.
- Backend `./mvnw -o compile` → BUILD SUCCESS.
- Prueba de flujo con android-cli (dispositivo físico `SM-S938B`, flavor `dev`):
  se instaló `installDevDebug`, se lanzó la app y se inspeccionó
  `files/observability/events-queue.jsonl` vía `run-as`. Se confirmó:
  - Breadcrumb `login.check_version` categoría `user_action` con
    `data = {feature: login, screen: login, action: check_version}`.
  - Evento `error` "Fallo al verificar la versión de la app" con
    `tags = {feature: login, screen: login, action: check_version}` y `severity: error`.
  - El archivo temporal de captura generado se eliminó al finalizar.

Notas:
- La validación de las columnas `feature`/`action` en backend se cubre por
  compilación; el dispositivo de prueba no tenía alcance de red al backend `dev`
  (`192.168.1.11:8080`), lo que a su vez sirvió para provocar y verificar el
  reporte de error instrumentado.
