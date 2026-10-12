# Secretos de build — IpsAdmin Android

## Fuente de verdad

Todos los secretos de build viven en `local.properties` (gitignored).
La plantilla versionada es `local.properties.example`.

```bash
cp local.properties.example local.properties
# Completar valores reales
```

## Catálogo de claves

| Clave | Consumidor | Obligatoria |
|---|---|---|
| `sdk.dir` | Android SDK | Sí (Android Studio) |
| `gpr.user` / `gpr.key` | GitHub Packages (`com.dscorp:components`) vía `settings.gradle` | Sí para build |
| `MAPS_API_KEY` | Google Maps (`AndroidManifest` vía `secrets-gradle-plugin`) | Sí |
| `OBS_API_KEY` | Observabilidad prod (`BuildConfig.OBS_API_KEY`) | Solo prod / release |
| `SEND_CLOUD_MESSAGING_API_KEY` | FCM legacy (`SendMessagingCloudApi`) | Sí si se usa envío FCM server-side |

## Flujo de inyección

```
local.properties.example  -->  local.properties (gitignored)
                                      |
                    +-----------------+------------------+
                    |                                    |
           secrets-gradle-plugin              gradle/secrets.gradle
                    |                                    |
         AndroidManifest ${MAPS_API_KEY}      BuildConfig (OBS, FCM)
```

- Maps: plugin `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` lee `local.properties` y sustituye `${MAPS_API_KEY}` en el manifest.
- El bloque `secrets { ignoreList }` excluye `sdk.*`, `gpr.user` y `gpr.key` para que no entren en `BuildConfig` ni en el APK.
- OBS / FCM: `presentation/build.gradle` carga claves con helpers de `gradle/secrets.gradle` y las inyecta como `buildConfigField`.

## Por flavor

| Campo | `dev` | `prod` |
|---|---|---|
| `OBS_API_KEY` | Hardcoded `dev-obs-android-key` (también forzado en runtime por `resolveObservabilityApiKey`) | Valor de `local.properties` o env `OBS_API_KEY` |
| `SEND_CLOUD_MESSAGING_API_KEY` | Desde `local.properties` | Desde `local.properties` |
| `BASE_URL` / `OBS_BASE_URL` | `http://127.0.0.1:8080/ispadmin/` | `https://api.gigafiberperu.tech/ispadmin/` |

## Firebase

Solo se versionan las fuentes por flavor:

- `presentation/src/dev/google-services.json`
- `presentation/src/prod/google-services.json`

Durante el build, Gradle copia el archivo del flavor activo a `presentation/google-services.json` (gitignored).

## Qué no va en el repo

- `local.properties` (secretos reales)
- `presentation/google-services.json` (generado)
- Keystores de release
- Credenciales E2E reales (los scripts de alta FIBER usan defaults sobrescribibles por env)

## Pendiente (fuera de alcance de esta limpieza)

- Rotar / restringir la Maps API key en Google Cloud Console (package + SHA-1)
- Cifrado de sesión en runtime (`SharedPreferences`)
- Restringir `HttpLoggingInterceptor` / Stetho a debug
- Integración Infisical para generar `local.properties`

## Verificación

```bash
./gradlew :presentation:testDevDebugUnitTest \
  --tests "com.dscorp.ispadmin.secrets.SecretsManifestTest" \
  --tests "com.dscorp.ispadmin.di.ObservabilityDevApiKeyTest"

./gradlew :presentation:compileDevDebugSources :presentation:compileProdReleaseSources
```
