# URL de producción — IpsAdmin Android

**Fecha de migración**: 2026-07-04

## URL actual

```
https://api.gigafiberperu.tech/ispadmin/
```

## URL anterior

```
http://212.85.13.47:8080/ispadmin/
```

El puerto 8080 fue restringido a `127.0.0.1` en el VPS después de activar HTTPS.
La URL anterior dejó de funcionar externamente tras el cambio.

## Archivo de configuración

`presentation/build.gradle` — flavor de producción/release:

```groovy
buildConfigField 'String', "BASE_URL", '"https://api.gigafiberperu.tech/ispadmin/"'
```

El flavor debug (`192.168.1.39`) no fue modificado.

> Observabilidad: el cliente de observabilidad usa una URL propia
> `OBS_BASE_URL` (buildConfigField por flavor), hoy igual a `BASE_URL`.
> Ver `.agent-docs/observabilidad-fase4-android.md`.

## Variantes de build (mapeo a producción)

El proyecto combina **flavors** (`environment`) con **build types**:

- Flavors (`presentation/build.gradle`):
  - `dev` → backend local (`http://192.168.1.11:8080/ispadmin/`), `applicationId` con sufijo `.dev`, `OBS_API_KEY = dev-obs-android-key`.
  - `prod` → backend de producción (`https://api.gigafiberperu.tech/ispadmin/`), `applicationId = com.dscorp.ispadmin` (sin sufijo), `OBS_API_KEY` inyectada desde `local.properties` (clave `OBS_API_KEY`, valor de la key `OBS_API_KEY_ANDROID` del backend).
- Build types: `debug` (instalable, firmado con la debug key) y `release` (**sin `signingConfig`** → APK sin firmar, no instalable directamente).

**`prodDebug` es la variante equivalente a producción** que se usa para instalar/probar en dispositivos: apunta al backend de prod (URLs + `OBS_API_KEY` de prod) y es instalable porque el build type `debug` sí queda firmado. `prodRelease` compila contra prod pero queda sin firmar hasta que se configure un `signingConfig`, por lo que hoy no se instala directamente.

Instalación en dispositivo (flavor prod, equivalente a producción):

```bash
./gradlew :presentation:installProdDebug
# o compilar y luego instalar el APK:
./gradlew :presentation:assembleProdDebug
adb -s <serial> install -r presentation/build/outputs/apk/prod/debug/*.apk
```

## Tras cambiar la URL

Requiere nuevo build y distribución del APK/AAB a los dispositivos en uso. Para
distribución oficial se debe configurar un `signingConfig` en `release` y usar:

```bash
./gradlew bundleProdRelease   # requiere signingConfig configurado
```

Para pruebas/instalación directa en dispositivos, usar `prodDebug` (ver sección anterior).

## Verificación

Hacer login desde la app y confirmar que carga suscripciones sin errores de red.
