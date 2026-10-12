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
