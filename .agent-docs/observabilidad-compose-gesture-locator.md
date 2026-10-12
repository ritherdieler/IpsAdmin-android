# Tracking automático de testTag (estilo Sentry)

**Fecha**: 2026-07-14

## Enfoque

Igual que Sentry Compose user interactions:

1. Captura global de toques vía `Window.Callback` (`ObservabilityUiTracker`).
2. Hit-test en el árbol interno de Compose (`LayoutNode` / `Owner`), no en Views.
3. Identificador = `SemanticsProperties.TestTag` o campo `tag` de `TestTagElement` (Compose 1.5+).
4. Emite si el nodo es interactivo:
   - click: `OnClick` / `ClickableElement` / `CombinedClickableElement`
   - texto: `EditableText` / `SetText` / `RequestFocus`
   - toggle: `ToggleableState` / `ToggleableElement`
5. Fallback: si no hay nodo interactivo, usa el `testTag` más profundo bajo el punto (cubre TextFields/interop).

Implementación: `ObservabilityComposeGestureLocator` + `ObservabilityComposeBoundsHelper` (Java para APIs internas de Compose, como Sentry).

## Requisitos en UI

- Controles interactivos con `modifier.testTag("...")`.
- `TestTagResourceIdRoot` (`testTagsAsResourceId = true`) sigue siendo útil para e2e/UIAutomator; el tracking automático usa el árbol `LayoutNode`.

## Referencias

- https://blog.sentry.io/how-we-built-user-interaction-tracking-for-jetpack-compose/
- https://docs.sentry.io/platforms/android/integrations/jetpack-compose/
