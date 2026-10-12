ChartCard frames one chart with a title, a subtitle naming the period and unit, an optional legend and an optional action.

- **Consumer provides:** `title`, `subtitle` (period + unit, e.g. "Últimos 6 meses · S/"), the chart content slot, optional `legend: List<LegendItem>` and `action` (e.g. "Ver detalle" in `accent-ink`).
- **Layout:** `surface-container-low` in light, `surface-container-low` in dark; `radius-md`; `space-4` padding; chart height 180–220dp.
- **Charts:** Vico `CartesianChartHost` themed from `chart-*`, `chart-grid` and `on-surface-variant`. One y-axis per card.
- A legend row sits under the title when there are 2 or more series: an 8dp swatch plus the label in `body-sm` / `on-surface-variant`.
- Empty data shows a one-line empty state inside the card, not a blank plot.
