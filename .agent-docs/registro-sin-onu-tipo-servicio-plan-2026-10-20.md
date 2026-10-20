# Registro sin ONU: paso inicial de tipo de servicio · Plan de implementación

> **Para quien ejecute el plan:** usar `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`. Los pasos usan casillas `- [ ]`.

**Meta:** que el asistente de registro pregunte primero el tipo de servicio y que Solo TV (receptor CATV) e Inalámbrico se registren sin autorizar ninguna ONU.

**Arquitectura:** un paso nuevo `SERVICE_TYPE` al inicio del asistente. Una propiedad calculada `usesPreauthorization` reemplaza a `preauthorizationEnabled` en todo lo que depende de que exista una ONU o una operación de preautorización. Fibra no cambia; los otros tipos saltan los tres pasos de ONU y envían el registro sin `registrationOperationId`. El backend no cambia.

**Tecnologías:** Kotlin, Jetpack Compose, ViewModel con `StateFlow`, JUnit + MockK, Robolectric con Compose test.

**Spec:** `.agent-docs/registro-sin-onu-tipo-servicio-2026-10-20.md` (aprobado). Diseño visual: lienzo «Rediseño IspAdmin Android», página «A · Registro», pantallas `a-reg-00-tipo` y `a-reg-11` a `a-reg-13`.

Rutas abreviadas en este plan: `REG` = `presentation/src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register`, `REGT` = `presentation/src/test/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register`. Todos los comandos se ejecutan desde la raíz del repo `IpsAdmin-android app`.

## Restricciones globales

- Textos visibles en español, en sentence case, sin signos de cierre en etiquetas; código, comentarios y nombres en inglés conciso.
- Los commits en la rama de trabajo están permitidos (el usuario retiró la regla de `AGENTS.md` el 2026-10-20); hacer push solo con petición explícita. Un commit por tarea.
- Fibra conserva exactamente su flujo: `SERVICE_TYPE` → `ONU_SELECTION` → `ONU_CONFIRMATION` → `WAITING_FOR_ACS` → `CLIENT_LOCATION` → `INSTALLATION` → `CONFIRMATION`, con 4 fases de progreso (Equipo, Cliente, Servicio, Revisión).
- Solo TV (CATV) e Inalámbrico: `SERVICE_TYPE` → `CLIENT_LOCATION` → `INSTALLATION` → `CONFIRMATION`, 3 fases (Cliente, Servicio, Revisión); el envío lleva `onu = null`, `vlan = null`, `registrationOperationId = null`.
- Solo TV sigue exigiendo caja NAP y no pide IP en modo offline.
- Sin cambios en backend, backoffice ni Gateway. Sin despliegue ni nueva versión de la app.
- Comando de pruebas: `./gradlew :presentation:testDevDebugUnitTest --tests "<patrón>"` (en Windows `gradlew.bat`). Las pruebas instrumentadas siguen la regla de pruebas largas del `AGENTS.md` de plataforma (consola visible, sin redirigir a un log oculto).

## Review Focus

Casos que el spec implica y que ninguna tarea cubriría por el camino feliz; cada uno tiene su prueba en la tarea indicada.

1. Un operador con una ONU guardada que reabre la app vuelve a `ONU_SELECTION` sin repetir el tipo (Tarea 2).
2. Una operación activa recuperada al abrir (fibra) no muestra `SERVICE_TYPE` (Tarea 2).
3. Volver atrás desde `CLIENT_LOCATION` en el camino sin ONU conserva lo escrito, y en fibra no vuelve a `SERVICE_TYPE` porque la ONU ya está autorizada (Tarea 2).
4. El modo «sin conexión» recibido antes de elegir el tipo se aplica bien después: Fibra no lo usa, Solo TV lo usa sin pedir IP, Inalámbrico lo usa y pide IP (Tarea 2).
5. Cambiar de Solo TV a Inalámbrico tras volver atrás no deja plan, caja NAP ni Wi-Fi del tipo anterior (Tarea 2).

---

### Tarea 1: Modelo del asistente

**Archivos:**
- Modificar: `REG/models/RegisterSubscriptionWizardStep.kt`, `REG/models/RegisterSubscriptionState.kt`, `REG/compose/RegisterWizardChrome.kt` (solo el nombre del parámetro), `REG/compose/RegisterSubscriptionForm.kt` (rama `SERVICE_TYPE -> Unit` provisional en el `when (step)` de contenido)
- Crear: `REGT/models/RegisterSubscriptionStateTest.kt`

**Interfaces:**
- Produce:
  - `RegisterSubscriptionWizardStep.SERVICE_TYPE` como primer valor del enum; `next()` y `previous()` devuelven `null` para él; `wizardFieldsFor(SERVICE_TYPE, form)` devuelve `emptyList()`.
  - `RegisterSubscriptionState.serviceTypeChosen: Boolean = false` y `serviceTypeError: String? = null`.
  - `val RegisterSubscriptionState.usesPreauthorization: Boolean` = `preauthorizationEnabled && (preauthorizationOperation != null || (serviceTypeChosen && registerSubscriptionForm.installationType == InstallationType.FIBER))`.
  - `internal fun registrationProgressPhases(usesPreauthorization: Boolean): List<RegistrationProgressPhase>` (solo cambia el nombre del parámetro; los llamadores actuales siguen pasando `preauthorizationEnabled` hasta la Tarea 3).

- [ ] **Paso 1: escribir las pruebas que fallan** en `RegisterSubscriptionStateTest`:
  - `usesPreauthorization is false before a type is chosen`: `RegisterSubscriptionState(preauthorizationEnabled = true)` → `assertFalse`.
  - `fiber chosen uses preauthorization`: con `serviceTypeChosen = true` e `installationType = FIBER` → `assertTrue`.
  - `solo TV and wireless do not use preauthorization`: con `serviceTypeChosen = true` y `installationType` en `ONLY_TV_FIBER` y en `WIRELESS` → `assertFalse` en ambos.
  - `an active operation always uses preauthorization`: con `preauthorizationOperation = OnuRegistrationOperation(id = "op-1", serial = "ZTEGC8F01A22", subscriptionId = null, phase = "WAITING_FOR_ACS", state = "WAITING", revision = 1)` y tipo `ONLY_TV_FIBER` → `assertTrue`.
  - `SERVICE_TYPE is the first step and has no fields`: `assertEquals(SERVICE_TYPE, RegisterSubscriptionWizardStep.values().first())`, `assertNull(SERVICE_TYPE.next())`, `assertNull(SERVICE_TYPE.previous())` y `assertEquals(emptyList<FormFieldKey>(), wizardFieldsFor(SERVICE_TYPE, RegisterSubscriptionFormState()))`.
  - `progress phases follow the path`: `registrationProgressPhases(true).map { it.label }` es `["Equipo", "Cliente", "Servicio", "Revisión"]` y con `false` es `["Cliente", "Servicio", "Revisión"]`.
- [ ] **Paso 2: ejecutar y ver que falla**: `./gradlew :presentation:testDevDebugUnitTest --tests "*RegisterSubscriptionStateTest"`. Esperado: FAIL por referencias sin resolver.
- [ ] **Paso 3: implementar** lo de Interfaces. Agregar `SERVICE_TYPE` a todos los `when` exhaustivos que indique el compilador (`next`, `previous`, `wizardFieldsFor`); `canAdvanceWizardStep` no cambia. La propiedad calculada va dentro de `RegisterSubscriptionState`.
- [ ] **Paso 4: ejecutar y ver que pasa**: el mismo comando; esperado PASS. Luego `./gradlew :presentation:compileDevDebugKotlin` sin errores.
- [ ] **Paso 5: commit**: `feat(registro): modelo del paso SERVICE_TYPE y usesPreauthorization`.

---

### Tarea 2: Lógica del ViewModel

**Archivos:**
- Modificar: `REG/models/RegisterSubscriptionIntent.kt`, `REG/compose/RegisterSubscriptionComposeViewModel.kt`
- Probar: `REGT/compose/RegisterSubscriptionComposeViewModelTest.kt`

**Interfaces:**
- Consume (Tarea 1): `SERVICE_TYPE`, `serviceTypeChosen`, `serviceTypeError`, `usesPreauthorization`.
- Produce: `data class ServiceTypeSelected(val type: InstallationType) : RegisterSubscriptionIntent`; comportamiento descrito abajo para `WizardContinueClicked` y `WizardBackClicked`.

- [ ] **Paso 1: ajustar las pruebas existentes.** Agregar al archivo de pruebas el helper `private fun RegisterSubscriptionComposeViewModel.chooseFiberService()` que envía `ServiceTypeSelected(InstallationType.FIBER)` y luego `WizardContinueClicked`. Llamarlo justo después de `loadScreenData(null)` + `advanceUntilIdle()` en las pruebas con `viewModelWithPreauthorization(...)` que hoy esperan `ONU_SELECTION` o usan `OnuSelected` (líneas ~292, ~318, ~652, ~930, ~2583 y la de ONU guardada ~270). La prueba de ONU guardada NO lo llama en el segundo ViewModel: debe volver a `ONU_SELECTION` por la restauración.
- [ ] **Paso 2: escribir las pruebas nuevas que fallan**, todas con `viewModelWithPreauthorization(operationUseCase)` y `coEvery { operationUseCase.active() } returns null`, y un catálogo con planes de `FIBER`, `ONLY_TV_FIBER` y `WIRELESS`:
  - `the wizard starts at SERVICE_TYPE and does not continue without a type`: `wizardStep == SERVICE_TYPE`; tras `WizardContinueClicked` sigue en `SERVICE_TYPE`.
  - `fiber continues to ONU_SELECTION`: `ServiceTypeSelected(FIBER)` + continuar → `ONU_SELECTION` y `usesPreauthorization == true`.
  - `solo TV continues to CLIENT_LOCATION with the optical receiver and sends no onu, vlan or operation id`: `ServiceTypeSelected(ONLY_TV_FIBER)` + continuar → `CLIENT_LOCATION`, `tvCpeKind == OPTICAL_RECEIVER`, `usesPreauthorization == false`; completar cliente, caja NAP y foto, `saveSubscription(...)`; en el `coEvery` del caso de uso verificar `installationType == ONLY_TV_FIBER`, `onu == null`, `vlan == null`, `registrationOperationId == null` y que `operationUseCase.start(any())` no se llamó (`coVerify(exactly = 0)`).
  - `wireless continues to CLIENT_LOCATION without preauthorization`: igual con `WIRELESS`, `tvCpeKind == null`.
  - `a type without active plans shows the error and stays`: catálogo sin planes de TV, `ServiceTypeSelected(ONLY_TV_FIBER)` → `serviceTypeError == "No hay planes activos de Solo TV."`, `installationType` sigue `FIBER`, `serviceTypeChosen == false`.
  - `back from CLIENT_LOCATION returns to SERVICE_TYPE only without preauthorization`: en Solo TV, escribir `FirstNameChanged("Lucía")`, avanzar, `WizardBackClicked` → `SERVICE_TYPE` y `firstName == "Lucía"`; en Fibra con operación activa recuperada, `WizardBackClicked` en `CLIENT_LOCATION` deja el paso igual.
  - `changing from solo TV to wireless clears plan, nap box and wifi`: Solo TV, elegir plan y caja NAP, volver, `ServiceTypeSelected(WIRELESS)` → `selectedNapBox == null`, plan automático de `WIRELESS`, campos Wi-Fi vacíos, nombre del cliente intacto.
  - `a stored ONU selection reopens ONU_SELECTION without asking the type`: ya cubierta por la prueba ajustada; añadir `assertTrue(uiState.serviceTypeChosen)`.
  - `an active operation recovered at startup skips SERVICE_TYPE`: `operationUseCase.active()` devuelve una operación `WAITING_FOR_ACS`; tras `loadScreenData(null)` el paso no es `SERVICE_TYPE` y `serviceTypeChosen == true`.
  - `offline mode follows the chosen type`: emitir `true` en `offlineModeFlow` antes de elegir; luego Fibra → `isOfflineMode == false` y `requiresClientIpAddress == false`; Solo TV → `isOfflineMode == true`, `requiresClientIpAddress == false`; Inalámbrico → ambos `true`.
- [ ] **Paso 3: ejecutar y ver que fallan**: `./gradlew :presentation:testDevDebugUnitTest --tests "*RegisterSubscriptionComposeViewModelTest"`. Esperado: FAIL en las pruebas nuevas.
- [ ] **Paso 4: implementar** en `RegisterSubscriptionComposeViewModel.kt`:
  - estado inicial: `wizardStep = SERVICE_TYPE` si `onuRegistrationOperationUseCase != null`;
  - `private fun onServiceTypeSelected(type: InstallationType)`: llama a `onInstallationTypeSelected(type)` (si no hay planes para el tipo, fija `serviceTypeError` con el texto del spec y no cambia nada); si hay planes o el tipo ya era el vigente, fija `serviceTypeChosen = true`, `serviceTypeError = null` y `tvCpeKind = OPTICAL_RECEIVER` solo para `ONLY_TV_FIBER` (`null` para los demás); luego recalcula el estado offline;
  - `onWizardContinueClicked`: rama `SERVICE_TYPE` dentro del bloque `if (current.preauthorizationEnabled)`; sin `serviceTypeChosen` no avanza; con `FIBER` → `ONU_SELECTION`; con otro tipo → `CLIENT_LOCATION`;
  - `onWizardBackClicked`: desde `CLIENT_LOCATION`, si `preauthorizationEnabled && !usesPreauthorization`, → `SERVICE_TYPE`; en los demás casos usa `previous()`;
  - los cuatro bloques que fijan `preauthorizationEnabled = true` al recuperar una operación (~1054, ~1069, ~1089, ~1136) agregan `serviceTypeChosen = true`; `restorePreauthorizationSelection()` fija `serviceTypeChosen = true` y `wizardStep = ONU_SELECTION` cuando restaura una ONU y no hay operación;
  - `onOnuSelected` (~1623) usa `state.usesPreauthorization`;
  - offline: variable `private var deviceOffline = false` actualizada en `observeOfflineMode`, y `private fun withOfflineState(state: RegisterSubscriptionState): RegisterSubscriptionState` con `isOfflineMode = deviceOffline && !state.usesPreauthorization` y `requiresClientIpAddress = deviceOffline && !state.usesPreauthorization && form.installationType != ONLY_TV_FIBER`; se usa en el colector y en `onServiceTypeSelected`.
- [ ] **Paso 5: ejecutar y ver que pasan**: el mismo comando; esperado PASS en toda la clase.
- [ ] **Paso 6: commit**: `feat(registro): elegir tipo de servicio antes de autorizar la ONU`.

---

### Tarea 3: Cabecera, revisión y cancelación según `usesPreauthorization`

**Archivos:**
- Modificar: `REG/compose/RegisterWizardChrome.kt`, `REG/compose/RegisterSubscriptionForm.kt`, `REG/compose/RegisterSubscriptionScreen.kt`, `REG/compose/RegisterReviewStep.kt`
- Probar: `REGT/compose/RegisterWizardChromeTest.kt`, `REGT/compose/RegisterSubscriptionFormWizardTest.kt`

**Interfaces:**
- Consume (Tarea 1): `usesPreauthorization`, `registrationProgressPhases(usesPreauthorization)`.
- Produce: `RegisterWizardHeader(currentStep, usesPreauthorization, onClose)`: en `SERVICE_TYPE` muestra «Antes de empezar» en vez de «N de M» y no dibuja barras ni nombres de pasos.

- [ ] **Paso 1: escribir las pruebas que fallan.**
  - En `RegisterWizardChromeTest`: `the sin-ONU path counts three steps` (estado en `CLIENT_LOCATION` con `preauthorizationEnabled = true`, `serviceTypeChosen = true`, tipo `ONLY_TV_FIBER`) → existen «1 de 3» y la descripción «Paso 1 de 3: Cliente»; `the type step has no counter` (estado en `SERVICE_TYPE`) → existe «Antes de empezar» y no existe «1 de 4»; `closing without preauthorization does not ask for OLT cleanup` (mismo estado sin ONU) → tocar «Cerrar registro» no agrega `CancelOnuRegistration` a `intents`.
  - En `RegisterSubscriptionFormWizardTest`: `the review of solo TV has no equipment card and says it has no ONU` (paso `CONFIRMATION`, `ONLY_TV_FIBER`, `tvCpeKind = OPTICAL_RECEIVER`, plan «Cable basico») → existen «Receptor CATV · sin ONU» y «No se autoriza ninguna ONU.»; no existen «Autorizada en la OLT» ni «Acceso a internet».
- [ ] **Paso 2: ejecutar y ver que fallan**: `./gradlew :presentation:testDevDebugUnitTest --tests "*RegisterWizardChromeTest" --tests "*RegisterSubscriptionFormWizardTest"`. Esperado: FAIL en las pruebas nuevas.
- [ ] **Paso 3: implementar** el cambio de consumidores:
  - `RegisterWizardHeader` y `RegisterSubscriptionForm` (`onClose`, texto de pasos completos, `registrationProgressPhases`) pasan a `formState.usesPreauthorization`; el botón cerrar solo lleva la etiqueta `ONU_REGISTRATION_CANCEL` y solo envía `CancelOnuRegistration` si `usesPreauthorization`;
  - `RegisterSubscriptionScreen.kt` (~417 y ~427): `onCancel` del overlay y mensaje del catálogo con `uiState.usesPreauthorization`;
  - `RegisterReviewStep.kt`: `trailing` y la fila VLAN con `usesPreauthorization`; en la tarjeta «Servicio», fila `ReviewRow("Conexión", "Receptor CATV · sin ONU")` cuando `installationType == ONLY_TV_FIBER && tvCpeKind == OPTICAL_RECEIVER` (antes de la fila «Equipo»); el texto de cierre es «Al registrar se reserva el puerto en la caja NAP y se activa el servicio de TV. No se autoriza ninguna ONU.» para `ONLY_TV_FIBER` y el actual para los demás tipos.
- [ ] **Paso 4: ejecutar y ver que pasan**: el mismo comando; esperado PASS. Luego `./gradlew :presentation:testDevDebugUnitTest --tests "*register*"` para detectar regresiones.
- [ ] **Paso 5: commit**: `feat(registro): cabecera y revisión según usesPreauthorization`.

---

### Tarea 4: Pantalla del tipo de servicio y selector de solo lectura

**Archivos:**
- Crear: `REG/compose/RegisterServiceTypeStep.kt`, `REGT/compose/RegisterServiceTypeStepTest.kt`
- Modificar: `REG/RegisterSubscriptionTestTags.kt`, `REG/compose/RegisterSubscriptionForm.kt`, `REG/compose/RegisterServiceStep.kt`
- Probar además: `REGT/RegisterSubscriptionTestTagsTest.kt`, `REGT/compose/RegisterServiceStepTest.kt`

**Interfaces:**
- Consume: `ServiceTypeSelected` (Tarea 2), `serviceTypeChosen`, `serviceTypeError` (Tarea 1).
- Produce: `internal fun ServiceTypeStepContent(formState: RegisterSubscriptionState, onIntent: (RegisterSubscriptionIntent) -> Unit)`; etiquetas `RegisterSubscriptionTestTags.SERVICE_TYPE_FIBER = "register_service_type_fiber"`, `SERVICE_TYPE_TV = "register_service_type_tv"`, `SERVICE_TYPE_WIRELESS = "register_service_type_wireless"`.

- [ ] **Paso 1: escribir las pruebas que fallan.**
  - `RegisterServiceTypeStepTest` (mismo estilo Robolectric que `RegisterServiceStepTest`): `shows the three options and the title` («¿Qué servicio vas a registrar?», «Fibra con internet», «Solo TV con receptor CATV», «Inalámbrico»); `choosing solo TV sends ServiceTypeSelected` (clic en la etiqueta `SERVICE_TYPE_TV` → `intents` contiene `ServiceTypeSelected(InstallationType.ONLY_TV_FIBER)`); `continue is enabled only after choosing` (con `serviceTypeChosen = false` el botón `WIZARD_CONTINUE` está deshabilitado y el pie dice «Elige un tipo de servicio»; con `true` está habilitado y el pie dice «Elegido: Solo TV con receptor CATV · sin ONU»); `the error is shown under the options` (`serviceTypeError = "No hay planes activos de Solo TV."`).
  - `RegisterServiceStepTest`: el caso `type, plan and access are chosen on the page` pasa a usar `preauthorizationEnabled = false` (parámetro nuevo en `show`), y se agrega `the installation type is read-only after the first step` con `preauthorizationEnabled = true`: existe «Elegido al inicio» y no existe un botón «Inalámbrico».
  - `RegisterSubscriptionTestTagsTest`: agregar las tres etiquetas nuevas a las comprobaciones de unicidad.
- [ ] **Paso 2: ejecutar y ver que fallan**: `./gradlew :presentation:testDevDebugUnitTest --tests "*RegisterServiceTypeStepTest" --tests "*RegisterServiceStepTest" --tests "*RegisterSubscriptionTestTagsTest"`. Esperado: FAIL.
- [ ] **Paso 3: implementar** `ServiceTypeStepContent` con tres tarjetas de radio según `a-reg-00-tipo` (textos: título «¿Qué servicio vas a registrar?», subtítulo «Según el tipo, el asistente sigue pasos distintos.», «Fibra con internet» / «Empiezas autorizando una ONU», «Solo TV con receptor CATV» / «Splitter, sin ONU ni internet», «Inalámbrico» / «Internet por antena, sin ONU», nota «Sin ONU no se autoriza ningún equipo en la OLT. Empiezas por los datos del cliente y son 3 pasos en vez de 4.»); la tarjeta seleccionada solo se marca si `serviceTypeChosen`. El enlace «Ver el camino con fibra» del prototipo no se implementa. Cablear en `RegisterSubscriptionForm.kt`: rama `SERVICE_TYPE` del contenido; `missingFields` (1 si no hay tipo) y `navigationHint` «Elige un tipo de servicio»; pie «Elegido: …» cuando hay tipo; `canContinue = formState.serviceTypeChosen`; en `WizardNavigationBar` nuevo parámetro `onLeave: () -> Unit` y rama `SERVICE_TYPE` con «Cancelar» (llama a `onLeave`, que `RegisterSubscriptionForm` define como `backDispatcher?.onBackPressed()`) y «Continuar» con la etiqueta `WIZARD_CONTINUE`. En `RegisterServiceStep.kt` (~137): si `formState.preauthorizationEnabled` mostrar una fila de solo lectura con el nombre del tipo («Solo TV · receptor CATV» para TV) y «Elegido al inicio», conservando la etiqueta `INSTALLATION_TYPE`; si no, el `InstallationTypeSelector` de hoy.
- [ ] **Paso 4: ejecutar y ver que pasan**: el mismo comando; esperado PASS. Luego `./gradlew :presentation:testDevDebugUnitTest --tests "*register*"`.
- [ ] **Paso 5: commit**: `feat(registro): pantalla de tipo de servicio y tipo de solo lectura`.

---

### Tarea 5: E2E, verificación manual y documentación

**Archivos:**
- Modificar: `presentation/src/androidTest/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/FiberRegisterFirstOnuE2ETest.kt`, `.agent-docs/registro-sin-onu-tipo-servicio-2026-10-20.md`, `.agent-docs/e2e-fiber-espresso.md`

- [ ] **Paso 1: actualizar el E2E de fibra.** Antes de la primera interacción con `RegisterSubscriptionTestTags.ONU_SEARCH`, tocar `SERVICE_TYPE_FIBER` y luego `WIZARD_CONTINUE`; el resto del recorrido queda igual.
- [ ] **Paso 2: compilar las pruebas instrumentadas**: `./gradlew :presentation:compileDevDebugAndroidTestKotlin`; esperado sin errores.
- [ ] **Paso 3: ejecutar el E2E en el laboratorio de staging** según `.agent-docs/e2e-fiber-espresso.md`, en primer plano y con la consola visible; esperado PASS y sin ONU huérfana en el laboratorio. Informar PASS/FAIL, ids y limpieza.
- [ ] **Paso 4: verificación manual en staging** (anotar resultado de cada caso): Solo TV (CATV) hasta el éxito sin ONU ni VLAN; Inalámbrico hasta el éxito; Fibra completo; volver atrás desde Cliente en TV y cambiar a Inalámbrico; cerrar la app en `ONU_SELECTION` con una ONU elegida y reabrir.
- [ ] **Paso 5: documentar.** Cambiar el estado del spec a «implementado» con la fecha y las desviaciones que haya habido; en `e2e-fiber-espresso.md` añadir el paso inicial del tipo.
- [ ] **Paso 6: commit**: `test(registro): E2E de fibra con el paso de tipo de servicio y documentación`.

---

## Autorrevisión

- **Cobertura del spec:** 5.1 → Tareas 1 y 2; 5.2 → Tarea 2; 5.3 → Tareas 2, 3 y 4 (cada consumidor listado tiene su tarea); 5.4 → Tarea 4; 5.5 → pruebas de Tarea 2 (campos nulos); 5.6 → Tarea 2 (`withOfflineState`); 5.7 → ya hecho en el lienzo; sección 6 → pruebas por tarea más Tarea 5.
- **Consistencia de nombres:** `usesPreauthorization`, `serviceTypeChosen`, `serviceTypeError`, `ServiceTypeSelected`, `SERVICE_TYPE` y las tres etiquetas se definen una vez (Tareas 1, 2, 4) y se usan igual en las demás.
- **Dependencias entre tareas:** la Tarea 1 deja la app compilando con una rama provisional; la Tarea 2 no necesita UI; la Tarea 3 depende de 1; la Tarea 4 depende de 1, 2 y 3; la Tarea 5 depende de todas.
