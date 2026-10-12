# Timeout HTTP del alta FIBER (2026-09-15)

El e2e Espresso staging (`FiberRegisterFirstOnuE2ETest`) llegó a submit pero no a `register_success_fullscreen` (360 s). En Core quedó `#8` `gf8` / `ZTEGDC47BFFD` con OLT+TR-069+MK `COMPLETE`.

Causa: `POST /subscription` es síncrono (OLT + MikroTik + TR-069) y el `OkHttp` de la app cortaba a **90 s**. El servidor terminaba; la UI mostraba error y el test no veía éxito.

## Cambio

`provideHttpClient` usa `readTimeout` de **4 minutos**.

Prueba: `RetrofitModuleTimeoutTest.provideHttpClient_readTimeout_covers_fiber_olt_tr069`.
