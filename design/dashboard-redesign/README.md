# Rediseño del dashboard — diseño de referencia

Diseño aprobado el 2026-10-11 (variante A, tema claro y oscuro). Implementación y decisiones: `.agent-docs/design-system-dashboard-2026-10-11.md`.

## Contenido

| Carpeta | Qué es | Cómo verlo |
|---|---|---|
| `design-system/` | Solo un README que apunta al design system único (web + Android) | https://claude.ai/artifact/T4ssmVpou5P4pvcFkjS6sF |
| `mockups/` | Tableros del lienzo: Financiero (`Main`), Comercial y Clientes, en claro (`*A` / `Main`) y oscuro (`*Oscuro`). `canvas.json` guarda su disposición | Interactivo en https://claude.ai/artifact/1d9B2qXwQBC2hBobes6Yu4. Los `.dc.html` dependen del runtime del lienzo (`support.js`), que no está en el repo |
| `capturas/` | La app implementada en el emulador con la base de desarrollo, después de la auditoría visual | PNG |

Los mockups usan datos de ejemplo; las capturas usan datos de desarrollo, no de producción.

## Diferencias entre el mockup y la implementación

- "Pagos electrónicos por mes" muestra el % de pagos digitales por mes, como envía el Core.
- "Promesas de pago" se reemplazó por prórrogas de corte, porque la tabla `payment_commitment` se eliminó en V80.
- Las prioridades de tickets (0 baja, 1 media, 2 alta) van en una tarjeta propia, porque cubren 6 meses de historial y no solo los tickets abiertos.
- Financiero: la línea de estado de la cabecera muestra el corte ("Corte: … · revisión N") en vez de "Actualizado hace X min"; el hero conserva la etiqueta Observado/Completo y el aviso de cobros sin fecha. El cuarto KPI es "Diferencias del servicio" en lugar de "Efectividad" (la efectividad está en Facturación y cartera).
- Datos que se guardan desde el 2026-10-12 (V88), así que aparecen con el tiempo: "⇄ Plan superior" solo cuenta migraciones hechas desde entonces, y el gráfico "Suscriptores activos fin de mes" reemplaza a "Suscripciones no canceladas" cuando haya dos cierres mensuales con el dato.

## Tercera auditoría visual (2026-10-12, sin TDD)

Contraste de capturas del emulador con los mockups renderizados, en claro y oscuro:

- El Dashboard oculta la barra compartida "Panel" y dibuja la fila del mockup dentro de la banda: ☰, "Dashboard", línea de estado y acciones (exportar en Financiero, actualizar). El ☰ abre el menú lateral (`main_nav_open_drawer`).
- Banda `#141d48` (oscuro `#1b2459`), igual a la barra de estado.
- Rótulos de figuras en mayúsculas espaciadas (11 sp, 0.8 sp) con `Overline`; los lectores de pantalla oyen el texto normal.
- Hero 36 sp; cambio del hero 13 sp; pestañas y chips 12 sp seminegrita; títulos de tarjeta 15 sp; valores de KPI 22 sp peso 650.
- Tarjetas KPI con la altura del contenido y filas de igual altura, como la cuadrícula del mockup.
- Selector "MES DE MOVIMIENTOS · ‹ oct 2026 ›".
- Enlaces de sección: "Ver deudores" (abre la cartera vencida), "Ver todo" (buscador de suscripciones), "Ver tickets" (tickets de soporte).
- Cobranza operativa: "Al 12 oct 2026", filas de 13 sp y valores de 18 sp.
- Sin los textos de apoyo que el mockup no tiene bajo el hero de Comercial y Clientes.

## Segunda auditoría con datos de producción (2026-10-12)

Revisión en el emulador con la copia de producción en staging. Cambios hechos con TDD:

- Encabezados de sección a 18 sp seminegrita; montos de las barras en una línea.
- Ejes con pasos redondos (`niceScale`); los conteos nunca muestran decimales. "Altas vs bajas" es divergente (bajas bajo cero).
- Financiero: 4 KPIs (Recuperación pasó al desglose por mes de servicio), sección Tendencia (neto vs cobrado, 6 meses de servicio), resumen del último mes en Historial, mes en curso fuera de Ingresos brutos (la facturación corre a mitad de mes), Cobros por método en dona con %.
- Comercial: altas netas del mes en el hero, valor del mes en las tarjetas de evolución, pagos digitales limitados a 6 meses, promedio en el churn y "Dato de sep" cuando el ARPU no es del mes en curso.
- Clientes: "! N de prioridad alta" en el hero (tickets abiertos ahora), top 4 + Otros en categorías y cancelaciones (Otros siempre al final; la categoría "Otros" del Core se suma ahí), top 5 de zonas e ingresos compactos desde S/ 10k.
- Backend (`ispadmin-backend`): resolución con `closed_at`, antigüedad desde la instalación, ventana de 6 meses en zonas y ciclo de vida, `openTicketsByPriority`, y ARPU por plan e ingreso por zona con la mensualidad del plan (`subscription.price` es el cobro de instalación).

## Comparaciones y periodos (2026-10-12, con TDD)

- Financiero: el hero compara lo cobrado con el mes anterior; con el mes en curso compara hasta el mismo día ("vs sep al día 11"), usando las fechas de los cobros del corte anterior.
- Clientes: tickets abiertos "vs semana pasada" (abiertos hace 7 días, reconstruidos con fechas de creación y cierre); resolución media y reapertura muestran el valor del mes y su cambio contra el mes anterior.
- Comercial: chips Este mes / 6 meses / 12 meses. Los KPIs suman el periodo (churn y ARPU lo promedian) y se comparan con el mismo número de meses previos (12 meses sin comparación: haría falta 24); los gráficos muestran 6 o 12 meses.
- Migraciones: "⇄ Plan superior: N" con las migraciones del mes a un plan más caro.

## Valor al tocar los gráficos (2026-10-12, sin TDD)

Todos los gráficos mensuales (`IspColumnChart`, `IspLineChart`) muestran al tocar un mes una línea guía y una etiqueta con el mes y el valor exacto de cada serie: soles con decimales en Financiero, % en churn y pagos electrónicos, conteos en el resto. La etiqueta queda visible al soltar y cambia al tocar otro mes (marcador persistente de Vico con `CartesianMarkerVisibilityListener`).

## Puntos y promedio en los gráficos (2026-10-12, sin TDD)

- Líneas: un punto por mes con anillo del color de la tarjeta; el último mes, más grande.
- Métricas de flujo mensual de una sola serie (ingresos, recaudación, churn, pagos electrónicos, cancelaciones, reconexiones, migraciones): línea punteada del promedio del período mostrado y "prom. X" en el subtítulo de la tarjeta (no sobre el gráfico, para no tapar barras ni la línea). Totales acumulados (suscriptores) y gráficos de varias series no llevan promedio.
