# Hotfix — migración wireless→fibra envía VLAN

**Fecha:** 2026-09-02  
**App Distribution:** `2.6.8` (`versionCode` 60)  
**Backend:** `1.0.3+4c1cc1f` (`hotfix/migration-vlan-from-app`)

## Cambio

- `MigrationRequest.vlan`
- Formulario de migración: selector VLAN (default `100`)
- Body de `PUT /subscription/migration` incluye `vlan`

## Nota

El backend hotfix acepta migración sin `vlan` (default `100`), así que APKs anteriores también dejan de fallar tras el deploy.
