# E2E FIBER — invariante de geo / polígono (2026-09-02)

## Problema que no debe repetirse

Si el Espresso pega (o el emulador fija) una coordenada **fuera** de `place.area`, o **dentro** pero lejos de la NAP de lab, falla el alta: `findByLocation` vacío, NAP incorrecta o `Could not select NAP from dropdown`.

## Fixture canónico (única fuente)

`E2ePlaceLocationFixture` + scripts `e2e_register_fiber_*_espresso.sh`:

| Campo | Valor |
|-------|--------|
| Place | `9 de octubre` (id 1) |
| Lat | `-11.2156` |
| Lon | `-77.4107` |
| NAP lab | `NO-001` (id 104) |
| Formato pegado UI | `-11.2156, -77.4107` (`lat, lon`) |
| `adb emu geo fix` | `-77.4107 -11.2156` (`lon lat`) |

Es el punto de la `nap_box` NO-001: está en el polígono **y** sale primero en `/napbox/near`.

## Checks obligatorios (staging wrapper)

1. `GET …/place/findByLocation?latitude=-11.2156&longitude=-77.4107` → 200 + `9 de octubre`
2. `GET …/napbox/near` mismas coords → lista incluye `NO-001`

Si falla el preflight, **no** lanzar Espresso.

## Anti-patrones

| Qué | Resultado |
|-----|-----------|
| Lat/lon invertidos | Fuera de todo polígono (404) |
| Solo `place.latitude/longitude` (`-11.2177` / `-77.4137`) | Polígono OK; near prioriza otras NAP ≠ NO-001 |
| Centro del envelope | A menudo fuera de `ST_Contains` |
| Default mapa manual (`-11.23416` / `-77.37872`) | Place `la villa` / cerca San Jerónimo |
| Confirmar mapa **antes** de que «Buscar» mueva la cámara | Queda el default → NAP `SJ -*` / polígono equivocado |

Tras pegar el fixture, pulsar **Buscar** y recién entonces **Seleccionar ubicación**. El diálogo prioriza la coordenada buscada sobre el centro de cámara por defecto.

Detalle backend: `ispadmin-backend/.agent-docs/staging-fiber-e2e-runbook.md` § Geo e2e.
