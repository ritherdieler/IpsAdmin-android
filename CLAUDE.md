# CLAUDE.md - Reglas de Ingeniería y Prompt del Sistema Android

## Mandatos Principales y Arquitectura

### 1. Integridad de Código y Seguridad en Producción
- **Protección de Producción:** NUNCA refactorices, modifiques o renombres funciones, módulos o APIs existentes que ya funcionan en producción a menos que se indique explícitamente.
- **Inspección de Contexto:** Analiza siempre los límites existentes en los módulos clave (Subscripciones, Aprovisionamiento, Autenticación, etc.) antes de agregar nuevo código para evitar romper flujos activos.
- **Compatibilidad Hacia Atrás:** Trata los modelos de datos existentes y los contratos de API del backend como inmutables.

### 2. Principios de Ingeniería de Software
- **Principios SOLID:** Aplica estrictamente la Responsabilidad Única, Abierto/Cerrado, Sustitución de Liskov, Segregación de Interfaces e Inversión de Dependencias en todas las capas.
- **Clean Code y Arquitectura:** Separación estricta entre Presentación (Compose + ViewModels), Dominio (UseCases/Entidades) y Datos (Repositories + Data Sources/DTOs).
- **Inmutabilidad y Seguridad:** Prioriza clases de datos inmutables y `StateFlow`. Prohibido el uso de mutabilidad pública, el operador `!!` o efectos secundarios dentro de los Composables.

### 3. Desarrollo Guiado por Pruebas (TDD) y Calidad
- **Flujo TDD:** Escribe primero las pruebas unitarias que fallen para la lógica de negocio, ViewModels y transiciones de estado antes de escribir el código de la feature.
- **Stack de Testing:** Pruebas unitarias con JUnit, MockK / Mockito y Turbine para la validación de `Flow`.
- **Cobertura:** Cubre casos de éxito, errores de red/HTTP, estados vacíos (`Empty`) y fallos de validación.

### 4. Sistema de Diseño y Reglas de UI
- **Tokens de Diseño:** Uso estricto de `MaterialTheme.colorScheme`, `MaterialTheme.typography` y tokens semánticos del proyecto. Prohibido hardcodear colores, dimensiones (dp/sp fuera de tokens) o números mágicos.
- **Espaciado y Retícula:** Escala estricta basada en múltiplos de 4/8dp.
- **Estados de UI:** Cada pantalla o feature debe manejar explícitamente 4 estados: `Loading`, `Success`, `Error` (con opción de reintento) y `Empty`.

---

## Comandos de Compilación, Test y Linter

```bash
# Compilar Proyecto
./gradlew assembleDebug

# Ejecutar Pruebas Unitarias
./gradlew testDebugUnitTest

# Ejecutar una Clase de Prueba Específica
./gradlew testDebugUnitTest --tests "com.package.path.FeatureTest"

# Inspección de Código y Lint
./gradlew lintDebug
```

---

## Herramientas Personalizadas

### run-tests
```yaml
# Custom Tool: run-tests
description: Ejecuta la suite de pruebas unitarias con Gradle para una feature o clase específica y analiza los fallos de TDD.
usage: claude --tool run-tests --args "FeatureNameTest"

commands:
  - execute: ./gradlew testDebugUnitTest --tests "*$1*"
```

### check-regressions
```yaml
# Custom Tool: check-regressions
description: Revisa el diff de Git frente a la rama principal (main/develop) para validar que no se hayan modificado funciones legacy o módulos clave de producción.
usage: claude --tool check-regressions

commands:
  - execute: git diff --stat origin/main -- app/src/main/java/com/tuapp/core/
```

### code-check
```yaml
# Custom Tool: code-check
description: Ejecuta la compilación de prueba y el linter del proyecto para garantizar que no haya errores de compilación ni violaciones de código.
usage: claude --tool code-check

commands:
  - execute: ./gradlew lintDebug assembleDebug --daemon
```

### sync-api-contracts
```yaml
# Custom Tool: sync-api-contracts
description: Extrae o verifica los contratos DTO/JSON del backend para garantizar la coherencia de los modelos en la capa Data de la app.
usage: claude --tool sync-api-contracts --args "path/to/backend/docs/or/swagger.json"
```
