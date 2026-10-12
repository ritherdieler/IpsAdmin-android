# Observabilidad — Instrumentación de Registro de Suscripción

**Fecha**: 2026-07-11
**Módulo**: `:presentation` — `com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose`

Instrumentación de la vista de registro de suscripción sobre el cliente de
observabilidad in-house (ver `observabilidad-fase4-android.md`). No se agregan
librerías ni dependencias nuevas: se reutilizan `ObservabilityClient.reportError()`,
`reportLog()` y `addBreadcrumb()`.

## Motivación

El interceptor OkHttp ya captura errores HTTP (`>= 400`) y fallos de red, pero los
fallos de **dominio** de esta pantalla (use cases que devuelven `Result.failure`,
usuario de sesión nulo, validación de formulario) se mostraban al usuario y se
descartaban sin dejar rastro. Esta vista es crítica (alta de clientes en campo),
por lo que se añadió trazabilidad completa: breadcrumbs de acciones → error de
negocio con contexto → correlación con el evento HTTP del interceptor.

## Cambios

### `RegisterSubscriptionComposeViewModel.kt`

- Se inyecta `observabilityClient: ObservabilityClient` en el constructor
  (antes de `mainImmediate`, que conserva su valor por defecto).
- Constante `OBS_SCREEN = "register_subscription"` para el tag `screen`.

Instrumentación por método:

| Método | Evento | Tipo | Tags/datos relevantes |
|--------|--------|------|-----------------------|
| `loadScreenData` | `load_screen_data` | breadcrumb | `orderId` |
| `loadScreenData` (catálogo) | `load_initial_catalog` | `reportError` | `orderId` |
| `loadScreenData` (orden) | `merge_installation_order` | `reportError` | `orderId` |
| `refreshOnuList` | `refresh_onu_list` | `reportError` | — |
| `resolvePlaceFromLocation` | `resolve_place_from_location` | `reportError` | `latitude`, `longitude` |
| `fetchNearbyNapBoxes` | `fetch_nearby_nap_boxes` | `reportError` | `latitude`, `longitude` |
| `onPlanSelected` | `plan_selected` | breadcrumb | `planId` |
| `onPlaceSelected` | `place_selected` | breadcrumb | `placeId` |
| `saveSubscription` (clic) | `register_click` | breadcrumb | `installationType`, `hasFacadePhoto`, `hasOrder` |
| `saveSubscription` (validación) | `save_subscription_validation` | `reportLog` (warning) | `invalidFields[]` |
| `saveSubscription` (usuario nulo) | `build_subscription` | `reportError` | — |
| `saveSubscription` (éxito) | `register_success` | breadcrumb | `orderId` |
| `saveSubscription` (fallo) | `save_subscription` | `reportError` | `orderId`, `installationType`, `hasFacadePhoto`, `planId`, `placeId`, `hasNapBox`, `hasOnu` |
| `closeInstallationOrder` | `close_installation_order` | `reportError` | `orderId` |

Notas de diseño:
- El fallo de validación se registra como `warning` con la lista de campos
  inválidos (`invalidFields`) para detectar problemas de UX invisibles hoy
  (p. ej. técnicos que no logran registrar por falta de foto de fachada).
- Se evitó dejar breadcrumbs en los cambios de campo de texto
  (`onFirstNameChanged`, `onDniChanged`, etc.) para no saturar la cola de
  breadcrumbs (máx. 50 en memoria).
- Todos los `reportError`/`reportLog` conservan la emisión de
  `RegisterSubscriptionUiEvent.Error` original; solo se añade el reporte.

### `di/ViewModelModule.kt`

- Se agrega un `get()` adicional al provider de `RegisterSubscriptionComposeViewModel`
  para inyectar el `ObservabilityClient` (registrado como `single` en `observabilityModule`).

### `RegisterSubscriptionComposeViewModelTest.kt`

- Se pasa `observabilityClient = mockk(relaxed = true)` a la construcción del ViewModel.

## Validación

- `./gradlew :presentation:compileDevDebugKotlin` → `BUILD SUCCESSFUL`.
- `./gradlew :presentation:testDevDebugUnitTest --tests "*RegisterSubscriptionComposeViewModelTest*"` → `BUILD SUCCESSFUL`.
