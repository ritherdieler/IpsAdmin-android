# Observabilidad — Session Replay sin enmascarado (Android)

**Fecha**: 2026-07-12
**Módulo**: `:presentation` — paquete `com.dscorp.ispadmin.observability`

Se eliminó por completo el enmascarado de PII del session replay por fotogramas.
Los replays ahora capturan y suben el contenido de la pantalla **tal cual**, sin
ocultar ningún campo con rectángulos negros.

## Motivación

Requerimiento explícito: no se debe enmascarar absolutamente nada en los replays.
Anteriormente ciertos campos de texto (Login, Registro de suscripción, Registro de
usuario) se marcaban con `.obsMask()` y el grabador pintaba rectángulos negros
opacos sobre esas regiones antes de comprimir cada frame, por lo que aparecían
negros en el reproductor.

## Cambios

### Eliminado
- `observability/ObservabilityMask.kt` (registro de regiones, `Modifier.obsMask()`,
  `View.obsMask()`, `collectRects`). Archivo borrado.
- En `observability/ObservabilityScreenRecorder.kt`:
  - `maskPaint` (`Paint` negro) y la función `drawMasks(...)`.
  - La copia intermedia del bitmap para pintar máscaras: `processBitmap` ahora
    hace el downscale y la compresión WebP directamente desde el bitmap de
    `PixelCopy`, sin paso de enmascarado. Firma simplificada a
    `processBitmap(source: Bitmap)`.
  - Imports ya no usados: `android.graphics.Canvas`, `android.graphics.Color`,
    `android.graphics.Paint`, `android.view.View`, `android.view.Window`.

### Usos de `.obsMask()` retirados
- `login/Login.kt` — usuario y contraseña (2).
- `subscription/register/compose/RegisterSubscriptionForm.kt` — DNI, teléfono,
  dirección y la fila nombres/apellidos (5).
- `registration/RegisterScreen.kt` — nombres, apellidos, DNI, email, teléfono,
  usuario, contraseña y confirmación (8).
- Se eliminaron también los `import com.dscorp.ispadmin.observability.obsMask`.

## Efecto

- Los fotogramas del replay muestran el contenido completo de la UI, incluida
  cualquier PII visible en pantalla (DNI, teléfono, dirección, credenciales, etc.).
- No cambia el resto del pipeline (buffer en anillo, WebP, gzip, enlace por
  `replayId`, topes de `ObservabilityReplayConfig`).

## Validación

- `./gradlew :presentation:compileDevDebugKotlin` → `BUILD SUCCESSFUL`.

## Docs relacionadas actualizadas

- `observabilidad-session-replay-frames-android.md` (marcado sin enmascarado).
