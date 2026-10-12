IspAdmin is the operations app of a fiber ISP: technicians and admins open it to see money collected, subscribers gained or lost, and what needs attention today. The system serves fast scanning on a phone. Summary first, detail on demand, and state that you can read without color.

## Content fundamentals

- Write in Spanish (Peru), addressing nobody directly: labels name things, they do not talk. "Ingreso bruto", "Por cobrar", "Cancelaciones", not "Tus ingresos".
- Money is always `S/` with two decimals and es-PE grouping: `S/ 48,320.50`. Counts have no decimals: `1,284 suscriptores`.
- Periods use short month names: `oct 2026`, `Últimos 6 meses`, `vs sep`.
- Everything is sentence case, KPI labels included (`overline` style, no uppercase).
- Errors say what failed and what to do: "No se pudo cargar el dashboard. Revisa tu conexión y vuelve a intentar." No apologies, no emoji.

## Color

- The screen is `background`; content sits on `surface` or `surface-container-low` cards. Text is `on-surface`; secondary text is `on-surface-variant`. Both pass 4.5:1 on every surface in both themes.
- Brand navy is `primary`. Use it for filled buttons, the selected tab indicator, focus rings and the single-series chart.
- `hero` is the navy band right under the app bar, in the app bar's own color so they read as one piece. Its top part (tabs, view selector, refresh) stays fixed; the `kpi-hero` figure scrolls, and the first KPI row overlaps its bottom edge. Text uses `on-hero` / `on-hero-muted`. Use one per screen.
- Brand orange `accent` is a fill, never text on white: white on `#FF5722` is 3.2:1. Put `on-accent` on it. Use `accent-ink` when orange must be text.
- Teal `tertiary` marks collected money (recaudado) next to billed money (`primary`).
- Status colors (`success`, `warning`, `error`, `info`) always come with a glyph or a word: `▲ 4.2%`, `Vencido`. Never reuse them as chart series.

## Charts

- Categorical series take `chart-1` … `chart-5` **in order**, assigned to the entity rather than its rank. A sixth category folds into "Otros" (`chart-other`). The set is validated for color-vision deficiency and contrast against `surface` in both themes.
- A single-series chart (gross revenue, active subscribers) uses `primary`.
- Gridlines and baseline use `chart-grid`; axis labels use `on-surface-variant` in `body-sm`. Text never takes a series color.
- Marks are thin: 2dp lines, bars with `radius-sm` tops anchored to the baseline, a 2dp gap between bars. Use direct labels on at most the last point and the max; a legend whenever there are 2 or more series.
- Use a single y-axis. Two measures of different scale go in two ChartCards.
- Donuts only for part-to-whole with 5 or fewer slices. Otherwise use horizontal bars.

## Type

- One family: Roboto Flex (`sans`), already bundled in the app as `R.font.robotoflex`.
- KPI figures use `kpi-hero` / `kpi` with tabular numerals (`fontFeatureSettings = "tnum"`) so columns of money align.
- Hierarchy on a dashboard: `headline` (screen) → `title-lg` (section) → `title-md` (card) → `body-md` / `body-sm`.

## Space, shape, elevation

- 4dp grid: `space-1` … `space-8`. Screen gutter and card padding are `space-4`; sections are `space-6` apart; KPI grid gap is `space-3`.
- `radius-md` for tiles and cards, `radius-full` for chips and segmented controls, `radius-lg` for the hero band's bottom corners.
- Cards separate by tone (`surface-container-low` on `background`), not shadow. `elevation-1` is only for KPI tiles that overlap the hero edge, and is `none` in dark theme.

## States

- Loading uses `DashboardSkeleton`: blocks in `surface-container-high` shaped like the final layout, with a subtle shimmer. Do not use a centered spinner.
- Use an empty state when a period has no data, for example "Sin pagos registrados en oct 2026."
- On error, show the error card on `error-container` with a "Reintentar" button. Pull-to-refresh is always available.
- The focus ring is 2dp `primary`, 3:1 or better on every surface.

## Iconography

Use Material Symbols Rounded (`androidx.compose.material:material-icons-extended`) at 20–24dp, colored `on-surface-variant` (or the status color inside a badge). Do not use emoji. There is no logo in the sources, so the product name is set in type.

## Compose mapping

- Code lives in `presentation/theme/` (`Color.kt`, `IspColors.kt`, `IspType.kt`, `Dimens.kt`) and `presentation/designsystem/` (`components/`, `charts/`). Read extended colors with `IspTheme.colors`; Material roles with `MaterialTheme.colorScheme`. Charts use Vico through `IspColumnChart` / `IspLineChart`.
- Components map 1:1 to composables in `presentation/designsystem/components/`: `KpiTile`, `ChartCard`, `SectionHeader`, `PeriodSelector`, `StatusBadge`, `DashboardSkeleton`.
