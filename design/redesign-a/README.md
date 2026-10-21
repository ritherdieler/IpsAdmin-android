# Rediseño A — diseño de referencia de la app

Diseño de la Propuesta A implementada en la app (64 pantallas, más las hojas de sistema, datos, mapa de flujos y diagnóstico). Exportado del lienzo https://claude.ai/artifact/1yfKRpqjG9eEXu3jD4XQwX, versión `1792419234-97b7`, el 2026-10-19. Implementación, decisiones y diferencias con el diseño: `.agent-docs/redesign-a-implementacion-2026-10-14.md`.

## Contenido

| Ruta | Qué es | Cómo se usa |
|---|---|---|
| `pantallas/` | Un HTML autónomo por tablero (`a-sus-04-ficha-resumen.html`, …), `index.html` con todos agrupados como en el lienzo, `flujos.html` con los happy paths y `mapa.html` con las líneas de navegación | Se abre en cualquier navegador, sin Claude, y se navega tocando los botones. Es lo que se importa a Figma |
| `fuente/` | Los tableros en formato del lienzo (`*.dc.html`), `canvas.json` (páginas, posición y tamaño de cada tablero) y `assets/` (logo y capturas de la app anterior) | Fuente para editar y para volver a publicar en el lienzo. Sueltos no se ven: dependen del runtime del lienzo (`support.js`), que no está en el repo |
| `fuente/enlaces.json` | Enlaces que el lienzo no trae (o que se corrigen) y los 24 happy paths | Se edita a mano; ver «Navegación y happy paths» |
| `navegacion.js` | Hace clicables los botones y muestra el recorrido de un happy path | Lo copia `exportar.py` a `pantallas/` |
| `mapa.js` | Dibuja el mapa de navegación de `mapa.html` | Lo copia `exportar.py` a `pantallas/` |
| `exportar.py` | Genera `pantallas/` a partir de `fuente/` | `python3 exportar.py` después de cambiar algo en `fuente/` |

Los nombres de personas son ficticios; planes, lugares, cajas NAP, categorías y métodos de pago son los de producción del 14 oct 2026. Las capturas de `fuente/assets/` son de la app anterior y no muestran datos de abonados.

## Ver las pantallas

```bash
python3 -m http.server 8765 --directory pantallas
```

y abrir http://localhost:8765 (pantallas), http://localhost:8765/flujos.html (happy paths) o http://localhost:8765/mapa.html (mapa de navegación). Cada tablero se abre a tamaño real (390 px de ancho las pantallas del teléfono). También se puede abrir un `.html` directamente; el índice usa iframes y algunos navegadores no los cargan desde `file://`.

## Navegación y happy paths

Las pantallas están enlazadas: tocar un botón, una fila o la barra inferior abre la pantalla a la que lleva en la app. Tocar donde no hay enlace resalta en azul lo que sí se puede tocar.

- El lienzo ya trae 199 enlaces entre tableros. `fuente/enlaces.json` agrega los que faltaban (botones de guardar y confirmar, ingreso, barra inferior, filas sin destino) y corrige dos: «Registrar suscripción» desde Hoy e Instalaciones abre el paso 1 · Equipo, no Cliente, porque el registro empieza por el equipo. En total quedan 352 enlaces.
- `flujos.html` muestra los **24 happy paths**: 5 son los flujos clave del mapa del diseño (cobrar a un cortado, instalar desde un ticket, ticket «Sin conexión» de un cortado, atender un ticket y mover de caja NAP) y 19 cubren el resto de tareas (ingresar, cobrar en efectivo, acciones de la ficha, tickets, instalaciones, caja, egresos, planes, cerrar sesión…). «Recorrer» abre el primer paso con un panel que dice qué tocar y lo resalta en naranja; también se avanza con ← y →.
- `mapa.html` es la vista de prototipo: las pantallas de un módulo en su posición del lienzo y una línea azul desde cada botón, fila o ítem hasta la pantalla que abre. Si el destino está en otro módulo, la línea termina en una etiqueta («→ Cobrar con Yape · Caja y egresos») que lleva a ese módulo. Al elegir un happy path muestra solo sus pantallas en orden, con la línea desde el botón que se toca; la línea punteada es un paso automático. Pasar el mouse sobre una pantalla deja solo sus líneas. «Barra inferior» y «Atrás y cerrar» muestran esos enlaces, ocultos por defecto porque se repiten en casi todas las pantallas. Necesita el servidor local: lee los enlaces dentro de cada pantalla.
- Las variantes de estado (cargando, error, vacío, sin conexión, modo oscuro), el ingreso con error y el inicio de Ventas no tienen enlaces de entrada: son el mismo destino en otra situación o con otro rol.

En `enlaces.json`, cada regla es «etiqueta del botón» → pantalla destino. La etiqueta es el `aria-label` del elemento o su texto; con `*` al final se compara solo el inicio, y `"*"` sola es toda la pantalla (el splash). `barra_inferior` da destino a los ítems de la barra que no lo tienen. Cada paso de un flujo indica la pantalla y, en `toca`, el botón que lleva al siguiente. `exportar.py` termina con error y lista qué falla si una regla o un paso ya no encuentra su botón, por ejemplo después de cambiar un texto en el lienzo.

## Pasar a Figma

- **Plugin html.to.design** (sin Claude): en Figma, plugin html.to.design → pestaña *File* → arrastrar uno o varios `.html` de `pantallas/`. Cada archivo trae su CSS y sus imágenes embebidas; las fuentes (Roboto Flex, Roboto Mono, Public Sans) son de Google Fonts, así que Figma las reconoce. Ancho sugerido: 390 para las pantallas `a-*`.
- **Code to canvas de Figma** (desde Claude Code con el MCP de Figma): captura las páginas servidas en el navegador como frames editables.

Lo importado queda como capas editables (textos, colores, cajas), no como componentes ni variables de Figma, y los enlaces no se convierten en conexiones de prototipo: para un prototipo navegable en Figma hay que unir los frames siguiendo `flujos.html`. Si Figma pasa a ser la fuente del diseño, conviene armar los componentes con `A-sistema` como guía y anotarlo en este README. Al importar se abre cada `.html` sin parámetros, así que el panel de recorrido no aparece en Figma.

## Mantenerlo al día

La fuente del diseño es el lienzo. Cuando cambie:

1. Descargar sus tableros a `fuente/` (`project/*.dc.html` y `project/canvas.json` del artifact; las imágenes `/_blob/<id>` van a `fuente/assets/<id>.png`).
2. `python3 exportar.py` y corregir en `fuente/enlaces.json` lo que reporte.
3. Actualizar la versión y la fecha de este README.

Para cambiar el diseño desde el repo se edita el HTML de `fuente/*.dc.html` (el contenido dentro de `<x-dc>`; los estilos van en `<helmet>`), se corre `exportar.py` y, si se quiere ver en el lienzo, se publica de vuelta al artifact.
