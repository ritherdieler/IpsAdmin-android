# E2E prod homologado a staging — 2026-09-17

`scripts/e2e_register_fiber_espresso.sh` es el mismo flujo que `e2e_register_fiber_staging_espresso.sh`. Solo cambian tokens de entorno.

## Tokens

| Token | Staging | Prod |
|-------|---------|------|
| API | `https://api.gigafiberperu.tech/ispadmin-staging` | `https://api.gigafiberperu.tech/ispadmin` |
| Flavor | `stagingDebug` | `prodDebug` |
| Cleanup | `--env staging` → `127.0.0.1:8081/ispadmin-staging` | `--env prod` → `127.0.0.1:8080/ispadmin` |
| Login / catálogo | textos `Staging` + `staging-e2e-registration-catalog.sql` | textos `Prod` + `prod-e2e-registration-catalog.sql` |
| OK | `E2E_FIBER_STAGING_ESPRESSO_OK` | `E2E_FIBER_PROD_ESPRESSO_OK` |

El resto es idéntico: flags, WiFi por marca, preflight, Espresso, service-health, poll TR-069, cleanup Gateway (sin SmartOLT).

Prueba: `E2ePlaceLocationFixtureTest.prod espresso script matches staging preflight and instrumentation contract` (igualdad tras normalizar tokens).

## WiFi por ONU

| ONU | SSID / pass |
|-----|-------------|
| VSOL / `56534F4C*` / ACS `12345B*` / `B46415*` | `lab-vsol-e2e-24` / `LabVsolWifi24!` |
| Resto (ZTE lab) | `ztelab` / `11111111` |

## Corrida 2026-09-17 (VSOL lab)

| Campo | Valor |
|-------|--------|
| Sub | **2366** (borrada en hard cleanup) |
| DNI | `99663707` |
| SN | ACS `12345B4641531C0B6` ↔ OLT `VSOL0031C0B6` |
| TR-069 | `COMPLETE` (retry 1/12) |
| MK / OLT | `COMPLETE` · PPPoE `gf2366` · host 8 |
| Cleanup | `CLEANUP_DONE` · `remaining_sub=0` |
| OLT delete | Gateway `127.0.0.1:8080/ispadmin` (sin SmartOLT) |

## Cómo correr

```bash
cd "IpsAdmin-android app"
./scripts/e2e_register_fiber_espresso.sh \
  --onu-sn 12345B4641531C0B6 \
  --access-mode pppoe \
  --auto-cleanup
```

Solo ONUs con tag GenieACS `lab`.
