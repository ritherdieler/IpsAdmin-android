# Eliminar el soporte offline del registro de suscripciones

Fecha: 2026-10-20 · Estado: **entrega 1 implementada en código el 2026-10-20; verificación manual en staging pendiente** · Repos: `IpsAdmin-android app` (entregas 1 y 2) e `ispadmin-backend` (solo entrega 2).

Sustituye lo que decía el spec `registro-sin-onu-tipo-servicio-2026-10-20.md` sobre el modo offline (§5.6): con este trabajo el registro deja de tener modo offline y la IP del cliente ya no se pide en la app.

## 1. Objetivo

Retirar el soporte offline para registrar suscripciones y su sincronización, en la app Android y en el backend. Después del trabajo:

1. El registro de una suscripción exige conexión con el servidor.
2. No existe cola local de registros pendientes ni sincronización de ella.
3. Ningún registro capturado sin conexión se pierde por la actualización (por eso se entrega en dos pasos).

## 2. Hechos verificados (inventario del 2026-10-20)

**App (`develop`, 7ae60039)**
- Detección offline: `ObserveOfflineRegistrationModeUseCase` emite `!conectado || !servidorAlcanzable`. Usa `NetworkConnectivityMonitorImpl` y `MikrotikReachabilityMonitorImpl` (GET a `subscription/provisioning-ready`, que ninguna otra parte usa). `RegisterSubscriptionUseCase.canRegisterOnline` repite la comprobación.
- Cola: Room `ispadmin.db`, versión 1, sin migraciones ni `fallbackToDestructiveMigration`; tabla `pending_subscriptions` más 6 tablas `catalog_*`. Las fotos viven en `filesDir/pending_subscriptions/<localId>.jpg`. Una preautorización (`registrationOperationId`) nunca se encola.
- **La sincronización es solo manual:** no hay worker, servicio ni reintento; solo corre al pulsar «Enviar ahora» en `PendingSubscriptionsViewModel`. Reenvía `POST subscription/with-facade-photo` y consulta `registration-progress`.
- UI exclusiva: `subscription/pending/*` (pantalla, ViewModel, estado), ruta `Subscription.PendingSubscriptions`, entrada «Pendientes de envío» en Más (`MainNavigation`, `MoreMenu`, `MoreRoute`), banner de pendientes en el inicio del técnico (`FieldModel`, `FieldContent`, `FieldViewModels`), el campo «IP del cliente» (solo con `requiresClientIpAddress`) y el diálogo `QueuedOfflineDialog`.
- **La caché de catálogo no es solo offline:** `RegisterSubscriptionComposeViewModel.applyInitialCatalogData` obtiene de ella planes, lugares, cajas NAP, ONU y equipos del asistente en línea (`RegistrationCatalogRepositoryImpl.refreshFromRemote` guarda en Room y `getCachedCatalog` lee de Room).
- Room solo se usa aquí. WorkManager y `ACCESS_NETWORK_STATE` se usan también por observabilidad y NO se tocan.

**Backend (`feature/module-switches`, ca0a45a)**
- No hay endpoint, tabla ni bandera exclusivos de offline. Los endpoints que usa la cola (`POST /subscription/with-facade-photo`, `GET /subscription/{id}/registration-progress`) sirven también al registro en línea.
- Exclusivo de offline: `SubscriptionIpConflictNocNotifier` (alerta WhatsApp «Colisión IP sync offline», usada solo por `SubscriptionController`) y su llamada al clasificar el 409 `IP_CONFLICT`.
- Compartido con el registro en línea y por tanto se conserva: `clientRequestId` con su índice único (preautorización de fibra, reenvío tras timeout), `reconcile`/`reconcileDue` y las columnas de aprovisionamiento, `IpAllocationService.allocate(preferredIp)`, la rama del multipart que devuelve la suscripción existente sin volver a subir la foto, y la clasificación del 409.
- No hay versión mínima del lado del servidor: `GET /app/check_version` devuelve la primera fila de `AppVersion` y la app decide. La app bloquea con diálogo si `versionCode` remoto > `BuildConfig.VERSION_CODE`, pero si la comprobación falla (por ejemplo sin red) deja pasar.

## 3. Fuera de alcance (no se elimina)

- ONU offline en NetDiag y ServiceHealth; asistencia facial y su sincronización offline (`ispadmin-asistencias`, `FaceDataController`, `FaceVerifyController`, `OfflineAttendanceSyncBody`); la cola offline de observabilidad (`ObservabilityQueue`, `ObservabilityFlushWorker`).
- Preautorización de ONU, aprovisionamiento, `PollRegistrationProgressUseCase`, `RegistrationConflictException`/`REGISTRATION_ALREADY_EXISTS`, `clientRequestId` y todo el registro en línea.
- Tablas, columnas y migraciones del backend (`client_request_id`, columnas de aprovisionamiento).
- Cambios en el backoffice o el Gateway (no usan estos endpoints).
- Un versionado mínimo del lado del servidor (no existe y no se crea aquí).

## 4. Entrega 1 · el registro exige conexión (solo app, sin cambios de backend)

### 4.1 Registro en línea
- `RegisterSubscriptionUseCase` siempre registra en línea; se eliminan `canRegisterOnline` y `enqueueOffline`.
- Si el envío falla por red (`IOException`), se conserva el flujo existente de resultado incierto: `SubmitAndTrackRegistrationUseCase` devuelve `Uncertain`, el asistente muestra «No se pudo confirmar si el registro quedó guardado. Puedes reintentar: no se duplicará.» y vuelve a mostrar «Registrar»; el reintento reutiliza el mismo `clientRequestId` (guardado en el estado de la pantalla), de modo que un reenvío no duplica el alta (idempotencia del backend). No se añade un mensaje nuevo.
- Se eliminan: `RegisterSubscriptionResult.QueuedOffline`, `RegistrationSubmission.Queued`, `RegisterSubscriptionUiEvent.QueuedOffline` y su diálogo, `EnqueuePendingSubscriptionUseCase`, `isOfflineMode`, `requiresClientIpAddress`, `FormFieldKey.CLIENT_IP_ADDRESS`, `RegisterSubscriptionIntent.ClientIpAddressChanged`, la etiqueta de prueba `CLIENT_IP`, `ClientIpAddressValidation`, la fila «IP del cliente» de la revisión y la rama de IP de `wizardFieldsFor`.
- `withOfflineState` y `deviceOffline` del ViewModel desaparecen con lo anterior.
- Los campos `clientIpAddress` e `ip` del modelo `Subscription` dejan de llenarse desde el formulario (se envían nulos; el backend ya acepta el campo ausente). Los campos se quitan del modelo, si procede, en la entrega 2.
- Se eliminan los monitores de conectividad y el ping: `ObserveOfflineRegistrationModeUseCase`, `NetworkConnectivityMonitor`/`Impl` (ver Desviaciones: se conserva hasta la entrega 2) y `MikrotikReachabilityMonitor`/`Impl`, con su cableado en Koin y los parámetros del constructor de `RegisterSubscriptionUseCase` y del ViewModel.

### 4.2 Catálogo directo
- `GetRegistrationCatalogUseCase()` pasa a devolver `Result<RegistrationCatalog>` pidiendo a la API los cinco endpoints existentes (`plan`, `place`, `napbox`, `onu/unconfigured_onus`, `networkDevice/coreTypes`), sin persistir. Se conserva que los DTO de equipos de core blanqueen usuario y clave.
- `RefreshRegistrationCatalogUseCase` se elimina y `applyInitialCatalogData` llama una sola vez al caso de uso. `CatalogNotAvailableOffline` se elimina.
- Si falla la carga, el asistente muestra el error de carga inicial y no abre el formulario. Cuando la causa es de red (`IOException`, también como causa encadenada) el mensaje es «No se pudo cargar el catálogo de registro. Revisa tu conexión y vuelve a intentar.»; cualquier otro fallo conserva su mensaje. A observabilidad se reporta siempre la excepción original. No hay botón de reintento (decisión del usuario para más adelante).
- Las clases `catalog_*` de Room (entidades y DAOs) NO se tocan en esta entrega, para no cambiar el esquema de la base ni arriesgar la cola; quedan sin uso con un comentario que remite a la entrega 2.

### 4.3 Cola existente: solo vaciarla
- Se conservan `PendingSubscriptionsScreen`/`ViewModel`, la sincronización manual y el descarte, para enviar o descartar lo que ya esté en cola.
- La entrada «Pendientes de envío» en Más (también la del rol técnico) y el banner del inicio del técnico se muestran solo si hay al menos un pendiente.
- Se corrigen los textos que prometen envío automático («Se envían solos al volver la señal», «se envían al volver la conexión»): pasan a decir que se envían al pulsar «Enviar ahora».

### 4.4 Telemetría para decidir la entrega 2
- Una vez por proceso, la app envía a observabilidad el evento `pending_subscriptions.count` con el número de pendientes. Se envía al iniciar la actividad con una sesión existente y, tras un login nuevo, al iniciar la sesión (una vez por proceso; `onSessionStarted` de `MainActivity`, porque ese flujo no recrea la Activity); la guarda por proceso evita el doble envío. Un fallo al leer la cola nunca cierra la app: no se envía nada y se reintenta en la siguiente oportunidad. Se documenta en `observabilidad-taxonomia-eventos.md`.

### 4.5 Lo que no cambia
Backend; esquema de Room; WorkManager y `ACCESS_NETWORK_STATE`; preautorización; `SubmitAndTrackRegistrationUseCase` (la reconciliación por `registrationOperationId` sigue igual); verificación de versión.

## 5. Entrega 2 · borrar lo que quedó (app y backend)

### 5.1 Condición de arranque (la decide el usuario, no el código)
1. La entrega 1 está publicada y su `versionCode` es el mínimo exigido: se sube el `versionCode` de la fila `AppVersion` (operación del usuario en la base).
2. La telemetría de 4.4 no muestra pendientes (o el usuario confirma que no los hay).

### 5.2 App
- Se borran: pantalla, ViewModel y estado de pendientes; la ruta y `FieldAction.PendingUploads`; la entrada de menú y el banner; `PendingSubscription*` del dominio y datos; `PendingSubscriptionRepository`; `SyncPendingSubscriptionsUseCase`, `DiscardPendingSubscriptionUseCase`, `ObservePendingSubscriptionsUseCase`; `SubscriptionSyncRemote`/`PendingSubscriptionSyncApi`; `PendingFacadePhotoStorage`; Room completo (`IspAdminDatabase`, entidades y DAOs de pendientes y de catálogo); `SubscriptionRegistrationRequest` (código muerto).
- Conectividad: `NetworkConnectivityMonitor` (puerto en `domain`), `NetworkConnectivityMonitorImpl` y su prueba, el binding en `DataModule` y la excepción `NoConnectivity` (solo `SyncPendingSubscriptionsUseCase` los usa; hoy figuran únicamente en §9).
- Telemetría: `PendingSubscriptionsTelemetry` (clase y prueba), su `factory` en `ViewModelModule`, la llamada desde `MainActivityViewModel` y desde `onSessionStarted` de `MainActivity`, y su sección en `observabilidad-taxonomia-eventos.md`.
- Navegación y menú: `MainNavigation.moreItemsFor(type, pendingCount)` (la sobrecarga con conteo), el parámetro `pending` de `MoreMenu.from` y el cálculo del conteo en `MoreRoute` (la entrada «Pendientes de envío» desaparece con la función).
- Inicio del técnico: `TechnicianToday.uploads`, `UploadBanner` y el composable `UploadsBanner`, `FieldGateway.pendingUploads` y la dependencia `pending` (`PendingSubscriptionRepository`) de `RepositoryFieldGateway`.
- Fotos: el puerto `FacadePhotoStorage` y su binding en `DataModule` (junto con `PendingFacadePhotoStorage`).
- Modelo: los campos `clientIpAddress` e `ip` de `Subscription` y su lectura en el análisis del JSON de `PendingSubscriptionsViewModel` (el detalle «IP …» de cada pendiente).
- Limpieza de arranque única: `deleteDatabase("ispadmin.db")` y borrado de `filesDir/pending_subscriptions`, con el mismo patrón que el borrado de la base de Chucker en `KoinApplication.onCreate`.
- Dependencias: Room y el plugin `ksp` solo se quitan si una búsqueda confirma que nada más los usa; `room-testing`, `robolectric` y `mockwebserver` solo si las pruebas restantes de `data` no los usan; WorkManager y `ACCESS_NETWORK_STATE` se conservan.
- Documentación: se actualizan los ejemplos canónicos de `AGENTS.md` que citan `PendingSubscriptions*`, y los documentos `.agent-docs` de offline se marcan como históricos o se borran (`offline-ip-conflict-sync-2026-08-15.md`, `deploy-app-distribution-offline-sync-2026-08-15.md`, `deploy-develop-offline-provision-2026-08-15.md`, los tres `backend-*.patch`).

### 5.3 Backend
- Se elimina `SubscriptionIpConflictNocNotifier` (clase, prueba y propiedades que solo usaba esa alerta, conservando las de NetDiag) y su llamada en `SubscriptionController`. La respuesta 409 con `errorCode = IP_CONFLICT` se mantiene.
- Se conserva todo lo compartido listado en §2, incluida la rama del multipart.
- Se retiran de `.agent-docs` y `local-docs` los documentos exclusivos del offline (`deploy-prod-offline-subscription-2026-08-15.md` y su copia; el resto solo si describe únicamente el offline) y se renombran los datos de prueba «offline-req-*».

## 6. Pruebas

**Entrega 1:** caso de uso sin cola; ViewModel sin estado offline y con error de red + reintento con el mismo `clientRequestId`; carga directa del catálogo (éxito, error de red, equipos de core sin credenciales); la pantalla de pendientes se conserva con sus pruebas; entradas de menú y banner ocultos con 0 pendientes y visibles con 1 o más; evento `pending_subscriptions.count`. Se ajustan las pruebas listadas en el inventario (`RegisterSubscriptionUseCaseTest`, `RegisterSubscriptionComposeViewModelTest`, `RegisterExistingCustomerViewModelTest`, `SubmitAndTrackRegistrationUseCaseTest`, `MoreScreenUiTest`, `MainNavigationTest`, `FieldViewModelsTest`, `TechnicianTodayModelTest`). Prueba manual en staging con y sin red: registro de Fibra, Solo TV e Inalámbrico, y envío de un pendiente ya encolado.

**Entrega 2:** prueba de la limpieza de arranque; las pruebas de `data` que dependían de Room se eliminan con su código; en el backend, `SubscriptionControllerMultipartIdempotencyTest` pierde solo el caso de la alerta y se conserva el resto; se ejecuta la suite de `core` afectada.

## 7. Riesgos

- **Pérdida de registros en cola:** mitigada por el orden de las entregas y la condición de arranque. Un teléfono que nunca abra la entrega 1 puede conservar pendientes: por eso la condición pide telemetría o confirmación.
- **Carga inicial del asistente:** la entrega 1 cambia de dónde sale el catálogo. Mitigación: pruebas del caso de uso y prueba manual del asistente completo con y sin red.
- **Verificación de versión que se salta sin conexión:** no garantiza que todos actualicen; se mitiga con la telemetría.
- **Cableado posicional de Koin** del ViewModel de registro: los cambios de constructor se hacen junto con `ViewModelModule.kt` y las pruebas.
- **Sin conexión el técnico no puede registrar:** es el comportamiento pedido; el error es explícito y reintentable.

## 8. Despliegue y reversión

- Entrega 1: nueva versión de la app; el backend no cambia. Reversión: reinstalar la versión previa (la cola local no se toca).
- Entrega 2: nueva versión de la app más un despliegue de backend independiente (el backend no depende de la versión de la app porque solo se retira una alerta). Reversión del backend: revertir el commit; de la app: reinstalar la versión previa (la limpieza de arranque es irreversible, por eso solo se ejecuta cuando la condición de 5.1 se cumple).
- Ningún despliegue, push ni cambio en la base de producción se hace sin petición explícita.

## 9. Desviaciones respecto al spec (entrega 1, verificadas en el código)

- **`NetworkConnectivityMonitor` se conserva** (interfaz, `NetworkConnectivityMonitorImpl`, su prueba y el binding en `DataModule`): `SyncPendingSubscriptionsUseCase` todavía lo usa para el envío manual de pendientes. Se elimina junto con la cola en la entrega 2. Sí se eliminaron `MikrotikReachabilityMonitor`/`Impl`, `ObserveOfflineRegistrationModeUseCase` y su cableado.
- **`GetRegistrationCatalogUseCase` ya no rechaza un catálogo sin equipos de core**: solo delega en `RegistrationCatalogRepository.fetchCatalog()`. Esa validación la hace el ViewModel con `NO_ACTIVE_CORE_DEVICES_ERROR`: solo deja continuar sin equipos core si hay una preautorización de ONU activa o restaurada; si no, muestra el error de carga inicial y no abre el formulario. `RegistrationCatalog.isAvailableOffline()` también se eliminó.
- **`CatalogMapper.kt` se borró**: solo lo usaba el repositorio anterior, que leía y guardaba el catálogo en Room.
- **Textos de la cola, tal como quedaron**: banner superior de la pantalla de pendientes (solo se muestra con pendientes) «Envíalos con “Enviar ahora” cuando tengas señal»; nota final de esa pantalla «Ya no se guardan registros sin señal: aquí solo quedan los que se guardaron antes.»; banner del inicio del técnico «N registro(s) sin enviar · toca para enviarlos», que ya no existe con 0 pendientes (se quitó el estado «Todo enviado»).
- **La guarda de telemetría se libera sola** si la cola no se puede leer o si la corrutina se cancela antes de enviar, de modo que se reintenta en la siguiente apertura del proceso.
