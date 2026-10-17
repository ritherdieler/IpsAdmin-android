KpiTile shows one business figure with its label, change against the previous period and an optional sparkline.

- **Consumer provides:** `label` (uppercase overline text), `value` (already formatted, e.g. `S/ 12,450.00`), optional `delta` (`+4.2%` and direction), optional `trend: List<Float>` for the sparkline, optional `onClick`.
- **Layout:** `surface` fill, `radius-md`, `space-4` padding. Label in `overline` / `on-surface-variant`, value in `kpi` / `on-surface`, delta in `kpi-delta`.
- **Delta:** ▲ in `success` for good direction, ▼ in `error` for bad. "Good" depends on the metric: fewer cancellations is ▼ in `success`. Always include the comparison period ("vs sep").
- **Sparkline:** 2dp line in `primary` with a faint area fill, emphasized endpoint dot. Omit axes.
- Use in a 2-column grid with `space-3` gaps. Don't put more than 6 tiles above the fold.
