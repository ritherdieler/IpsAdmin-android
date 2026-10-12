# E2E Espresso — registro FIBER (primera ONU)

En Android solo hay dos scripts de alta: producción y staging. Staging habla con KVM4 en `https://api.gigafiberperu.tech/ispadmin-staging/`. El helper de consola es `scripts/e2e_console.sh`.

Orquesta instrumented Compose/Espresso + limpieza dura §4 del backend.

## Cómo correr

**Antes de cualquier e2e:** cold boot del emulador (`-no-snapshot-load`, sin reutilizar sesión previa). Detalle: `ispadmin-backend/.agent-docs/staging-fiber-e2e-runbook.md` § «Cold boot del emulador».

Tras el login ADMIN el grafo arranca en **Panel**, que muestra `CircularProgressIndicator` mientras carga el dashboard. En Compose UI Test 1.9, `waitForIdle()` no termina con esa animación infinita: el e2e se queda en «Panel / Cargando» y nunca ve `FORM_READY`. Los polls del instrumented (`waitUntilTag` / `waitUntilAnyTag`) **no** llaman `composeRule.waitForIdle()`; esperan con `Thread.sleep`. El `NavHost` de features solo se monta cuando ya hay usuario (destino inicial estable).

```bash
# Emulador/dispositivo con ≥1 ONU unconfigured en SmartOLT
cd "IpsAdmin-android app"
./scripts/e2e_register_fiber_espresso.sh
```

Consola: hitos de color en `scripts/e2e_console.sh` (login/alta/ACS/HTTP/OK/FAIL). Sin TTY: prefijos `OK`/`FAIL`/`WAIT`.

Variables útiles: `E2E_DNI`, `E2E_PLACE`, `E2E_WIFI_SSID`, `E2E_WIFI_PASS`, `BACKEND_ROOT`, `DEVICE`, `PACKAGE`.

Staging (ONU lab + polígono `place`):

```bash
# 1) cold boot medium_phone (-no-snapshot-load) y esperar sys.boot_completed=1
# 2) luego:
cd "IpsAdmin-android app"
E2E_ONU_SN=ZTEGDC47BFFD ./scripts/e2e_register_fiber_staging_espresso.sh
```

Las coords (`GEO_LAT` / `GEO_LON`) tienen que estar **dentro** de un `place.area` (polígono), no solo cerca del pin del lugar. Validar con `GET /place/findByLocation`. Fixture: [e2e-register-fiber-staging-lab-onu-2026-09-01.md](./e2e-register-fiber-staging-lab-onu-2026-09-01.md). Runbook: `ispadmin-backend/.agent-docs/staging-fiber-e2e-runbook.md` § «Coordenadas dentro de polígonos».

## Qué hace el wrapper

1. Pre-cleanup `--dni` (idempotente / `--allow-empty`)
2. `adb emu geo fix` lab + `pm clear`
3. `connectedProdDebugAndroidTest` → `FiberRegisterFirstOnuE2ETest` (elige **`register_onu_item_0`**)
4. **Ping MikroTik2 → IP asignada** (`tr069-e2e-mk-ping.sh --dni`) — **obligatorio** para considerar la prueba exitosa; debe responder antes del cleanup
5. Post-cleanup obligatorio vía [`ispadmin-backend/scripts/tr069-e2e-hard-cleanup.sh`](../../ispadmin-backend/scripts/tr069-e2e-hard-cleanup.sh)

### Criterio de éxito

Exitoso **solo** si el wrapper imprime `E2E_FIBER_STAGING_ESPRESSO_OK` / equivalente prod y sale 0: UI con **autorización OLT** (`oltProvisionStatus=COMPLETE`) + **ACS** (`tr069ProvisionStatus=COMPLETE`) + alta + **verificación de red post-alta (ping MK2)** + cleanup. Si el ping falla o se corta (proceso abortado), **no** es éxito aunque Espresso haya pasado. El cleanup **igual** corre cuando el ping falla y el wrapper sale ≠ 0.

WiFi del alta (defaults): `E2E_WIFI_SSID` / `E2E_WIFI_PASS` (5 GHz = `{ssid} - 5G` si nombres unificados).

## Flujo UI (wizard)

Alineado a `feature/manual-location-selection`:

1. **Cliente + ubicación** — datos, lugar, dirección, `location_method_manual` (coords del polígono) → `wizard_continue`
2. **Instalación** — FIBER, plan, ONU `register_onu_item_0`, WiFi, NAP y foto de fachada (broadcast DEBUG, ver abajo) → `wizard_continue`
3. **Confirmación** — revisión → `btn_register_subscription`

## Cleanup (fuente de verdad)

Runbook: `ispadmin-backend/.agent-docs/tr069-e2e-validacion-modelo.md` §4  
Script: MikroTik → SmartOLT → Firebase Storage (`facades/`) → MySQL → purge ACS.

Credencial Firebase: `FIREBASE_SERVICE_ACCOUNT_JSON` (default `firebase_service_account_prod.json` en resources del backend). Ver catálogo de secretos.

## Debug foto fachada y NAP (prod y staging)

Solo builds **`debug`** (`prodDebug`, `stagingDebug`): `BuildConfig.DEBUG` registra receivers en `RegisterSubscriptionScreen`.

| Acción | Broadcast | Extra |
|--------|-----------|-------|
| Foto fachada | `com.dscorp.ispadmin.DEBUG_SET_FACADE_PHOTO` | `path` = ruta absoluta a un `.jpg` |
| NAP | `com.dscorp.ispadmin.DEBUG_SET_NAP_CODE` | `code` = ej. `NO-001` |

El e2e **no** usa cámara ni selector de galería. Copia `presentation/src/androidTest/assets/facade_e2e.jpg` y lo inyecta con el broadcast de fachada. Cualquier JPEG válido alcanza; el backend no valida el contenido. Staging usa el mismo mecanismo vía `connectedStagingDebugAndroidTest`.
