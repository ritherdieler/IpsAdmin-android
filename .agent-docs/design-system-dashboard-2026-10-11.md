# Design system y dashboard rediseñado — 2026-10-11

Rama `feature/dashboard-redesign`. El commit `12abf06` es una instantánea del trabajo pendiente anterior (dashboard financiero del 2026-10-09 y otros cambios). El rediseño va encima.

## Fuentes de diseño

- Design system (tokens, componentes, reglas de uso): https://claude.ai/artifact/T4ssmVpou5P4pvcFkjS6sF
- Mockups aprobados (solo variante A, claro y oscuro): https://claude.ai/artifact/1d9B2qXwQBC2hBobes6Yu4
- Copia en el repo (design system, mockups y capturas de la implementación): `design/dashboard-redesign/`

## Código

| Ruta (`presentation/src/main/java/com/dscorp/ispadmin/`) | Qué es |
|---|---|
| `presentation/theme/Color.kt`, `Theme.kt` | `MyTheme`. Los roles de marca globales no cambian. Neutros, error y tema oscuro alineados al design system |
| `presentation/theme/IspColors.kt` | Colores sin rol Material: hero, estados, paleta de gráficos. Se leen con `IspTheme.colors` |
| `presentation/theme/IspType.kt`, `Dimens.kt` | Estilos numéricos (cifras tabulares), espaciado (`IspSpacing`) y formas (`IspShapes`) |
| `presentation/theme/Type.kt` | Corrige un bug: antes `FontStyle(R.font.robotoflex)` no cargaba la fuente. Ahora `RobotoFlex` es un `FontFamily` variable |
| `presentation/designsystem/components/` | `KpiTile` (con ⓘ opcional), `ChartCard`, `IspCard`, `SectionHeader`, `StatusBadge`, `HeroTop`/`HeroBand`/`HeroTabs`/`HeroChips`/`HeroFigure`, `HorizontalBarList`, `ProportionBar`, `ProgressTrack`, `DashboardSkeleton` |
| `presentation/designsystem/charts/IspCharts.kt` | `IspColumnChart` / `IspLineChart` sobre Vico 2.1.2. Las barras parten de 0; las líneas ajustan el eje al rango de los datos |
| `presentation/ui/features/dashboard/DashBoardComposeScreen.kt` | Contenedor con 3 pestañas: Financiero, Comercial y Clientes |
| `presentation/ui/features/financial/FinancialScreen.kt` | Pestaña Financiero. Lógica, `testTag` y textos intactos (los tests existentes pasan) |
| `presentation/ui/features/dashboard/CommercialDashboard.kt` | Mapeo puro de `DashBoardDataResponse`: tendencias de 6 meses, churn y ARPU |
| `presentation/ui/features/dashboard/CommercialScreen.kt` | Pestaña Comercial. Sigue usando `DashBoardViewModel` legado |
| `presentation/ui/features/dashboard/clients/` | Pestaña Clientes: `ClientsViewModel` (UDF) y `ClientsScreen` |
| `data/datasource/remote/dashboard/`, `data/repository/ClientsDashboardRepositoryImpl.kt`, `domain/model/dashboard/`, `domain/repository/ClientsDashboardRepository.kt` | API acotada a los sub-endpoints del Core `/dashboard/{plan-analysis,assistance-tickets,client-quality,geographic-performance,client-lifecycle}`. Cada sección carga por separado; si una falla, queda en `null` |

Se eliminaron `DashboardScreen.kt`, `components/ChartComponents.kt`, `graphics/*` y la dependencia MPAndroidChart. Se agregó `vicoVersion = '2.1.2'` en `versions.gradle`.

## Decisiones

- **Banda superior:** usa el mismo color que la barra superior compartida de la app (`primaryContainer`) para leerse como una sola pieza.
  - Su parte superior (pestañas, selector de vista, actualizar) queda fija; la cifra principal se desplaza con la lista.
  - Los KPIs se montan sobre su borde (`Modifier.pullUp`).
- **Rótulos:** en minúsculas normales, sin mayúsculas. Son largos y los tests los buscan textualmente.
- **Pagos por método:** el gráfico legado "Pagos electrónicos por mes" en realidad mostraba el % de pagos por método (`paymentResume`). Se renombró a "Pagos por método".
- **Churn:** cancelaciones del mes ÷ activos al cierre del mes anterior.
- **ARPU:** recaudado del mes (`monthlyCollects.totalRaised`) ÷ activos del mes.
- **Prioridad de tickets:** el Core guarda un entero. Confirmado por el usuario: 0 = baja, 1 = media, 2 = alta. La banda muestra "N de prioridad alta".
- **No se muestra:** `networkHealthResume`, porque sus datos son simulados en el Core.

## Cobranza operativa, deuda por moroso y ventana de tickets (backend con TDD)

- **Core:** `GET /dashboard/collections` y `months` en `/dashboard/assistance-tickets`. Detalle en `ispadmin-backend/.agent-docs/dashboard-cobranza-operativa-2026-10-11.md`.
- **Android:**
  - `CollectionsViewModel` (UDF) se carga una vez en `DashBoardComposeScreen` y llega a las pestañas como `DashboardTabData`.
  - Financiero muestra `CollectionsSection` en la vista "Cobros y egresos".
  - La tarjeta "Deuda por moroso" de Clientes usa `overduePerDelinquent`.
  - Clientes pide tickets con `months = TICKET_HISTORY_MONTHS` (6).
- **Tests nuevos:** `DashboardRepositoriesTest`, `CollectionsViewModelTest` y `CollectionsUiTest`, escritos primero en Red.
- **Con un Core anterior:** la sección no aparece y la deuda dice "No disponible".

## Auditoría en emulador con datos dev (correcciones)

| Hallazgo | Corrección |
|---|---|
| `/dashboard` envía `monthlyCollects.totalRaised` como **% recaudado**, no en soles | Recaudado = `grossIncome × totalRaised ÷ 100` (`collectedSoles`); ARPU = recaudado ÷ no canceladas del mes |
| `paymentResume` es el **% de pagos digitales por mes** (claves "May.", "Set."), no un desglose por método | Gráfico "Pagos electrónicos por mes", en orden cronológico (`digitalPayments`) |
| Comercial mezclaba el snapshot mensual con el log de eventos (las tarjetas no cuadraban con los gráficos) | Altas, cancelaciones, reconexiones y migraciones salen solo de `subscriptionLogSummary`, con ventana que termina en el mes actual. El snapshot (`countNonCancelledSubscriptions` = activas + cortadas) solo alimenta "Suscripciones no canceladas" y la base del churn y del ARPU. La banda muestra los ACTIVE sin variación |
| Prioridades (ventana de 6 meses) mostradas como si fueran de los tickets abiertos | Tarjeta propia "Por prioridad", sin insignia en la banda |
| Morosos > activos (incluye CUT_OFF) | El texto dice "suscripciones morosas (incluye cortadas)" |
| Más de 20 planes y precios absurdos en la fuente | Top 4 + "Otros" (ARPU ponderado); importes ≥ 100k compactos (`formatSolesCompact`) |
| Variación que redondea a 0 mostrada como ▲ | Se redondea a la precisión mostrada antes de elegir la flecha |
| Chip seleccionado invisible en oscuro | `selectedContainer` oscuro = `#B9C3FF` / `#0E1640` |
| Primario oscuro `#3A4998` ilegible en las superficies oscuras de toda la app | `DarkPrimary = #B9C3FF`, `DarkOnPrimary = #0E1640` |
| Al cambiar el tema, la app volvía a la primera pantalla del menú | `MainActivity` declara `android:configChanges="uiMode"`. Un intento de condicionar el NavHost a la carga del usuario no lo resolvió y se revirtió. La rotación sigue recreando la Activity (no se tocó) |

Datos de origen a revisar: el plan "Internet70" tiene un precio de S/ 89,699,210.91 en `ispadmin_dev`, y `monthly_subscription_resume` termina en agosto.

## Verificación

El rediseño visual se hizo sin TDD, por decisión del usuario. La cobranza operativa, la deuda por moroso y la ventana de tickets se hicieron con TDD.

```bash
./gradlew :presentation:compileDevDebugKotlin
./gradlew :presentation:testDevDebugUnitTest   # 432 tests, 0 fallos
./gradlew :presentation:lintDevDebug           # sin errores; solo avisos preexistentes de recursos sin uso
```

- Revisión visual: capturas de las 3 pestañas en claro y oscuro con Robolectric (`GraphicsMode.NATIVE`), mediante un test temporal no commiteado.
- No se instaló en dispositivo ni se probó contra el Core real. En particular, falta validar contra datos reales los sub-endpoints de Clientes y el formato real de `subscriptionLogSummary.period`.
