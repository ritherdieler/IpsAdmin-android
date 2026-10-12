PeriodSelector picks the month or range every chart on the screen reads from.

- **Consumer provides:** `options` (e.g. "Este mes", "Mes anterior", "6 meses", "12 meses"), `selected`, `onSelect`.
- Horizontal scrollable row of `radius-full` chips with an `space-2` gap. Selected: `primary-container` / `on-primary-container` with a check icon. Unselected: `surface-container` / `on-surface-variant`.
- On the hero band, use the on-hero variant: unselected chips are transparent with a 1dp `on-hero-muted` border, and the selected chip is `surface` / `on-surface`.
- Changing period does not repaint series colors.
