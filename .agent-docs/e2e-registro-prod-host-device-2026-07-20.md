# E2E registro suscripción prod (host device CCR 2) — 2026-07-20

## Resultado
- Validación de host único en UI: con un solo CLOUD_CORE_ROUTER activo (Mikrotik CCR 2, id=8) el selector `register_host_device_dropdown` no se muestra.
- ONU piloto `ZTEGDC47C169` visible en autofind/unconfigured y seleccionable en UI.
- Registro exitoso (API prod, mismo contrato que la app): subscription id **2252**, `host_device_id=8`, IP **192.168.30.10**.
- Cleanup completado: suscripción eliminada de MySQL, queue MikroTik removida, ONU desautorizada en OLT (vuelve a estado no configurado / autofind).

## Limitación UI automation
En SM-S938B vía adb wireless, taps en la mitad inferior del formulario (dirección/plan/NAP) enfocaban campos incorrectos; el submit E2E completo por android-cli no fue fiable. Login, navegación, ausencia de host selector y selección de ONU sí se validaron en dispositivo.

## Reversión
- MySQL: `DELETE subscription` id=2252 (+ logs).
- MikroTik CCR 2: `/queue/simple/remove` target `192.168.30.10/32`.
- OLT: `DELETE /onu?onuExternalId=ZTEGDC47C169` → ONU vuelve a unconfigured.
