# Selector de fechas — Subscription Finder

Fecha: 2026-09-21

## Problema UTC

Material3 `DatePicker` entrega `selectedDateMillis` como medianoche **UTC** del día civil elegido. Formatear ese millis con la zona local (p. ej. Perú UTC−5) corría la fecha un día atrás.

## Corrección

`formatDatePickerUtcMillis` interpreta el millis en `ZoneOffset.UTC` y formatea `dd/MM/yyyy` (el mismo contrato que espera `GET` de búsqueda por fecha en el backend).

`todayDdMmYyyy` usa el calendario local del dispositivo para el botón **Hoy**.

## Controles reformulados

- Campos **Desde** / **Hasta**
- Botón **Hoy**: rellena ambos con el día local y dispara la búsqueda
- Botón **Buscar**: requiere ambas fechas
- En el DatePicker: acción **Hoy** para rellenar solo el campo abierto
