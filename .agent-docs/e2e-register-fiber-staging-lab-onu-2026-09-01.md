# E2E registro FIBER Android — fixture staging (2026-09-01)

Datos para **replicar** el alta de una suscripción FIBER desde la app contra `ispadmin-staging`, con la ONU de lab ZTE y coordenadas **dentro de un polígono** de `place`.

Runbook maestro (seed, red MK2 `.250`, deploy, cleanup Firebase): `ispadmin-backend/.agent-docs/staging-fiber-e2e-runbook.md`.

## Cómo correr

**Antes:** cold boot del AVD `medium_phone` con `-no-snapshot-load` (ver runbook § «Cold boot del emulador»). No reutilizar emulador de un intento previo.

```bash
cd "IpsAdmin-android app"
E2E_ONU_SN=ZTEGDC47BFFD E2E_WIFI_SSID=lab-zte-e2e-24 E2E_WIFI_PASS='LabZteWifi24!' \
  ./scripts/e2e_register_fiber_staging_espresso.sh
```

Variante: `stagingDebug` / `connectedStagingDebugAndroidTest`. Package: `com.dscorp.ispadmin` (sin suffix). API: `https://api.gigafiberperu.tech/ispadmin-staging`.

El wrapper exige que `GET /place/findByLocation` resuelva el lugar con `GEO_LAT` / `GEO_LON`. Si falla, reaplicar el seed de catálogo.

## Catálogo staging (obligatorio, copiar desde prod)

Seed idempotente:

```bash
mysql < ispadmin-backend/scripts/sql/staging-e2e-registration-catalog.sql
```

Tablas: `place`, `mufa`, `nap_box`, `plan`, `network_device` (MK id=8), `ip_pool` (`192.168.250.1/24`). Detalle: `ispadmin-backend/.agent-docs/staging-e2e-registration-catalog-2026-09-01.md`.

Aplicado 2026-09-01: **24** places (22 con `area`), **162** nap_box, plan FIBER `lab-basico`, MK2 host 8.

Regla para pruebas futuras:

1. Copiar **toda** la tabla `place` de `ispadmin` (no un solo id).
2. Pegar en el mapa las columnas `latitude` / `longitude` de una fila con `area IS NOT NULL`.
3. Confirmar con `GET /place/findByLocation?latitude=…&longitude=…` (status 200).
4. No usar el centro del envelope: en `san bosco` y `la merced` queda fuera del polígono.
5. Sin polígono (no sirven): `san miguel` (id 10), `DESCONOCIDO` (id 150).

### Fixture de ubicación validado

**Obligatorio:** lat/lon **dentro** del polígono `place.area` (`ST_Contains` / `findByLocation` 200). Fuera del polígono el wizard no resuelve lugar/NAP correctamente.

| Campo | Valor |
|-------|--------|
| Place | id **1** `9 de octubre` |
| Lat / lon | `-11.2156` / `-77.4107` (preselecciona NAP **NO-001** vía `/napbox/near`) |
| `ST_Contains` | sí (prod y staging, 2026-09-01) |
| Código | `E2ePlaceLocationFixture` |
| UI test | `LocationSelectorComposeDialogTest` pega esas coords |

Cualquier otro lugar con polígono también vale: usar un punto **dentro** del `area` (idealmente las columnas `latitude`/`longitude` de esa fila si ya están validadas) y el mismo `E2E_PLACE`. No usar el centro del envelope.

## Datos del cliente / instalación

| Campo | Valor |
|-------|--------|
| Login | `dscorp` / `nohacker` (staff seed) |
| Nombre / apellido | `EeeFiber` / `Prueba` (cleanup: apellido `Prueba*` o DNI `9xxxxxxx`) |
| DNI | generado `9` + 7 dígitos (`E2E_DNI`) |
| Teléfono | `999888777` |
| Dirección | `Jr EEE Lab ciento veintitres` |
| ONU usuario | `ZTEGDC47BFFD` |
| SN SmartOLT | `5A544547DC47BFFD (ZTEG-DC47BFFD)` |
| Tipo ONU | `F6600RV9.0.21` |
| NAP | `NO-001` (id 104, place 1) |
| WiFi 2.4 | `lab-zte-e2e-24` / `LabZteWifi24!` |
| Host MK | `network_device.id=8`, pool `192.168.250.1/24`, colas `[stg]` |

## UI / debug (solo `BuildConfig.DEBUG`, incluye `stagingDebug`)

En staging **no** se abre cámara ni galería: la **foto de fachada** se inyecta por broadcast. NAP, ONU, plan, WiFi y ubicación se completan **por UI** (Espresso/Compose).

- Ubicación **manual** (GPS abre Ajustes y tumba Compose en emulador). Tags: `location_method_manual`, `map_coordinate_search`, `map_coordinate_search_button`, `map_select_location_button`.
- NAP: la ubicación del fixture preselecciona **NO-001** (`/napbox/near`); si el campo queda vacío, el test abre el dropdown Material. Tag carga: `register_nearby_nap_loading`.
- ONU: dropdown Compose (`register_onu_dropdown`, `register_onu_item_*`) + botón refresh.
- WiFi: tags `tf_wifi_ssid_24`, `tf_wifi_password_24`.
- **Foto fachada (única inyección):** broadcast `com.dscorp.ispadmin.DEBUG_SET_FACADE_PHOTO` extra `path` → JPEG en cache (`facade_e2e.jpg`). Cualquier JPEG válido sirve.

**Importante:** no volver a pulsar «Tipo FIBER» en el paso 2; el ViewModel resetea NAP/ONU/WiFi al cambiar tipo de instalación.

## Cleanup

```bash
./scripts/tr069-e2e-hard-cleanup.sh --env staging --sn ZTEGDC47BFFD --dni "$E2E_DNI"
```

Runbook: `ispadmin-backend/.agent-docs/tr069-e2e-validacion-modelo.md` §4. El wrapper hace ping MK2 **antes** del cleanup.

**Criterio de éxito:** la prueba solo es exitosa con `E2E_FIBER_STAGING_ESPRESSO_OK` (exit 0), que incluye Espresso **y** la verificación de red post-alta (ping MK2). Fuente: `ispadmin-backend/.agent-docs/staging-fiber-e2e-runbook.md` § «Criterio de éxito». Sin ping OK (o si el ping se corta) **no** se da por verde.

## Relacionado

- Seed SQL: `ispadmin-backend/scripts/sql/staging-e2e-registration-catalog.sql`
- Catálogo backend: `ispadmin-backend/.agent-docs/staging-e2e-registration-catalog-2026-09-01.md`
- Wrapper prod (primera ONU): [e2e-fiber-espresso.md](./e2e-fiber-espresso.md)
