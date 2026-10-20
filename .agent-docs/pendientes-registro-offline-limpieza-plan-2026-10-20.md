# Limpieza de pendientes del registro sin ONU y de la entrega 1 offline · Plan

> **Para quien ejecute el plan:** usar `superpowers:subagent-driven-development`. Los pasos usan casillas `- [ ]`.

**Meta:** cerrar todos los hallazgos menores diferidos de las revisiones del registro sin ONU y de la entrega 1 de la eliminación offline, añadir el botón «Reintentar» a la carga fallida del catálogo y corregir el fallo latente de `PlanResponse.equals`, sin cambiar el comportamiento del flujo de fibra.

**Contexto:** parte de `develop` (6a4854c5). Specs vigentes: `.agent-docs/registro-sin-onu-tipo-servicio-2026-10-20.md` y `.agent-docs/eliminar-soporte-offline-registro-2026-10-20.md`. Cada ítem proviene de una revisión previa y es pequeño y localizado.

Rutas abreviadas: `PRES` = `presentation/src/main/java/com/dscorp/ispadmin`, `PREST` = `presentation/src/test/java/com/dscorp/ispadmin`, `REG` = `PRES/presentation/ui/features/subscription/register`, `REGT` = `PREST/presentation/ui/features/subscription/register`, `PEND` = `PRES/presentation/ui/features/subscription/pending`. Todo se ejecuta desde la raíz del worktree.

## Restricciones globales

- Textos visibles en español, sentence case, sin signos de cierre en etiquetas; código y comentarios en inglés conciso; commits en inglés (uno por tarea) con la línea `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`; sin push.
- Sin cambios de backend, del esquema de Room (`IspAdminDatabase` versión 1, entidades y DAOs), de WorkManager ni de `ACCESS_NETWORK_STATE`. El flujo de fibra con preautorización no cambia de comportamiento.
- Pruebas: `./gradlew :presentation:testDevDebugUnitTest --tests "<patrón>"`, `./gradlew :domain:test`, `./gradlew :data:testDebugUnitTest`; compilar `:presentation:compileDevDebugAndroidTestKotlin` al final de cada tarea que toque `androidTest`.

## Fuera de esta ola (decisión explícita)

- Registrar fallos de los endpoints opcionales del catálogo (ONU, equipos de core): exige conectar observabilidad con el módulo `:data`; el síntoma ya se ve («No hay routers core disponibles»).
- La prueba del ViewModel que fije «sin foto» con preautorización: ya la fija `RegisterSubscriptionUseCaseTest`.
- La tarjeta «Equipo» con una serie de ONU guardada en un tipo no fibra: inalcanzable (la guarda se borra al cancelar la fibra).
- Mostrar la entrada de pendientes cuando falla la lectura de Room, y el banner del técnico desactualizado tras una recarga fallida: casos improbables e inofensivos.
- `GetRegistrationCatalogUseCase` como paso directo: lo exige el plan de la entrega 1.

---

### Tarea 1: Paso de tipo de servicio y cabecera

**Archivos:**
- Modificar: `REG/compose/RegisterSubscriptionComposeViewModel.kt`, `REG/compose/RegisterWizardChrome.kt`, `REG/compose/RegisterServiceTypeStep.kt`, `REG/compose/RegisterServiceStep.kt`, `REG/compose/RegisterSubscriptionForm.kt`, `presentation/src/androidTest/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/FiberRegisterFirstOnuE2ETest.kt`
- Probar: `REGT/compose/RegisterSubscriptionComposeViewModelTest.kt`, `REGT/compose/RegisterServiceTypeStepTest.kt`, `REGT/compose/RegisterWizardChromeTest.kt`, `REGT/compose/RegisterServiceStepTest.kt`

- [ ] **Paso 1: escribir o ajustar las pruebas que fallan.**
  - ViewModel: `ServiceTypeSelected is ignored outside the type step` (con el asistente en `CLIENT_LOCATION` el intent no cambia `installationType` ni los planes) y `ServiceTypeSelected is ignored while a preauthorization operation exists`; `a type without active plans does not let Continue advance` (tras el error, `WizardContinueClicked` mantiene `SERVICE_TYPE`).
  - `RegisterServiceTypeStepTest`: «Cancelar» invoca el callback de salida (verificar con un contador), el pie de Inalámbrico dice «Elegido: Inalámbrico · sin ONU», el grupo de tarjetas se anuncia como grupo de selección (semántica `selectableGroup`), y `closing the type step never starts an OLT cancellation` ahora también comprueba que la salida SÍ ocurrió (contador de salida > 0).
  - `RegisterWizardChromeTest`: `closing without preauthorization leaves the screen` comprueba la salida, no solo la ausencia de `CancelOnuRegistration`; un estado en `REGISTERING` sin ONU (Solo TV) no muestra el botón de cancelar de la OLT.
  - `RegisterServiceStepTest`: la fila de solo lectura de Solo TV muestra «Solo TV · receptor CATV» y se lee como un solo elemento (descendientes fusionados).
- [ ] **Paso 2: ejecutar y ver que fallan**: `./gradlew :presentation:testDevDebugUnitTest --tests "*RegisterServiceTypeStepTest" --tests "*RegisterWizardChromeTest" --tests "*RegisterServiceStepTest" --tests "*RegisterSubscriptionComposeViewModelTest"`.
- [ ] **Paso 3: implementar.**
  - `onServiceTypeSelected` (ViewModel) ignora el intent si `wizardStep != SERVICE_TYPE` o si hay `preauthorizationOperation`.
  - `RegisterWizardHeader`: reemplazar el `return@Column` por un `if (!choosingType) { … }` con el mismo contenido.
  - `RegisterServiceTypeStep.kt`: la columna de tarjetas con `Modifier.selectableGroup()`; en cada tarjeta recortar con la forma antes de `selectable` para que el ripple respete las esquinas; `serviceTypeTitle()` pasa a un `when` exhaustivo sobre `InstallationType` (sin `first {}`); el pie «Elegido: …» se dibuja con la línea de estado correcta (`ok = true`) siempre que la prueba de texto exacto siga pasando.
  - `RegisterServiceStep.kt`: la fila de solo lectura del tipo con `semantics(mergeDescendants = true)`.
  - `FiberRegisterFirstOnuE2ETest.kt`: cuando el paso de tipo se omite porque se recuperó una ONU u operación, escribir una línea de registro (`Log.i`) para que una regresión que arranque en `ONU_SELECTION` sin recuperación no pase en silencio.
- [ ] **Paso 4: ejecutar y ver que pasan** los mismos comandos; luego `./gradlew :presentation:testDevDebugUnitTest --tests "*register*"` y `./gradlew :presentation:compileDevDebugAndroidTestKotlin`.
- [ ] **Paso 5: commit**: `fix(register): harden the service type step, header and their tests`.

---

### Tarea 2: Cola de pendientes, telemetría y pruebas de la entrega 1

**Archivos:**
- Modificar: `PEND/PendingSubscriptionsUiState.kt`, `PEND/PendingSubscriptionsScreen.kt`, `PRES/presentation/ui/features/more/MoreMenu.kt`, `PRES/presentation/ui/features/more/MoreRoute.kt`, `PRES/navigation/MainNavigation.kt`, `PEND/PendingSubscriptionsTelemetry.kt`, `PRES/presentation/ui/features/main/MainActivityViewModel.kt`, `.agent-docs/eliminar-soporte-offline-registro-2026-10-20.md`, `AGENTS.md`, `.agent-docs/observabilidad-taxonomia-eventos.md`
- Probar: `PREST/presentation/ui/features/subscription/pending/PendingSubscriptionsScreenTest.kt`, `PREST/presentation/ui/features/subscription/pending/PendingSubscriptionsTelemetryTest.kt`, `PREST/presentation/ui/features/more/MoreScreenUiTest.kt`, `PREST/navigation/MainNavigationTest.kt`, `REGT/compose/RegisterSubscriptionComposeViewModelTest.kt`, `REGT/compose/RegisterSubscriptionFormHostDeviceTest.kt`, `data/src/test/java/com/dscorp/ispadmin/data/repository/RegistrationCatalogRepositoryTest.kt`

- [ ] **Paso 1: escribir o ajustar las pruebas que fallan.**
  - `PendingSubscriptionsScreenTest`: el estado «Esperando señal» pasa a «Por enviar» (cambiar la aserción y los textos de la lista); con la lista vacía no existe la nota final «Ya no se guardan registros sin señal…» y con pendientes sí existe.
  - `MoreScreenUiTest` / `MainNavigationTest`: la regla «pendientes > 0» se prueba en un único lugar (decidir cuál: `MainNavigation.moreItemsFor`) y las pruebas de `MoreMenu.from` dejan de duplicarla.
  - `PendingSubscriptionsTelemetryTest`: `a cancelled collection releases the guard so the next process start retries` (la corrutina se cancela durante `first()`; después una llamada sana reporta una vez).
  - `RegistrationCatalogRepositoryTest`: `cancellation in the onu fetch is rethrown` y `cancellation in the core devices fetch is rethrown`.
  - ViewModel: el test `a network failure on submit keeps the form in the uncertain state…` también verifica el texto «No se pudo confirmar si el registro quedó guardado. Puedes reintentar: no se duplicará.» y que se emite `RegisterSubscriptionUiEvent.Error` con él.
  - `RegisterSubscriptionFormHostDeviceTest`: eliminar las aserciones `doesNotContain("CLIENT_IP")` y `doesNotContain("requiresClientIpAddress")` sobre el código fuente.
- [ ] **Paso 2: ejecutar y ver que fallan** los archivos de prueba anteriores con `--tests` (esperado: FAIL en los ajustados).
- [ ] **Paso 3: implementar.**
  - Etiqueta «Esperando señal» → «Por enviar» (`PendingSubscriptionsUiState.kt`); la nota final de `PendingSubscriptionsScreen.kt` solo se compone cuando `items.isNotEmpty()`.
  - Dejar la regla «pendientes > 0» solo en `MainNavigation.moreItemsFor`; `MoreMenu.from` recibe la lista ya filtrada y mantiene únicamente la insignia con el conteo.
  - `MoreRoute.kt`: la clave de `produceState` de los planes activos pasa de la lista de ítems a `type`, para que cruzar el conteo por cero no reinicie la petición.
  - `PendingSubscriptionsTelemetry.resetForTest()` con `@VisibleForTesting`; `MainActivityViewModel` usa el `user` ya leído en lugar de volver a llamar a `getCurrentUser()`.
  - Docs: el evento se envía «al iniciar la actividad con una sesión existente y, tras un login nuevo, al iniciar la sesión (una vez por proceso)» (spec §4.4 y taxonomía); `AGENTS.md`: la fila de Room dice «(tablas `catalog_*` sin uso hasta la entrega 2)»; spec línea ~47: añadir «(ver Desviaciones: se conserva hasta la entrega 2)».
  - Mover el `@OptIn(ExperimentalCoroutinesApi::class)` de `RegisterSubscriptionComposeViewModelTest.kt` de nuevo a la clase (hoy aplica a una constante).
- [ ] **Paso 4: ejecutar y ver que pasan** los mismos comandos, más `./gradlew :data:testDebugUnitTest` y `./gradlew :presentation:testDevDebugUnitTest --tests "*register*" --tests "*Pending*" --tests "*more*" --tests "*navigation*" --tests "*main*"`.
- [ ] **Paso 5: commit**: `fix(pending): label, single pending rule, telemetry test and doc follow-ups`.

---

### Tarea 3: «Reintentar» en la carga del catálogo y `PlanResponse` seguro

**Archivos:**
- Modificar: `REG/models/RegisterSubscriptionState.kt`, `REG/compose/RegisterSubscriptionComposeViewModel.kt`, `REG/compose/RegisterSubscriptionScreen.kt` (o el composable raíz del asistente), `REG/models/RegisterSubscriptionIntent.kt`, `PRES/domain/model/PlanResponse.kt`
- Probar: `REGT/compose/RegisterSubscriptionComposeViewModelTest.kt`, un test de Compose del estado de error (nuevo `REGT/compose/RegisterCatalogErrorTest.kt`), `PREST/domain/model/PlanResponseTest.kt` (nuevo)

**Interfaces:**
- Produce: `RegisterSubscriptionState.catalogLoadError: String? = null`; `RegisterSubscriptionIntent.RetryCatalogLoad`; etiqueta de prueba `RegisterSubscriptionTestTags.CATALOG_RETRY = "register_catalog_retry"`.

- [ ] **Paso 1: escribir las pruebas que fallan.**
  - `PlanResponseTest`: `equals does not throw with null or another type` (`plan.equals(null)` y `plan.equals("x")` dan falso); `plans with the same id are equal and share the hash code`; `plans with different ids are not equal`.
  - ViewModel: `a catalog fetch failure sets catalogLoadError and does not emit a dialog event`; `RetryCatalogLoad clears the error, reloads and opens the form on success`; `a failed reload keeps the error`; `an IOException shows the Spanish connectivity message in catalogLoadError` (reutiliza el texto ya definido: «No se pudo cargar el catálogo de registro. Revisa tu conexión y vuelve a intentar.»); un fallo de carga NO deja al asistente elegir tipo con una lista vacía (mientras `catalogLoadError != null`, `ServiceTypeSelected` no muestra «No hay planes activos…»).
  - `RegisterCatalogErrorTest` (Compose): con `catalogLoadError` no nulo se muestra el mensaje y el botón «Reintentar» (etiqueta `CATALOG_RETRY`); al pulsarlo se envía `RetryCatalogLoad`; con el error nulo no se muestra.
- [ ] **Paso 2: ejecutar y ver que fallan**: `./gradlew :presentation:testDevDebugUnitTest --tests "*PlanResponseTest" --tests "*RegisterCatalogErrorTest" --tests "*RegisterSubscriptionComposeViewModelTest"`.
- [ ] **Paso 3: implementar.**
  - `PlanResponse`: `equals` seguro (`other is PlanResponse && id == other.id`) y `hashCode` coherente basado en `id`.
  - ViewModel: `emitInitialCatalogError` fija `catalogLoadError` con el mensaje (el de conectividad si hay `IOException` en la cadena, el propio del fallo si no) y ya no emite el diálogo `RegisterSubscriptionUiEvent.Error` para ese caso; `loadScreenData` limpia `catalogLoadError` al empezar; `RetryCatalogLoad` llama a `loadScreenData` con los mismos parámetros de la última carga; el reporte a observabilidad con la excepción original se mantiene.
  - UI: mientras `catalogLoadError != null`, el asistente muestra una vista de error centrada (reutilizar el patrón de error con «Reintentar» que ya usa la app, p. ej. el de `SubscriptionFinderScreen` o `CashDayContent`; si no hay un componente compartido, componer uno mínimo con los tokens del tema) en lugar del contenido del paso, y no muestra la barra de navegación del asistente; «Cerrar» sigue saliendo.
- [ ] **Paso 4: ejecutar y ver que pasan** los mismos comandos y `./gradlew :presentation:testDevDebugUnitTest --tests "*register*"`.
- [ ] **Paso 5: commit**: `feat(register): retry button when the catalog fails to load; null-safe PlanResponse.equals`.

---

### Tarea 4: Verificación global y documentación

**Archivos:** `.agent-docs/eliminar-soporte-offline-registro-2026-10-20.md`, `.agent-docs/registro-sin-onu-tipo-servicio-2026-10-20.md`

- [ ] **Paso 1:** `./gradlew :domain:test :data:testDebugUnitTest :presentation:testDevDebugUnitTest --rerun` completo y `./gradlew :presentation:compileDevDebugAndroidTestKotlin`; esperado BUILD SUCCESSFUL sin fallos. Informar los conteos por módulo.
- [ ] **Paso 2:** búsquedas de restos: ninguna referencia a símbolos eliminados; `IspAdminDatabase`, entidades y DAOs sin diff respecto a `develop`; `ACCESS_NETWORK_STATE` y WorkManager sin diff.
- [ ] **Paso 3:** documentar en ambos specs una línea «Pendientes cerrados el 2026-10-20» con la lista de lo hecho en esta ola y la lista de lo decidido fuera (sección «Fuera de esta ola» de este plan).
- [ ] **Paso 4: commit**: `docs(register): record the closed follow-ups`.
