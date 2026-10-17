StatusBadge labels a state (paid, pending, overdue, migrated) with color and a word, never color alone.

- **Consumer provides:** `text`, `tone` (`Success`, `Warning`, `Error`, `Info`, `Neutral`), optional leading icon.
- `radius-sm` pill, 4dp × 8dp padding, `label-md`. Fill is the `*-container` token; text and icon are the matching status token.
- Keep it to one or two words: "Pagado", "Por vencer", "Vencido", "Migración".
