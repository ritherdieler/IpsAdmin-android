# Observabilidad prodDebug — API key

## Problema (2026-08)

En **prodDebug**, los eventos de error no creaban sesiones en el dashboard porque la ingestión a prod respondía **401**.

Causa: el plugin **Maps Secrets** inyectaba `OBS_API_KEY=dev-obs-android-key` desde `local.properties` (clave deprecada) y **sobrescribía** el `buildConfigField` del flavor `prod`. El código usaba solo `BuildConfig.OBS_API_KEY` contra `https://api.gigafiberperu.tech/ispadmin/`.

## Solución

1. `presentation/build.gradle`: `OBS_API_KEY` y `OBS_API_KEY_ANDROID` en `secrets.ignoreList`.
2. Flavors definen explícitamente `OBS_API_KEY` y `OBS_API_KEY_ANDROID` (prod: ambas desde `resolveProdObsApiKey()`).
3. `ObservabilityModule.resolveObservabilityApiKey`: flavor `dev` → `dev-obs-android-key`; flavor `prod` → `ProdObsApiKeyResolver` (prioriza `OBS_API_KEY_ANDROID`).

## Build local prod

- Obligatorio: `OBS_API_KEY_ANDROID` en `local.properties` (misma cadena que VPS / backend prod).
- `OBS_API_KEY` en `local.properties` ya no afecta `BuildConfig`; puede eliminarse para evitar confusión.
- Tras cambiar claves: `./gradlew :presentation:installProdDebug`.

## Verificación

- Logcat prodDebug: `POST .../observability/events` → **202**, no 401.
- Dashboard sesiones: release Android `versionName (versionCode)`, filtro “Todas las versiones” si hace falta.
