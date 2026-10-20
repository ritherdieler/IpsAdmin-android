# Eliminar el soporte offline del registro · Entrega 1 · Plan de implementación

> **Para quien ejecute el plan:** usar `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`. Los pasos usan casillas `- [ ]`.

**Meta:** que el registro de suscripciones exija conexión (sin modo offline, sin cola nueva y sin catálogo en Room) y que la pantalla de pendientes quede solo para enviar o descartar lo ya encolado, con telemetría para decidir la entrega 2.

**Arquitectura:** el catálogo de registro se pide directo a la API en memoria; `RegisterSubscriptionUseCase` siempre registra en línea; se borra todo lo que detectaba o usaba el modo offline en el ViewModel y la interfaz; la cola existente se conserva solo para vaciarse. El backend y el esquema de Room no cambian.

**Tecnologías:** Kotlin, Jetpack Compose, Koin, Room (solo conservado), JUnit + MockK, Robolectric con Compose test.

**Spec:** `.agent-docs/eliminar-soporte-offline-registro-2026-10-20.md` (aprobado), §4 y §6.

Rutas abreviadas: `PRES` = `presentation/src/main/java/com/dscorp/ispadmin`, `PREST` = `presentation/src/test/java/com/dscorp/ispadmin`, `DOM` = `domain/src/main/java/com/dscorp/ispadmin/domain`, `DOMT` = `domain/src/test/java/com/dscorp/ispadmin/domain`, `DATA` = `data/src/main/java/com/dscorp/ispadmin/data`, `DATAT` = `data/src/test/java/com/dscorp/ispadmin/data`, `REG` = `PRES/presentation/ui/features/subscription/register`, `REGT` = `PREST/presentation/ui/features/subscription/register`. Todo se ejecuta desde la raíz del repo `IpsAdmin-android app`; el trabajo se hace en un worktree propio.

## Restricciones globales

- Textos visibles en español, en sentence case, sin signos de cierre en etiquetas; código, comentarios y nombres en inglés conciso; mensajes de commit en inglés.
- Un commit por tarea en la rama de trabajo; sin push (solo con petición explícita).
- Backend sin cambios. El esquema de Room NO cambia: no se tocan `IspAdminDatabase` (versión 1), entidades ni DAOs, ni las tablas `catalog_*`; quedan sin uso con un comentario que remite a la entrega 2.
- WorkManager, `ACCESS_NETWORK_STATE`, la preautorización de ONU, la reconciliación por `registrationOperationId` y la verificación de versión NO se tocan.
- Los campos `clientIpAddress` e `ip` del modelo `Subscription` se conservan (se envían nulos).
- Si falla la red al registrar, se conserva el flujo existente: `Uncertain` con «No se pudo confirmar si el registro quedó guardado. Puedes reintentar: no se duplicará.» y el reintento reutiliza el mismo `clientRequestId`.
- Pruebas: `./gradlew :presentation:testDevDebugUnitTest --tests "<patrón>"`, `./gradlew :domain:test --tests "<patrón>"` y `./gradlew :data:testDebugUnitTest --tests "<patrón>"` (si la tarea del módulo `data` tuviera otro nombre, usar la equivalente y anotarlo en el informe). Las pruebas instrumentadas siguen la regla de pruebas largas del `AGENTS.md` de plataforma.

## Review Focus

1. Un fallo de red al registrar (no preautorizado) deja el formulario intacto en `Uncertain` y el reintento envía el MISMO `clientRequestId` (Tarea 2).
2. La preautorización de fibra (`registrationOperationId`) sigue registrando en línea sin foto y sin tocar la nueva lógica (Tarea 2).
3. Si fallan los endpoints obligatorios del catálogo (planes, lugares, cajas NAP) el asistente muestra el error de carga inicial y no abre el formulario; si fallan ONU o equipos de core el catálogo sigue con listas vacías, como antes (Tarea 1).
4. Un teléfono con registros ya encolados los ve, los envía y los descarta tras actualizar; cuando el último sale, la entrada de menú y el banner desaparecen sin cerrar la pantalla de forma abrupta (Tarea 3).
5. Un borrador antiguo que trae la clave `clientIpAddress` se restaura sin error (Tarea 2).

---

### Tarea 1: Catálogo directo a la API

**Archivos:**
- Modificar: `DOM/repository/RegistrationCatalogRepository.kt`, `DOM/usecase/catalog/GetRegistrationCatalogUseCase.kt`, `DOM/model/RegistrationCatalog.kt`, `DOM/di/DomainModule.kt`, `DATA/repository/RegistrationCatalogRepositoryImpl.kt`, `DATA/di/DataModule.kt`, `PRES/di/ViewModelModule.kt`, `REG/compose/RegisterSubscriptionComposeViewModel.kt` (constructor y `applyInitialCatalogData`)
- Eliminar: `DOM/usecase/catalog/RefreshRegistrationCatalogUseCase.kt`, `DOM/exception/CatalogNotAvailableOffline.kt`, `DATA/local/mapper/CatalogMapper.kt` (solo si ningún otro código lo usa; si lo usa, conservarlo y anotarlo)
- Conservar sin tocar: entidades de catálogo, `CatalogDao`, `IspAdminDatabase` (añadir el comentario «sin uso; se elimina en la entrega 2» al `CatalogDao`)
- Probar: `DATAT/repository/RegistrationCatalogRepositoryTest.kt`, `DOMT/usecase/catalog/GetRegistrationCatalogUseCaseTest.kt`, `DOMT/usecase/catalog/RefreshRegistrationCatalogUseCaseTest.kt` (eliminar), `REGT/compose/RegisterSubscriptionComposeViewModelTest.kt`, `REGT/compose/RegisterExistingCustomerViewModelTest.kt`

**Interfaces:**
- Produce:
  - `RegistrationCatalogRepository { suspend fun fetchCatalog(): Result<RegistrationCatalog> }` (se eliminan `getCachedCatalog`, `refreshFromRemote` y `observeLastSync`).
  - `RegistrationCatalogRepositoryImpl(remote: RegistrationCatalogRemoteDataSource)`; en `DataModule`: `RegistrationCatalogRepositoryImpl(get())`.
  - `GetRegistrationCatalogUseCase(repository)` con `suspend operator fun invoke(): Result<RegistrationCatalog>` que devuelve el resultado del repositorio.
  - `RegisterSubscriptionComposeViewModel` sin el parámetro `refreshRegistrationCatalogUseCase`; el bloque posicional de `ViewModelModule` se ajusta igual.

- [ ] **Paso 1: escribir las pruebas que fallan.**
  - `RegistrationCatalogRepositoryTest` (datos, reescrita sin Room): `fetchCatalog maps plans, places, nap boxes, onus and core devices` (`remote` falso con un elemento de cada tipo y aserciones sobre la conversión); `a failing plans fetch returns failure`, `a failing places fetch returns failure`, `a failing nap boxes fetch returns failure` (cada uno con `assertTrue(result.isFailure)`); `a failing onu fetch gives an empty onu list and succeeds`; `a failing core devices fetch gives an empty list and succeeds`; `cancellation is rethrown` (el remoto lanza `CancellationException` y la llamada la propaga).
  - `GetRegistrationCatalogUseCaseTest`: `invoke returns the repository result` (éxito y fallo).
  - ViewModel: quitar `refreshRegistrationCatalogUseCase` de los constructores de prueba y el `coVerify(exactly = 1) { refreshRegistrationCatalogUseCase() }`; añadir `a failing catalog fetch emits the initial catalog error and does not open the form` (el caso de uso devuelve `Result.failure`; se emite `RegisterSubscriptionUiEvent.Error`, `isLoading == false` y `registerSubscriptionForm.planList` queda vacía).
- [ ] **Paso 2: ejecutar y ver que fallan**: `./gradlew :data:testDebugUnitTest --tests "*RegistrationCatalogRepositoryTest"`, `./gradlew :domain:test --tests "*GetRegistrationCatalogUseCaseTest"`, `./gradlew :presentation:testDevDebugUnitTest --tests "*RegisterSubscriptionComposeViewModelTest"`. Esperado: FAIL por referencias sin resolver.
- [ ] **Paso 3: implementar.** `fetchCatalog` pide planes, lugares y cajas NAP (cualquier fallo devuelve `Result.failure`) y ONU y equipos de core de forma opcional (un fallo da lista vacía), sin persistir ni escribir en Room; propaga `CancellationException`. En `applyInitialCatalogData` quitar la llamada a `refreshRegistrationCatalogUseCase()` y dejar solo `getRegistrationCatalogUseCase()`. Borrar `isAvailableOffline()` y todo lo listado en «Eliminar»; ajustar `DomainModule`, `DataModule` y `ViewModelModule`.
- [ ] **Paso 4: ejecutar y ver que pasan** los tres comandos anteriores más `./gradlew :presentation:testDevDebugUnitTest --tests "*RegisterExistingCustomerViewModelTest"`. Luego `./gradlew :presentation:compileDevDebugKotlin`.
- [ ] **Paso 5: commit**: `feat(register): load the registration catalog straight from the API`.

---

### Tarea 2: El registro siempre es en línea y sin IP offline

**Archivos:**
- Modificar: `PRES/domain/usecase/subscription/RegisterSubscriptionUseCase.kt`, `PRES/domain/usecase/subscription/RegisterSubscriptionResult.kt`, `PRES/domain/usecase/subscription/SubmitAndTrackRegistrationUseCase.kt`, `PRES/di/useCaseModule.kt` (línea de `RegisterSubscriptionUseCase`), `DOM/di/DomainModule.kt`, `DATA/di/DataModule.kt`, `PRES/di/ViewModelModule.kt`, `REG/compose/RegisterSubscriptionComposeViewModel.kt`, `REG/compose/RegisterSubscriptionScreen.kt`, `REG/compose/RegisterServiceStep.kt`, `REG/compose/RegisterReviewStep.kt`, `REG/models/RegisterSubscriptionState.kt`, `REG/models/RegisterSubscriptionFormState.kt`, `REG/models/FormFieldKey.kt`, `REG/models/RegisterSubscriptionIntent.kt`, `REG/models/RegisterSubscriptionUiEvent.kt`, `REG/models/RegisterSubscriptionWizardStep.kt`, `REG/RegisterSubscriptionTestTags.kt`, y el cableado de navegación que pasaba el callback de «ir a pendientes» al diálogo (`PRES/navigation/FeatureNavGraph.kt`)
- Eliminar: `DOM/usecase/subscription/ObserveOfflineRegistrationModeUseCase.kt`, `DOM/usecase/subscription/EnqueuePendingSubscriptionUseCase.kt`, `DOM/connectivity/NetworkConnectivityMonitor.kt`, `DOM/connectivity/MikrotikReachabilityMonitor.kt`, `DATA/connectivity/NetworkConnectivityMonitorImpl.kt`, `DATA/connectivity/MikrotikReachabilityMonitorImpl.kt`, `PRES/domain/model/subscription/ClientIpAddressValidation.kt`, el diálogo `QueuedOfflineDialog`, y sus pruebas (`ObserveOfflineRegistrationModeUseCaseTest`, `EnqueuePendingSubscriptionUseCaseTest`, `NetworkConnectivityMonitorImplTest`, `MikrotikReachabilityMonitorImplTest` y la prueba de `ClientIpAddressValidation` si existe). Antes de borrar los monitores de conectividad, comprobar con una búsqueda que ningún otro código los usa; si algo los usa, conservarlos y anotarlo en el informe.
- Probar: `PREST/domain/usecase/subscription/RegisterSubscriptionUseCaseTest.kt`, `PREST/domain/usecase/subscription/SubmitAndTrackRegistrationUseCaseTest.kt`, `REGT/compose/RegisterSubscriptionComposeViewModelTest.kt`, `REGT/compose/RegisterExistingCustomerViewModelTest.kt`, `REGT/compose/RegisterResultScreensTest.kt`, `REGT/compose/RegisterServiceStepTest.kt`, `REGT/compose/RegisterSubscriptionFormWizardTest.kt`, `REGT/compose/RegisterSubscriptionFormHostDeviceTest.kt`, `REGT/RegisterSubscriptionTestTagsTest.kt`, `PREST/domain/model/subscription/RegisterSubscriptionFormValidationTest.kt`, `REGT/models/RegisterSubscriptionWizardStepTest.kt` (las rutas exactas se localizan por nombre; las que no existan se omiten)

**Interfaces:**
- Consume (Tarea 1): `RegisterSubscriptionComposeViewModel` sin `refreshRegistrationCatalogUseCase`.
- Produce:
  - `RegisterSubscriptionUseCase(subscriptionWriteRepository: SubscriptionWriteRepository, uuidGenerator: () -> String = { UUID.randomUUID().toString() })`; en `useCaseModule`: `RegisterSubscriptionUseCase(get())`. Con `registrationOperationId` registra en línea sin foto; sin él registra en línea con la foto; nunca encola.
  - `RegisterSubscriptionResult` conserva solo `Registered(subscription)`; `RegistrationSubmission` pierde `Queued`; `RegisterSubscriptionUiEvent` pierde `QueuedOffline`.
  - `RegisterSubscriptionComposeViewModel` sin `observeOfflineRegistrationModeUseCase`; `RegisterSubscriptionState` sin `isOfflineMode`; `RegisterSubscriptionFormState` sin `requiresClientIpAddress`, `clientIpAddress` ni `clientIpAddressError`; sin `FormFieldKey.CLIENT_IP_ADDRESS`, `RegisterSubscriptionIntent.ClientIpAddressChanged` ni `RegisterSubscriptionTestTags.CLIENT_IP`.

- [ ] **Paso 1: escribir o ajustar las pruebas que fallan.**
  - `RegisterSubscriptionUseCaseTest`: `with a registration operation id it registers online without the photo`; `without an operation it registers online with the photo`; `a network failure returns a failure and never queues` (el repositorio lanza `IOException` → `result.isFailure`, sin interacción con ninguna cola); `it keeps the given clientRequestId and generates one when missing`.
  - `SubmitAndTrackRegistrationUseCaseTest`: quitar los casos de `Queued`; conservar `IOException → Uncertain` sin operación y la reconciliación con operación.
  - ViewModel: quitar las pruebas de modo offline, de IP offline y de `QueuedOffline`, y las de `withOfflineState`; añadir `a network failure on submit keeps the form in the uncertain state and the retry reuses the same clientRequestId` (Solo TV sin operación; la primera llamada devuelve `Result.failure(IOException())`, `submission` queda `SubmissionState.Uncertain`; en la segunda llamada capturada, `clientRequestId` es igual al de la primera) y `a preauthorized fiber registration still registers online with its operation id` (sin cambios de comportamiento).
  - Formulario: `a draft saved by an older version with a clientIpAddress key restores without error` (borrador con la clave; no falla y el estado no tiene IP).
  - Quitar de las pruebas de formulario, pasos, etiquetas y revisión todo lo referente a la IP del cliente.
- [ ] **Paso 2: ejecutar y ver que fallan**: `./gradlew :presentation:testDevDebugUnitTest --tests "*RegisterSubscriptionUseCaseTest" --tests "*SubmitAndTrackRegistrationUseCaseTest" --tests "*RegisterSubscriptionComposeViewModelTest"`. Esperado: FAIL.
- [ ] **Paso 3: implementar.** Borrar los archivos de «Eliminar» y todo lo que la compilación señale como referencia a lo eliminado. En el ViewModel: quitar `observeOfflineMode`, `deviceOffline`, `withOfflineState` (dejando el `copy` interno de cada bloque de recuperación), la rama `Queued`, `onClientIpAddressChanged` y las claves `clientIpAddress` del guardado y la restauración de borrador (la restauración ignora la clave si existe). En `buildSubscriptionFromForm` dejar de llenar `clientIpAddress` e `ip`. Quitar el parámetro de «ir a pendientes» del diálogo en `RegisterSubscriptionScreen` y el cableado correspondiente en `FeatureNavGraph` sin tocar la ruta de la pantalla de pendientes. Ajustar `DomainModule`, `DataModule`, `useCaseModule` y `ViewModelModule`.
- [ ] **Paso 4: ejecutar y ver que pasan**: el comando anterior más `./gradlew :presentation:testDevDebugUnitTest --tests "*register*"`, `./gradlew :domain:test` y `./gradlew :data:testDebugUnitTest`. Luego `./gradlew :presentation:compileDevDebugAndroidTestKotlin`.
- [ ] **Paso 5: commit**: `feat(register): registration requires a connection, no offline queue`.

---

### Tarea 3: La cola existente solo se vacía

**Archivos:**
- Modificar: `PRES/presentation/ui/features/more/MoreMenu.kt`, `PRES/navigation/MainNavigation.kt`, `PRES/presentation/ui/features/home/FieldModel.kt`, `PRES/presentation/ui/features/home/FieldContent.kt`, `PRES/presentation/ui/features/home/FieldViewModels.kt` (solo si el cambio de modelo lo exige), `PRES/presentation/ui/features/subscription/pending/PendingSubscriptionsScreen.kt`
- Probar: `PREST/presentation/ui/features/more/MoreScreenUiTest.kt`, `PREST/navigation/MainNavigationTest.kt`, `PREST/presentation/ui/features/home/TechnicianTodayModelTest.kt`, `PREST/presentation/ui/features/home/FieldViewModelsTest.kt`, `PREST/presentation/ui/features/subscription/pending/PendingSubscriptionsScreenTest.kt`

**Interfaces:**
- Produce:
  - `TechnicianToday.uploads: UploadBanner?`, nulo cuando no hay pendientes (se quita el banner verde «Todo enviado»).
  - El menú Más y la navegación del rol técnico incluyen `MoreItem.PENDING_SUBSCRIPTIONS` solo cuando el conteo de pendientes es mayor que cero.
  - Textos nuevos: banner del inicio «N registros sin enviar» / «toca para enviarlos»; encabezado de la pantalla de pendientes «Envíalos con “Enviar ahora” cuando tengas señal» (en lugar de «Se envían solos al volver la señal»).

- [ ] **Paso 1: escribir las pruebas que fallan.**
  - `TechnicianTodayModelTest`: `no pending uploads gives no banner`; `pending uploads give a banner that says to tap to send them` (aserciones sobre el título «2 registros sin enviar» y el detalle).
  - `MoreScreenUiTest` y `MainNavigationTest`: `pending subscriptions entry is hidden with zero pending` y `is shown with its count when there are pending`.
  - `PendingSubscriptionsScreenTest`: `the header explains that sending is manual` (existe el texto nuevo y no existe «Se envían solos al volver la señal»); `sending the last pending item leaves an empty state` (con la lista vacía la pantalla muestra su estado vacío y no falla).
  - `FieldViewModelsTest`: ajustar el esperado de `uploads` a nulo con cero pendientes.
- [ ] **Paso 2: ejecutar y ver que fallan** los cinco archivos de prueba con `--tests`. Esperado: FAIL.
- [ ] **Paso 3: implementar** los cambios de Interfaces; buscar y corregir cualquier otro texto de las pantallas de pendientes e inicio que prometa envío automático.
- [ ] **Paso 4: ejecutar y ver que pasan** los mismos comandos y luego `./gradlew :presentation:testDevDebugUnitTest --tests "*register*"` más los de `home`, `more` y `navigation`.
- [ ] **Paso 5: commit**: `feat(pending): entries appear only while there are queued registrations`.

---

### Tarea 4: Telemetría de pendientes

**Archivos:**
- Crear: `PRES/presentation/ui/features/subscription/pending/PendingSubscriptionsTelemetry.kt`, `PREST/presentation/ui/features/subscription/pending/PendingSubscriptionsTelemetryTest.kt`
- Modificar: `PRES/presentation/ui/features/main/MainActivityViewModel.kt`, `PRES/di/ViewModelModule.kt` o el módulo Koin donde se resuelva, y `.agent-docs/observabilidad-taxonomia-eventos.md`

**Interfaces:**
- Consume: `ObservePendingSubscriptionsUseCase` (flujo con la lista de pendientes) y `ObservabilityClient.reportLog(message, severity, tags)`.
- Produce: `class PendingSubscriptionsTelemetry(observePending: ObservePendingSubscriptionsUseCase, observability: ObservabilityClient)` con `suspend fun reportOncePerProcess()`: la primera llamada del proceso envía `reportLog(message = "pending_subscriptions.count", severity = "info", tags = mapOf("count" to n))`; las siguientes no hacen nada. `MainActivityViewModel` la invoca desde `viewModelScope` cuando hay usuario con sesión.

- [ ] **Paso 1: escribir las pruebas que fallan** en `PendingSubscriptionsTelemetryTest`: `reports the pending count once per process` (dos llamadas con 3 pendientes → `reportLog` exactamente una vez con `count = 3`); `reports zero when nothing is queued` (`count = 0`, que es el dato que habilita la entrega 2); `a different instance in the same process does not report again`.
- [ ] **Paso 2: ejecutar y ver que fallan**: `./gradlew :presentation:testDevDebugUnitTest --tests "*PendingSubscriptionsTelemetryTest"`. Esperado: FAIL.
- [ ] **Paso 3: implementar** la clase con una guarda de proceso (un `AtomicBoolean` en un `companion object`), cablear en `MainActivityViewModel` solo si hay sesión, y documentar el mensaje y la etiqueta en la taxonomía de eventos.
- [ ] **Paso 4: ejecutar y ver que pasa** el mismo comando y `./gradlew :presentation:compileDevDebugKotlin`.
- [ ] **Paso 5: commit**: `feat(observability): report the pending registrations count once per process`.

---

### Tarea 5: Documentación y verificación

**Archivos:**
- Modificar: `.agent-docs/eliminar-soporte-offline-registro-2026-10-20.md` (estado y desviaciones), `.agent-docs/registro-sin-onu-tipo-servicio-2026-10-20.md` (nota en §5.6 de que el modo offline fue retirado)

- [ ] **Paso 1: verificación global.** Ejecutar `./gradlew :domain:test :data:testDebugUnitTest :presentation:testDevDebugUnitTest` completo; esperado BUILD SUCCESSFUL sin fallos. Ejecutar `./gradlew :presentation:compileDevDebugAndroidTestKotlin`.
- [ ] **Paso 2: comprobaciones de que nada sobra ni falta.** Con búsquedas: ninguna referencia restante a `ObserveOfflineRegistrationModeUseCase`, `EnqueuePendingSubscriptionUseCase`, `NetworkConnectivityMonitor`, `MikrotikReachabilityMonitor`, `RefreshRegistrationCatalogUseCase`, `CatalogNotAvailableOffline`, `QueuedOffline` ni `requiresClientIpAddress`; `IspAdminDatabase` sin diff respecto a `develop`; WorkManager y `ACCESS_NETWORK_STATE` sin diff.
- [ ] **Paso 3: documentar.** Cambiar el estado del spec a «entrega 1 implementada en código; verificación manual en staging pendiente», anotar las desviaciones reales, y dejar en el spec de registro sin ONU la nota del modo offline retirado.
- [ ] **Paso 4 (la hace el usuario, no se delega): verificación manual en staging** con un teléfono real: registrar Fibra, Solo TV e Inalámbrico con red; probar el fallo de red al registrar (modo avión) y el reintento; instalar sobre una versión anterior con un registro encolado y comprobar que aparece, se envía y se descarta; comprobar que el evento `pending_subscriptions.count` llega a observabilidad.
- [ ] **Paso 5: commit**: `docs(register): record the offline removal, delivery 1`.

---

## Autorrevisión

- **Cobertura del spec:** §4.1 → Tarea 2; §4.2 → Tarea 1; §4.3 → Tarea 3; §4.4 → Tarea 4; §4.5 → restricciones globales y Tarea 5 (paso 2); §6 → pruebas por tarea más Tarea 5. Lo de la entrega 2 queda fuera.
- **Consistencia de nombres:** `fetchCatalog`, `GetRegistrationCatalogUseCase`, `RegisterSubscriptionUseCase(get())`, `TechnicianToday.uploads: UploadBanner?`, `PendingSubscriptionsTelemetry.reportOncePerProcess` y el mensaje `pending_subscriptions.count` se definen una vez y se usan igual.
- **Orden y compilación:** la Tarea 1 deja el ViewModel compilando sin `refreshRegistrationCatalogUseCase`; la Tarea 2 elimina el modo offline en una sola tarea porque los tipos sellados (`Queued`, `QueuedOffline`) no se pueden quitar a medias; la Tarea 3 solo toca la visibilidad de la cola; la Tarea 4 es independiente de las anteriores.
- **Quedan sin uso a propósito** (se borran en la entrega 2): `PendingSubscriptionRepository.enqueue`, `PendingFacadePhotoStorage.save`, las entidades y el DAO de catálogo, y los campos `clientIpAddress` e `ip` del modelo `Subscription`.
