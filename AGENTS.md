# IpsAdmin Android — instrucciones para agentes

**Plataforma:** aplican también las reglas de `gigafiber/AGENTS.md` (TDD, secretos, español).

**Scope:** solo el repo Android `IpsAdmin`. Si el archivo o la tarea es `wispadministrator-main` (Spring), **ignora estas instrucciones**.

Workflows opcionales: `.cursor/skills/*/SKILL.md` cuando el flujo lo requiera; los skills son workflows y no sustituyen estas reglas.

Las reglas con **Ámbito** aplican al editar archivos que coincidan con esos patrones. Si hay conflicto, gana la regla más específica para el tipo de archivo.

## Project profile

Fuente de verdad auditada. No inventar stack ni arquitectura.

### Identidad

| Campo | Valor |
|---|---|
| App | IspAdmin (`app_name`) |
| Gradle root | `ispAdminAndroid` |
| applicationId / namespace | `com.dscorp.ispadmin` |
| Módulos | `:presentation` (app), `:domain` (JVM), `:data` (Android lib), `:observability` (Android lib) |
| minSdk / targetSdk / compileSdk | 26 / 34 / 36 |
| Kotlin plugin / stdlib | 2.1.0 / 2.1.21 (hay desfase; no “arreglarlo” en una feature) |
| AGP / Gradle / JVM | 8.13.2 / 8.13 / Java 17 |
| DSL | Groovy (`.gradle`, no `.kts`, no version catalog TOML) |
| Flavors | `dev`, `staging`, `prod` (dimension `environment`) |
| versionCode / versionName | ver `presentation/build.gradle` |

`dev` → `applicationIdSuffix ".dev"`, `BASE_URL = http://127.0.0.1:8080/ispadmin/`  
`staging` → `BASE_URL = https://api.gigafiberperu.cloud/ispadmin-staging/` (mismo `applicationId` que prod)  
`prod` → `BASE_URL = https://api.gigafiberperu.cloud/ispadmin/`

### Arquitectura REAL

Híbrido en migración, no Clean Architecture completa.

```text
:presentation  →  :domain, :data, :observability
:data          →  :domain
:domain        →  Kotlin + coroutines + Koin core (sin Android)
```

- Dominio **nuevo** (offline sync, catálogo): puertos en `:domain`, impl en `:data`.
- Dominio **legado**: `presentation/src/main/java/com/dscorp/ispadmin/domain/` (~75 archivos).
- Data **legado**: `IRepository` + `Repository.kt` (~1124 líneas) en `:presentation`.
- UI **nueva**: Jetpack Compose + Navigation Compose + Material 3 + `MyTheme`.
- UI **legado**: ~33 XML layouts, ~11 Fragments, ViewBinding/DataBinding, algo de LiveData.

No migrar XML→Compose ni romper `IRepository` salvo tarea explícita.

### Patrones de estado

**Canónico (código nuevo):** UDF / MVI ligero.

- `UiState` (`data class`) + `StateFlow`
- `Intent` / `onIntent`
- `UiEvent` via `SharedFlow` (one-shot)
- Dispatcher inyectable (`mainImmediate`) para tests
- Ejemplo: `presentation/.../subscription/pending/PendingSubscriptionsViewModel.kt`

**Legado (no copiar en código nuevo):** `IRepository` directo, `init { load() }`, LiveData, try/catch en ViewModel.

### Stack real

| Área | Tecnología |
|---|---|
| DI | Koin (no Hilt) — `di/KoinApplication.kt` |
| Red | Retrofit + OkHttp + Gson. Kotlin Serialization solo para rutas `@Serializable` |
| Local | Room en `:data` (sync/catálogo). SharedPreferences + `TokenStore` para sesión |
| Imágenes | Coil |
| Nav | Navigation Compose (`IpsAdminNavHost.kt`, `NavRoutes.kt`). Sin `navigation/*.xml` |
| Firebase | Analytics, Messaging, Crashlytics, App Distribution |
| Tests | JUnit 4, MockK, Robolectric + Compose UI Test en `src/test`. **0 androidTest** |
| Turbine / Detekt / Ktlint / Spotless | No |

### Comandos reales (Windows)

```bat
gradlew.bat :presentation:compileDevDebugKotlin
gradlew.bat :presentation:testDevDebugUnitTest --tests "com.dscorp.ispadmin.<Fqcn>"
gradlew.bat :domain:test
gradlew.bat :data:test
gradlew.bat :presentation:lintDevDebug
gradlew.bat :presentation:assembleProdDebug
gradlew.bat :presentation:installDevDebug
gradlew.bat adbReverseAll
```

No existe `installDevDebugWithReverse`. Usar `debugWithReverse` o `adbReverseAll`.  
No existe Spotless/Detekt. `InstrumentationTestRunner` está referenciado en Gradle y **no está en el repo**.

### Archivos canónicos

- ViewModel UDF: `presentation/.../subscription/pending/PendingSubscriptionsViewModel.kt`
- Screen Compose: `presentation/.../subscription/pending/PendingSubscriptionsScreen.kt`
- UseCase real: `domain/.../usecase/subscription/SyncPendingSubscriptionsUseCase.kt`
- Puerto + impl: `domain/.../PendingSubscriptionRepository.kt` + `data/.../PendingSubscriptionRepositoryImpl.kt`
- API monolítica: `presentation/.../datasource/remote/RestApiServices.kt`
- API acotada: `presentation/.../auth/AuthApiService.kt`
- Mapper: `data/.../local/mapper/CatalogMapper.kt`
- Test VM: `presentation/src/test/.../pending/PendingSubscriptionsViewModelTest.kt`
- Nav: `presentation/.../navigation/IpsAdminNavHost.kt`
- App: `presentation/.../di/KoinApplication.kt`

### Vocabulario de dominio

Alinear con el backend WISP: **Subscription** (no Client/Customer), **Payment** (no Invoice), **Plan**, **Place**, **NapBox**, **User** (staff).

## Engineering core

### Antes de editar

Buscar, en este orden: implementaciones similares → tests existentes → arquitectura **local** de la feature → dependencias ya inyectadas → usages de APIs que vayas a cambiar.

Seguir el patrón del rincón. Código Compose nuevo: `PendingSubscriptions*`. XML / Fragment / `IRepository`: no migrarlos de paso.

### Canónico (UDF nuevo)

- VM: `presentation/.../subscription/pending/PendingSubscriptionsViewModel.kt`
- Screen: `presentation/.../subscription/pending/PendingSubscriptionsScreen.kt`
- UseCase: `domain/.../usecase/subscription/SyncPendingSubscriptionsUseCase.kt`
- Port: `domain/.../repository/PendingSubscriptionRepository.kt`

`Screen → ViewModel.onIntent → UseCase si aporta valor → port :domain → impl :data`

### Invariantes

- Para lógica nueva testeable: **RED → GREEN → REFACTOR** (obligatorio; ver `gigafiber/AGENTS.md`).
- No crear una abstracción hasta poder explicar qué problema **real** resuelve **en este repo**.
- Una clase extra debe **bajar** complejidad total, no mover código de sitio.
- No UseCase passthrough (`invoke() = repository.x()`) salvo consistencia del módulo.
- No hinchar `IRepository`. No Hilt, Ktor, Moshi, DataStore, Turbine, Navigation XML, ni otro ORM.
- Stack cerrado: Koin, Retrofit+Gson, Room en `:data`, Coil, Navigation Compose, JUnit4+MockK.
- Preservar comportamiento. Diff mínimo.

### DoD

`gradlew.bat :presentation:compileDevDebugKotlin` y el `*Test` afectado (`testDevDebugUnitTest`).

## Kotlin

**Ámbito:** `presentation/**/*.kt`, `domain/**/*.kt`, `data/**/*.kt`, `observability/**/*.kt`

- `val` por defecto. `var` solo con mutación local justificada.
- Null-safety: `?.`, `?:`, `requireNotNull` con mensaje. **Prohibido `!!` en código nuevo** (el legado lo usa; no copiarlo).
- Estados finitos: `sealed interface` / `sealed class` + `when` exhaustivo.
- Datos: `data class` inmutables. Colecciones `List`/`Map` por defecto; `Mutable*` encapsulado.
- Named arguments cuando hay 3+ params o booleanos.
- Expression body solo si cabe en una línea clara.
- Extensiones cuando nombran una operación del dominio (`toDto`, `toListItem`), no para esconder lógica.
- Scope functions (`apply`/`also`/`run`) solo si mejoran lectura. Evitar anidarlas.
- Máximo ~30 líneas por función; extraer helpers con nombre de intención.
- Máximo 3 parámetros; el resto va en un tipo (`UiState`, request, config).
- Cero magia: constantes `SCREAMING_SNAKE_CASE` o `BuildConfig`.
- Nombres de dominio: `Subscription`, `Payment`, `Plan`, `NapBox`, `User` (staff). No `Client`/`Invoice`.
- Evitar `Utils`/`Helpers` cajón de sastre. Un tipo = un propósito.

```kotlin
// ❌
val body = response.body()!!

// ✅
val body = response.body() ?: return Result.failure(IllegalStateException("Empty body"))
```

## Arquitectura

**Ámbito:** `presentation/**/*ViewModel.kt`, `presentation/**/*Screen.kt`, `presentation/**/*UseCase.kt`, `domain/**/*UseCase.kt`, `domain/**/*Repository*.kt`, `data/**/*Repository*.kt`, `presentation/**/*Repository*.kt`, `**/di/**/*.kt`

Híbrido. No es Clean Architecture pura.

```text
Nuevo:  Screen → VM.onIntent → UseCase (si hay regla) → port :domain → impl :data
Legado: Fragment/XML + IRepository — no migrar ni hinchar Repository.kt
```

Canónico nuevo:

- `presentation/src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/pending/PendingSubscriptionsViewModel.kt`
- `presentation/src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/pending/PendingSubscriptionsScreen.kt`
- `domain/src/main/java/com/dscorp/ispadmin/domain/usecase/subscription/SyncPendingSubscriptionsUseCase.kt`
- `domain/src/main/java/com/dscorp/ispadmin/domain/repository/PendingSubscriptionRepository.kt`

`:domain` sin Android/Retrofit/Room/Gson/Compose.

- VM nuevo: sin `init` de carga, sin `IRepository`, sin Retrofit.
- UseCase: no si solo delega una línea.
- Feature ya en `IRepository` → no extraer capas “por arquitectura”.
- Prohibido el sandwich Screen→VM→UseCase→Repo→DS→Mapper→Facade si no hace falta.

## Tests (Android)

Cumple TDD de plataforma (`gigafiber/AGENTS.md`). Detalle de stack en este repo:

**Ámbito:** `presentation/**/*Test.kt`, `domain/**/*Test.kt`, `data/**/*Test.kt`, `observability/**/*Test.kt`, `**/*AndroidTest.kt`.

El fallo RED se demuestra con Gradle. No afirmar TDD si se implementó antes.

### Stack (no cambiar)

JUnit 4 + MockK. VM: `StandardTestDispatcher` + dispatcher inyectable. Compose UI: Robolectric en `src/test` (no hay `androidTest`). Sin Turbine/Mockito en tests nuevos.

Canónico: `presentation/src/test/.../pending/PendingSubscriptionsViewModelTest.kt`

```bat
gradlew.bat :presentation:testDevDebugUnitTest --tests "com.dscorp.ispadmin....FooTest"
```

RED = comportamiento ausente, no test que no compile.

### Legacy

Sin cobertura: characterization → cambio mínimo → test del bug. No reescribir para “TDD perfecto”.

No testear getters, layouts cosméticos, wiring Koin.

## Coroutines / Flow

**Ámbito:** `presentation/**/*ViewModel.kt`, `presentation/**/*UseCase.kt`, `domain/**/*UseCase.kt`, `domain/**/*Repository*.kt`, `data/**/*Repository*.kt`, `presentation/**/*Repository*.kt`

- VM: `viewModelScope` + dispatcher inyectado (`mainImmediate`). Collect largo: cancelar Job (ver `PendingSubscriptionsViewModel`).
- Compose: `LaunchedEffect`. Views: `repeatOnLifecycle`. **Prohibido** `GlobalScope` / copiar `MainScope` de FCM.
- Estado: `StateFlow` + `data class`. One-shot: `SharedFlow`. Room: cold `Flow`. LiveData solo legado XML.
- No tragar `CancellationException`. No `runBlocking` ni `Thread.sleep` fuera de tests (dispatcher de test).

## Compose

**Ámbito:** `presentation/**/*Screen.kt`, `presentation/**/*Content.kt`, `presentation/**/navigation/**/*.kt`, `presentation/**/composecomponents/**/*.kt`

Código nuevo: Compose + Material 3 + `MyTheme`. Canónico: `PendingSubscriptionsScreen.kt` (mismo package que el ViewModel UDF).

```text
Screen(koinViewModel) → Content(uiState, onIntent) → hijos stateless
```

- Hijo **sin** ViewModel. Collect: `collectAsStateWithLifecycle()`.
- `LaunchedEffect` dispara `Load`; no `init` en el VM.
- Interactivo: `testTag`. Ícono: `contentDescription`.
- Lazy: `key` = id de dominio. Previews de Content.
- Rutas `@Serializable` en `NavRoutes.kt` + `IpsAdminNavHost`.
- Prohibido: red/repo/UseCase en Composable; Navigation XML.

## Views / XML (legado)

**Ámbito:** `presentation/**/layout/**/*.xml`, `presentation/**/*Fragment.kt`, `presentation/**/*Activity.kt`

Existen ~33 layouts y ~11 Fragments. **No migrar a Compose** ni apagar ViewBinding/DataBinding en una feature.

### Al tocar legado

- ViewBinding (o DataBinding si el layout ya lo usa). Sin `findViewById`.
- Lógica de negocio fuera del Fragment/Activity. ViewModel existente, aunque sea LiveData.
- `viewLifecycleOwner` para observers. Limpiar listeners en `onDestroyView`.
- RecyclerView: `ListAdapter` + `DiffUtil` si se toca el adapter.
- `findNavController()` solo en Fragments que ya navegan así. Features nuevas → Navigation Compose.
- ConstraintLayout en XML existente; no reescribir a Compose “de paso”.
- `contentDescription` en íconos/imagenes clickeables.

### Memory leaks

- No guardar `View`/`Binding`/`Activity` en el ViewModel.
- `binding = null` en `onDestroyView` si el Fragment lo usa.
- No `observeForever` sin remove.

### Activities

Hay 4: `MainActivity` (Compose NavHost), `MigrationActivity`, `TicketActivity`, `BaseActivity`. No crear Activities nuevas si basta un destino Compose.

## Data / networking

**Ámbito:** `data/**/*.kt`, `presentation/**/data/**/*.kt`, `presentation/**/datasource/**/*.kt`, `presentation/**/*Api*.kt`, `data/**/*Dao*.kt`, `data/**/*Database*.kt`, `data/**/*Mapper*.kt`, `presentation/**/*Retrofit*.kt`, `presentation/**/*TokenStore*.kt`

### Red

- Retrofit + OkHttp + **Gson**. Kotlin Serialization no es el converter HTTP (solo rutas).
- APIs nuevas acotadas al feature (`AuthApiService` style). Evitar crecer `RestApiServices.kt` si puedes un service separado.
- Timeouts e interceptors se configuran en `RetrofitModule.kt`. No duplicar clientes.
- Auth: interceptors existentes + `TokenStore`. No loguear `Authorization`, tokens, passwords.
- Chucker/Stetho son debug. No activarlos en lógica de prod.
- Errores: mapear HTTP/red a `Result.failure` / sealed de dominio. No `body()!!`.
- Retry solo si la operación es idempotente y el módulo ya reintenta (sync offline).
- Cancelación: coroutines del caller; no `enqueue` callback nuevo.

### Persistencia

- Room vive en `:data` (`IspAdminDatabase`). Entidades Room ≠ modelos de dominio; mapear (`CatalogMapper`).
- SharedPreferences / `TokenStore` = sesión. **No** introducir DataStore en una feature.
- Source of truth: Room para pending/catalog; red para el resto salvo diseño explícito.
- Prohibido `fallbackToDestructiveMigration()` en prod. Toda schema change = Migration.
- Queries Room en background (DAO suspend/`Flow`). UI nunca toca DAO.

### Mapeo

DTO Retrofit → domain → UI model. No pasear DTO Gson hasta Compose salvo pantalla trivial y ya existente.

## Dependency injection

**Ámbito:** `presentation/**/di/**/*.kt`, `domain/**/di/**/*.kt`, `data/**/di/**/*.kt`, `presentation/**/*Module.kt`, `presentation/**/*ViewModel.kt`, `**/*.gradle`

**Solo Koin.** No Hilt, no Dagger, no Service Locator global nuevo.

### Dónde registrar

`KoinApplication` carga: `domainModule`, `dataModule`, `retrofitModule`, `apiModule`, `repositoryModule`, `viewModelModule`, `useCaseModule`, `localDataModule`, `observabilityModule`, …

- ViewModel → `viewModelModule` + `viewModel { }` / `viewModelOf`.
- UseCase → `useCaseModule` o `domainModule` si vive en `:domain`.
- Repo impl → `dataModule` / `repositoryModule`.
- No `get()` / `by inject()` dentro de Composables. `koinViewModel()` en la Screen.
- Legado: algunos VM usan `by inject()`. No copiarlo en código nuevo.

### Construcción

```kotlin
// ✅ constructor injection (testeable)
class FooViewModel(
    private val barUseCase: BarUseCase,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel()

// ❌
class FooViewModel : ViewModel() {
    private val repo: IRepository by inject()
}
```

- Default dispatcher en el constructor para tests, como `PendingSubscriptionsViewModel`.
- `allowOverride(true)` ya está; no abusar. No `getKoin()` desde dominio.
- Presentation puede depender de domain/data. Domain **no** depende de Koin Android.

## Seguridad

**Ámbito:** `presentation/**/AndroidManifest.xml`, `presentation/**/network_security_config.xml`, `presentation/**/*TokenStore*.kt`, `**/local.properties*`, `presentation/**/*Interceptor*.kt`, `presentation/**/di/LocalData*.kt`

### Secretos

- Nunca commitear valores de `local.properties`. Nombres van en `local.properties.example`: `MAPS_API_KEY`, `OBS_API_KEY_ANDROID`, `gpr.key`, etc.
- Prod exige `OBS_API_KEY_ANDROID`. No hardcodear keys en Kotlin.
- No loguear ni Crashlytics: tokens, `Authorization`, passwords, `SESSION_PASSWORD`.

### Almacenamiento (realidad actual)

`TokenStore` y prefs `MODE_PRIVATE` guardan access/refresh y passwords de sesión en claro. **No empeorar**: no agregar más secretos a prefs. Código nuevo de credenciales debe justificar almacenamiento; no introducir EncryptedSharedPreferences “de paso” sin tarea de seguridad.

`android:allowBackup="true"` está activo. No guardar PII extra en backup-able storage sin revisión.

### Red

- Manifest tiene `usesCleartextTraffic="true"` y `network_security_config` con cleartext a localhost/LAN (dev).
- No ampliar cleartext a dominios públicos. Prod habla HTTPS (`api.gigafiberperu.cloud`).
- Cert pinning: no existe; no inventarlo en una feature.

### Componentes

- Revisar `exported` al tocar Manifest. `MainActivity` es launcher exported.
- `CloudMessagingService` existe en código; verificar Manifest antes de asumir FCM registrado.
- Intents/deep links: validar extras; no exportar Activities nuevas sin necesidad.
- FileProvider ya `exported=false`. Permisos: mínimo privilegio.

### R8

Release usa minify del módulo. No debilitar keep rules. No actualizar ProGuard “por si acaso”.

## Performance / lifecycle

Usar ante ANR, jank, memory leaks o listas grandes. No aplicar en cambios triviales.

minSdk 26. No micro-optimizar sin evidencia.

- VM: sin referencias a View/Activity. Compose: keys reales; cancelar al salir.
- Fragment legado: `viewLifecycleOwner`; `binding = null` en `onDestroyView`.
- I/O, Room, Gson, imágenes: fuera del Main. Listas: Lazy + key de id.
- Coil acotado. No `MainScope` nuevo; WorkManager ya está en `:observability`.

## Gradle

**Ámbito:** `**/*.gradle`, `**/*.gradle.kts`, `**/gradle.properties`, `**/versions.gradle`

DSL **Groovy**. Versiones en `versions.gradle` + `gradle.properties`. No TOML catalogs. No migrar a `.kts` en una feature.

### Módulos y flavors

- App: `:presentation`. Libs: `:domain`, `:data`, `:observability`.
- Flavors: `dev` / `staging` / `prod`. Tareas de desarrollo: `*DevDebug*`. Staging e2e: `*StagingDebug*`.
- `compileSdk 36`, `targetSdk 34`, `minSdk 26`, JVM 17. No subir SDKs “de paso”.

### Scopes

`implementation` por defecto. `api` solo si el módulo consumidor debe ver el tipo. `testImplementation` para MockK/JUnit/Robolectric. No mezclar `androidTest` (está vacío).

### Dependencias

Antes de agregar librería: ¿API oficial? ¿Ya está en el proyecto (Koin, Retrofit, Gson, Coil, Room, MockK)? ¿minSdk 26? ¿tamaño/seguridad?

No actualizar AGP/Kotlin/Firebase porque “hay versión nueva”. Kotlin plugin 2.1.0 vs stdlib 2.1.21 es deuda conocida.

### Comandos

```bat
gradlew.bat :presentation:compileDevDebugKotlin
gradlew.bat :presentation:testDevDebugUnitTest
gradlew.bat :presentation:lintDevDebug
gradlew.bat :presentation:assembleProdDebug
gradlew.bat :presentation:installDevDebug
gradlew.bat adbReverseAll
```

Prod APK + distribución: sección Firebase App Distribution (bump versionCode/Name).  
Secrets de Maps/OBS: `local.properties`, nunca en `build.gradle`.

## Firebase App Distribution (prodDebug)

**Ámbito:** `presentation/build.gradle`

Antes de **cada** subida de APK a App Distribution:

1. Incrementar en `presentation/build.gradle` → `defaultConfig`:
   - `versionCode` +1
   - `versionName`: incrementar el segmento patch (ej. `2.6.1` → `2.6.2`)
2. Compilar: `gradlew.bat :presentation:assembleProdDebug`
3. Distribuir con `scripts/firebase-distribute-prod-debug.sh` (o el mismo flujo manual).

No subir un APK sin haber bumpado versión en el mismo turno. Incluir `versionName (versionCode)` en `--release-notes`.

Proyecto Firebase prod: `ispadmin-687ca`. App ID: `1:74615302027:android:87751a6bad0a78cbb0318e`. Grupo testers: `gigafiber`.
