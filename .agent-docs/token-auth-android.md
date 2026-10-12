# Autenticación por token (Bearer + refresh) en la app Android

## Contexto

El backend ahora exige `Authorization: Bearer <accessToken>` en todos los endpoints
salvo los públicos. Login normal y facial devuelven `accessToken` y `refreshToken`
dentro del `UserDto`. Ante un `401` se debe renovar con `POST users/token/refresh` y
reintentar; si la renovación también falla, se limpia la sesión y se fuerza el login.

## Componentes agregados

Paquete `com.dscorp.ispadmin.data.datasource.remote.auth`:

- `TokenStore`: lee/escribe/limpia `accessToken` y `refreshToken` en el
  `SharedPreferences "my_prefs"` (claves `access_token`, `refresh_token`).
  `clearSession()` borra las claves de la sesión activa (no la copia biométrica).
- `AuthApiService` + `RefreshTokenRequest` / `RefreshTokenResponse`: endpoint
  `POST users/token/refresh`. Se consume con un Retrofit/OkHttp mínimo propio para
  evitar reentrar en el `Authenticator`.
- `AuthInterceptor`: añade el header `Authorization: Bearer <accessToken>` a las
  peticiones dirigidas al host del backend, excepto rutas públicas.
- `TokenAuthenticator` (`okhttp3.Authenticator`): se dispara en `401`, renueva el
  token de forma síncrona y reintenta la request original. Si no hay refresh token,
  el refresh falla o se supera el número de intentos, limpia la sesión y emite el
  evento de sesión expirada.
- `SessionEventBus`: `SharedFlow` que notifica "sesión expirada" a la UI.

## Rutas públicas (sin token, no fuerzan login)

`users/login`, `users/login/face`, `users/login/face/photo`, `users/token/refresh`,
`app/check_version`, `fcm/save-token` y cualquier ruta que contenga `webhook`.
El interceptor también ignora hosts distintos al del backend (p. ej. FCM
`fcm.googleapis.com`).

## Persistencia de tokens

- `domain/model/User.kt`: campos `accessToken` y `refreshToken` (nullable). Gson los
  mapea automáticamente desde la respuesta de login.
- `data/repository/Repository.kt`: `saveUserSession`/`saveBiometricUserSession`
  persisten los tokens; `getUserSession`/`getBiometricUserSession` los leen;
  `clearUserSession` los borra. La copia biométrica guarda sus propios tokens para
  que el login por huella restaure una sesión con tokens (el refresh los renueva).

## Cableado

`di/RetrofitModule.kt` registra `SessionEventBus`, `TokenStore`, `AuthApiService`,
`AuthInterceptor` y `TokenAuthenticator`. `provideHttpClient` agrega el
`AuthInterceptor` y configura `authenticator(tokenAuthenticator)`.

`navigation/IpsAdminNavHost.kt` colecta `SessionEventBus.sessionExpired` y navega a
`NavRoutes.Auth` limpiando el back stack cuando la sesión expira.

## Flujo 401 -> login

1. Request con token vencido -> `401`.
2. `TokenAuthenticator` llama `users/token/refresh` con el refresh token.
3. Éxito -> guarda tokens nuevos y reintenta la request original con el nuevo access.
4. Falla (o sin refresh token / reintentos agotados) -> `TokenStore.clearSession()` +
   `SessionEventBus.notifySessionExpired()` -> el NavHost lleva a la pantalla de login.

## Build

`./gradlew :presentation:assembleDebug` -> BUILD SUCCESSFUL (variantes devDebug y
prodDebug). Solo advertencias preexistentes de deprecaciones.
