# Rediseño del dashboard — diseño de referencia

Diseño aprobado el 2026-10-11 (variante A, tema claro y oscuro). Implementación y decisiones: `.agent-docs/design-system-dashboard-2026-10-11.md`.

## Contenido

| Carpeta | Qué es | Cómo verlo |
|---|---|---|
| `design-system/` | Design system IspAdmin: `README.md` (reglas de uso), `tokens.json` (colores claro/oscuro, tipografía, espaciado, radios, sombra) y `components/<Componente>/` (guía + vista previa HTML) | Interactivo en https://claude.ai/artifact/T4ssmVpou5P4pvcFkjS6sF. Las `preview.html` usan variables CSS que genera ese artifact; abiertas sueltas no tienen estilos |
| `mockups/` | Tableros del lienzo: Financiero (`Main`), Comercial y Clientes, en claro (`*A` / `Main`) y oscuro (`*Oscuro`). `canvas.json` guarda su disposición | Interactivo en https://claude.ai/artifact/1d9B2qXwQBC2hBobes6Yu4. Los `.dc.html` dependen del runtime del lienzo (`support.js`), que no está en el repo |
| `capturas/` | La app implementada en el emulador con la base de desarrollo, después de la auditoría visual | PNG |

Los mockups usan datos de ejemplo; las capturas usan datos de desarrollo, no de producción.

## Diferencias entre el mockup y la implementación

- La banda superior usa el color de la barra de la app (`#1E2A6D`), y su parte de arriba (pestañas, selector y actualizar) queda fija.
- Los rótulos de los KPIs van en minúsculas normales, no en mayúsculas.
- "Pagos electrónicos por mes" muestra el % de pagos digitales por mes, como envía el Core.
- "Promesas de pago" se reemplazó por prórrogas de corte, porque la tabla `payment_commitment` se eliminó en V80.
- Las prioridades de tickets (0 baja, 1 media, 2 alta) van en una tarjeta propia, porque cubren 6 meses de historial y no solo los tickets abiertos.
- La barra "Panel" de la app sigue fuera de la banda; el mockup pone "Dashboard / Actualizado hace 3 min" con exportar y actualizar dentro de ella.
- El hero de Financiero conserva el aviso de cobros sin fecha y la línea del corte (las pruebas existentes los exigen visibles); la referencia del corte se ve en el diálogo de registros.
- Aún sin datos para replicar el mockup: variación del hero Financiero contra el mes anterior (requiere el corte previo), chips Este mes / 6 / 12 meses en Comercial e historial de suscriptores *ACTIVE* (el snapshot mensual guarda activas + cortadas).

## Segunda auditoría con datos de producción (2026-10-12)

Revisión en el emulador con la copia de producción en staging. Cambios hechos con TDD:

- Encabezados de sección a 18 sp seminegrita; montos de las barras en una línea.
- Ejes con pasos redondos (`niceScale`); los conteos nunca muestran decimales. "Altas vs bajas" es divergente (bajas bajo cero).
- Financiero: 4 KPIs (Recuperación pasó al desglose por mes de servicio), sección Tendencia (neto vs cobrado, 6 meses de servicio), resumen del último mes en Historial, mes en curso fuera de Ingresos brutos (la facturación corre a mitad de mes), Cobros por método en dona con %.
- Comercial: altas netas del mes en el hero, valor del mes en las tarjetas de evolución, pagos digitales limitados a 6 meses, promedio en el churn y "Dato de sep" cuando el ARPU no es del mes en curso.
- Clientes: "! N de prioridad alta" en el hero (tickets abiertos ahora), top 4 + Otros en categorías y cancelaciones (Otros siempre al final; la categoría "Otros" del Core se suma ahí), top 5 de zonas e ingresos compactos desde S/ 10k.
- Backend (`ispadmin-backend`): resolución con `closed_at`, antigüedad desde la instalación, ventana de 6 meses en zonas y ciclo de vida, `openTicketsByPriority`, y ARPU por plan e ingreso por zona con la mensualidad del plan (`subscription.price` es el cobro de instalación).
