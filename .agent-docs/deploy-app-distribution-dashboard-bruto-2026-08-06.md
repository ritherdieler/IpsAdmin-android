# App Distribution — dashboard ingreso bruto — 2026-08-06

## Versión

| Campo | Valor |
|-------|--------|
| versionName | `2.6.3` |
| versionCode | `55` |
| Variant | `prodDebug` (`assembleProdDebug`) |
| Commit feature | `4226187` — pie usa `economicResume.grossRevenue` |
| Commit bump | posterior al script de distribución |

## Cambio funcional

- El total bajo el gráfico circular del dashboard muestra **Total bruto** (`grossRevenue` del API), alineado con backend `1.0.3+eb56b70`.

## Distribución

- Script: `./scripts/firebase-distribute-prod-debug.sh`
- Proyecto Firebase: `ispadmin-687ca`
- Grupo testers: `gigafiber`
- Release notes: `2.6.3 (55): Dashboard: total bruto alineado con backend 1.0.3+eb56b70`

## Git push (misma sesión)

| Repo | Rama | Rango push |
|------|------|------------|
| ispadmin-backend | `develop` | `83934ad..eb56b70` |
| ispadmin-backoffice | `develop` | `aab9c7d..757b43b` |
| IpsAdmin-android | `develop` | incluye `4226187` + bump `2.6.3 (55)` |
