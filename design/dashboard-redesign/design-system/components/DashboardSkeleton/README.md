DashboardSkeleton is the loading state of the dashboard: grey blocks shaped like the hero, KPI grid and first chart card.

- **Consumer provides:** nothing; it fills the screen.
- Blocks in `surface-container-high` with `radius-sm` lines and `radius-md` cards, plus a slow shimmer (1.2s). The shimmer is disabled when the system "remove animations" setting is on.
- Replace with content in one step. Don't reveal sections one by one.
