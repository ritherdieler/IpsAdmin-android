# Android prod (prodDebug) y observabilidad

## Variante de produccion

La app en campo se distribuye como **`prodDebug`** (`com.dscorp.ispadmin`), no `prodRelease`.

```bash
./gradlew :presentation:installProdDebug
```

## Clave de observabilidad

El flavor `prod` embebe en `BuildConfig.OBS_API_KEY` el valor de **`OBS_API_KEY_ANDROID`** en `local.properties` (misma variable que el backend en `/opt/gigafiber/.env`).

Sincronizar desde el VPS:

```bash
./scripts/sync-obs-android-key-from-prod.sh
```

Si falta la clave, Gradle falla al compilar/instalar variantes `prod*`.

`OBS_API_KEY` en `local.properties` solo se usa como fallback deprecado.

## Build

```bash
./gradlew :presentation:testProdDebugUnitTest
./gradlew :presentation:compileProdDebugSources
```
