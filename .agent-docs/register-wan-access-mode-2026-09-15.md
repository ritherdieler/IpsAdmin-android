# Alta Android: tipo de WAN PPPoE / IP estática

El formulario de registro de suscripción permite elegir el tipo de WAN. El default es PPPoE (`PPPOE_DYNAMIC`). La opción se envía en `POST /subscription` como `accessMode`.

## Contrato

| UI | `accessMode` |
|----|----------------|
| PPPoE | `PPPOE_DYNAMIC` |
| IP estática | `STATIC_IP` |
| (e2e explícito) | `PPPOE_FIXED` si se pasa |

El 360/directorio ya es XOR por `accessMode` en backend.

## E2E

Los scripts `e2e_register_fiber_*_espresso.sh` aceptan:

```bash
--access-mode pppoe
--access-mode static
E2E_ACCESS_MODE=pppoe
E2E_ACCESS_MODE=static
```

Alias: `PPPOE_DYNAMIC`, `STATIC_IP`, `PPPOE_FIXED`.

Si no se pasa nada, el e2e y el formulario usan PPPoE. Ya no se fuerza `STATIC_IP` por ONU VSOL.

Ejemplo staging IP estática:

```bash
./scripts/e2e_register_fiber_staging_espresso.sh --access-mode static
```
