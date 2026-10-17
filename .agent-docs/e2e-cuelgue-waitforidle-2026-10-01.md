# Cuelgue e2e instrumentado — Panel / Cargando

Fecha: 2026-10-01.

## Qué pasaba

El e2e de alta FIBER (`FiberRegisterFirstOnuE2ETest`) se quedaba tras el login en la pantalla **Panel** con el texto **Cargando**. El hilo de instrumentación no avanzaba; `FORM_READY` nunca aparecía.

## Causa

1. Tras el login, `FeatureNavGraph` recalculaba `startDestination` cuando llegaba el usuario (primer ítem del drawer ADMIN = Dashboard). El `NavHost` se remontaba en Panel.
2. El dashboard muestra `Loader()` (`CircularProgressIndicator` indeterminado) mientras `isLoading && dashboardData == null`.
3. Los helpers del e2e llamaban `composeRule.waitForIdle()` en cada poll. Compose UI Test 1.9 no considera idle esa animación: el `waitForIdle` no vuelve. El drawer ni el formulario se siguen sondando.

Un alta manual (android-cli, sin `waitForIdle`) sí llegaba al formulario.

## Cambio

- `FeatureNavGraph`: no montar el `NavHost` hasta que exista `currentUser` y un destino de drawer; así el grafo no arranca en Perfil y luego se recrea en Panel.
- E2E: quitar `waitForIdle` de los bucles de espera (`FiberRegisterFirstOnuE2ETest`, `SubscriptionSearchStagingE2ETest`).
