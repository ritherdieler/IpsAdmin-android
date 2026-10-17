# App Distribution — registro offline de suscripciones — 2026-08-15

## Versión

| Campo | Valor |
|-------|--------|
| versionName | `2.6.4` |
| versionCode | `56` |
| Variant | `prodDebug` (`assembleProdDebug`) |
| Backend alineado | `1.0.3+bce3bc2` |

## Cambio funcional

- Cola y sync de suscripciones offline con `clientRequestId`
- Mapeo de `IP_CONFLICT` en pendientes
- Éxito parcial si `provisioningPending`

## Distribución

- Script: `./scripts/firebase-distribute-prod-debug.sh`
- Proyecto Firebase: `ispadmin-687ca`
- Grupo testers: `gigafiber`
- Release notes: `2.6.4 (56): Registro y sync offline: clientRequestId, IP_CONFLICT y provisión pendiente`
- Consola: https://console.firebase.google.com/project/ispadmin-687ca/appdistribution/app/android:com.dscorp.ispadmin/releases/75c63ir4bqf2g
