# Compose: testTag y contentDescription

**Fecha**: 2026-07-14

## Regla

Todo componente visual Compose debe tener **al menos uno** de:

| Tipo de componente | Mínimo obligatorio | Recomendado |
|--------------------|--------------------|-------------|
| Interactuable (click, focus, input, checkbox, switch, botón, enlace) | `Modifier.testTag(...)` | También `contentDescription` / label visible cuando aporte |
| No interactuable (imagen, ícono decorativo/informativo, ilustración) | `contentDescription` no vacío | — |

**Nunca** dejar un componente visual sin `testTag` ni `contentDescription` (incluido `contentDescription = null` o `""` en imágenes/íconos con significado).

## Por qué

- Observabilidad e instrumentación resuelven el target UI por semantics (`testTag` > descripción > texto).
- Sin ninguno de los dos, el hit-test de Android cae en `AndroidComposeView` y el evento no identifica el control.
- Los `testTag` estables no dependen del idioma ni del copy de UI.

## Convención de nombres

- Prefijo por pantalla/flujo: `login_submit`, `login_username`.
- Constantes en un objeto por feature (ej. `LoginTestTags`, `LoginContentDescriptions`).
- No reutilizar el mismo tag en pantallas distintas.

## Exposición a UI Automator / e2e

En la raíz Compose (`TestTagResourceIdRoot` en `MainActivity`) se activa `testTagsAsResourceId = true`.
Así `android layout` / UI Automator ven el `testTag` como `resourceId` y el e2e puede hacer tap por tag
(nunca por coordenadas fijas). El login del e2e va dentro de `e2e_register_fiber_espresso.sh` y `e2e_register_fiber_staging_espresso.sh`.

## Tracking de clicks Compose

Automático (estilo Sentry): `ObservabilityComposeGestureLocator` resuelve `testTag` bajo el tap
vía `LayoutNode` + `TestTag` / `TestTagElement`. Ver `observabilidad-compose-gesture-locator.md`.

El tracker omite hosts Compose genéricos si no hay tag.

## Ejemplo (login)

- Interactivos: `login_username`, `login_password`, `login_password_visibility`, `login_remember_session`, `login_submit`, `login_biometric`, `login_create_account`.
- No interactivos: logo e ícono biométrico con `LoginContentDescriptions`.

## Checklist al agregar UI

1. ¿El usuario puede interactuar con el composable? → `testTag` obligatorio.
2. ¿Es solo visual? → `contentDescription` obligatorio y significativo.
3. ¿Tiene al menos uno de los dos? Si no, no mergear.
