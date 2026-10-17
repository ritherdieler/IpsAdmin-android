# Staging lab: solo VSOL + alta ZTE Sergio Carrillo Diestra (2026-09-07)

## Objetivo

1. Dejar en staging **solo** el lab VSOL (`#2389` / `VSOL0031C0B6`, `subscription_acs.lab=1`).
2. Registrar un **nuevo** lab ZTE (`ZTEGDC47BFFD`) con nombre **Sergio Carrillo Diestra**.
3. Conservar la suscripción (`SKIP_POST_CLEANUP=1`) y marcar `subscription_acs.lab=1` tras COMPLETE.

## Cleanup previo

| id | Acción |
|----|--------|
| 2391 | Hard cleanup (`ZTEGDC47BFFD`, EEEFIBER PRUEBA) |
| 2387 | Hard cleanup (huérfana sin SN) |
| 2389 | Conservada (EeeFiber VsolLab) |

## E2E

```bash
SKIP_POST_CLEANUP=1 \
E2E_ONU_SN=ZTEGDC47BFFD \
E2E_FIRST_NAME=Sergio \
E2E_LAST_NAME='Carrillo Diestra' \
./scripts/e2e_register_fiber_staging_espresso.sh
```

Args nuevos en script / `FiberRegisterFirstOnuE2ETest`: `e2e.firstName`, `e2e.lastName` (defaults `EeeFiber` / `Prueba`).

WiFi default ZTE: `lab-zte-e2e-24` / `LabZteWifi24!` (5G: `lab-zte-e2e-24 - 5G`).

## Resultado

| Campo | Valor |
|-------|--------|
| id | **2392** |
| Nombre | SERGIO CARRILLO DIESTRA |
| SN | ZTEGDC47BFFD |
| IP | 192.168.250.22 (MK2 ping OK) |
| DNI | 98821070 |
| WiFi 2.4 | `lab-zte-e2e-24` / `LabZteWifi24!` |
| WiFi 5 | `lab-zte-e2e-24 - 5G` / `LabZteWifi24!` |
| `subscription_acs.lab` | **1** (device `5872C9-F6600R-ZTEGDC47BFFD`) |

Labs staging: `#2389` VSOL + `#2392` ZTE.
