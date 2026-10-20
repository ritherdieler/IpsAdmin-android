# Registro de suscripción sin ONU: paso inicial de tipo de servicio

Fecha: 2026-10-20 · Estado: **implementado en código el 2026-10-20; verificación manual en staging pendiente** · Repo: `IpsAdmin-android app` (solo app; sin cambios de backend).

> **Nota (2026-10-20):** las partes de modo offline de este documento (§5.6 y las líneas que mencionan `requiresClientIpAddress`, `isOfflineMode` u offline) quedaron superadas por `eliminar-soporte-offline-registro-2026-10-20.md`: el registro ya no tiene modo offline ni pide la IP del cliente. El texto original se conserva como histórico.

Diseño visual: lienzo «Rediseño IspAdmin Android», página «A · Registro» (https://claude.ai/artifact/1yfKRpqjG9eEXu3jD4XQwX), pantallas `a-reg-00-tipo`, `a-reg-11-sin-onu-cliente`, `a-reg-12-tv-servicio`, `a-reg-13-sin-onu-revision`, más `a-reg-03-servicio` con el tipo de solo lectura.

## 1. Problema

No se puede registrar una suscripción de **solo TV con splitter** (sin ONU) desde la app. Causa verificada en el código:

- `OnuRegistrationOperationUseCase` se registra siempre en Koin (`domain/.../DomainModule.kt:21`) y se inyecta sin nulos en `FeatureNavGraph.kt:105`. Por tanto `preauthorizationEnabled` es `true` en producción.
- Con eso, `RegisterSubscriptionComposeViewModel` arranca el asistente en `ONU_SELECTION` y «Autorizar y continuar» solo se habilita con una ONU elegida (`RegisterSubscriptionForm.kt`, `canContinue = form.selectedOnu != null`).
- El tipo de servicio (`InstallationTypeSelector`) vive en el paso `INSTALLATION`, que llega **después** de autorizar la ONU en la OLT.
- Sin interfaz que dispare `TvCpeKindSelected`, además, `tvCpeKind` queda en `null` y bloquea el guardado de «Solo TV» (el selector «ONU / CATV» nunca se dibujó).
- El backend ya soporta el caso: `SubscriptionValidatorService` exige ONU solo para `FIBER`; `OnlyTvFiberInstallationStrategy` no crea cola; `SubscriptionProvisionService` marca OLT y TR-069 como `NA` sin ONU; sin `registrationOperationId` el registro sigue el camino clásico (`FiberRegistrationService.handles` solo acepta `FIBER`).
- **Inalámbrico** pasa por el mismo bloqueo (deducido del código; no probado en un teléfono).

## 2. Objetivo y criterios de éxito

Un técnico puede registrar **Solo TV con receptor CATV** (y, por el mismo camino, **Inalámbrico**) sin tocar ninguna ONU.

1. El primer paso del asistente pregunta el tipo de servicio.
2. Fibra conserva exactamente el flujo actual (autorización de ONU, espera de ACS, borrador, cancelación con limpieza en la OLT).
3. Solo TV con CATV e Inalámbrico saltan `ONU_SELECTION`, `ONU_CONFIRMATION` y `WAITING_FOR_ACS`, empiezan en `CLIENT_LOCATION` y envían el registro **sin** `registrationOperationId`, `onu` ni `vlan`.
4. Solo TV sigue exigiendo caja NAP y borne (decisión confirmada); no pide Wi-Fi ni acceso a internet.

## 3. Fuera de alcance

- **Solo TV con ONU** (entrega 2). El backend rechaza la preautorización para tipos distintos de `FIBER` (`ProvisioningV2RegistrationService.validatePreauthorizationTarget`, `PROVISIONING_REQUIRES_FIBER`); hacerlo requiere cambiar aprovisionamiento (VLAN de TV, snapshot, promoción).
- Cambios de backend, del backoffice o del Gateway.
- Pantallas de Progreso y Éxito específicas de TV (se reutilizan; muestran pasos de fibra).
- Borrador remoto para el camino sin ONU (hoy el borrador vive en la operación de preautorización; sin operación no hay borrador, igual que antes de existir la preautorización).
- Despliegue, versión de la app y commits.

## 4. Flujo por tipo

| Tipo elegido | Pasos | Barra de progreso | Operación de preautorización |
|---|---|---|---|
| Fibra | `SERVICE_TYPE` → `ONU_SELECTION` → `ONU_CONFIRMATION` → `WAITING_FOR_ACS` → `CLIENT_LOCATION` → `INSTALLATION` → `CONFIRMATION` | Equipo, Cliente, Servicio, Revisión (4) | Sí |
| Solo TV (CATV) | `SERVICE_TYPE` → `CLIENT_LOCATION` → `INSTALLATION` → `CONFIRMATION` | Cliente, Servicio, Revisión (3) | No |
| Inalámbrico | igual que Solo TV | 3 | No |

`SERVICE_TYPE` no muestra barra de progreso; la cabecera dice «Antes de empezar».

## 5. Diseño técnico

### 5.1 Pasos y estado

- `RegisterSubscriptionWizardStep`: agregar `SERVICE_TYPE` como primer valor. `next()` y `previous()` no cambian para los demás pasos; la transición desde `SERVICE_TYPE` la decide el ViewModel porque depende del tipo. `isPreauthorizationStep()` no incluye `SERVICE_TYPE`.
- `RegisterSubscriptionState`:
  - `preauthorizationEnabled` pasa a significar «la función existe» (sin cambio de valor).
  - Nuevo `serviceTypeChosen: Boolean = false` y `serviceTypeError: String? = null`.
  - Nueva propiedad calculada `usesPreauthorization = preauthorizationEnabled && (preauthorizationOperation != null || (serviceTypeChosen && registerSubscriptionForm.installationType == FIBER))`. Antes de elegir el tipo es `false`, así que cerrar en `SERVICE_TYPE` solo sale de la pantalla.
- Estado inicial: `wizardStep = SERVICE_TYPE` si el caso de uso existe; si no, `CLIENT_LOCATION` como hoy (y el selector de tipo sigue editable en `INSTALLATION`).

### 5.2 ViewModel

- Nuevo intent `ServiceTypeSelected(type)`. Reutiliza `onInstallationTypeSelected(type)` (filtra planes, limpia caja NAP y Wi-Fi) y además:
  - marca `serviceTypeChosen = true`;
  - si el tipo es `ONLY_TV_FIBER`, fija `tvCpeKind = OPTICAL_RECEIVER`; si no, `tvCpeKind = null`;
  - si no hay planes activos para el tipo, no cambia de tipo y fija `serviceTypeError = "No hay planes activos de <tipo>."`.
  - Seleccionar el tipo ya vigente (`FIBER` por defecto) también marca `serviceTypeChosen`.
- `onWizardContinueClicked` agrega la rama `SERVICE_TYPE`: sin tipo elegido no avanza; con `FIBER` va a `ONU_SELECTION`; con otro tipo va a `CLIENT_LOCATION`.
- `onWizardBackClicked`: desde `CLIENT_LOCATION`, si `preauthorizationEnabled && !usesPreauthorization`, vuelve a `SERVICE_TYPE` y mantiene lo escrito. Desde `ONU_SELECTION` no hay atrás (se cancela); en `CLIENT_LOCATION` de fibra sigue sin atrás (la ONU ya está autorizada).
- Los cuatro bloques que hoy hacen `preauthorizationEnabled = true` al recuperar una operación (`RegisterSubscriptionComposeViewModel.kt` ~1054, ~1069, ~1089, ~1136) fijan además `serviceTypeChosen = true` (una operación activa siempre es de fibra y no debe mostrar `SERVICE_TYPE`).
- `restorePreauthorizationSelection()` (serie de ONU guardada del operador, sin operación activa) fija `serviceTypeChosen = true` y `wizardStep = ONU_SELECTION`: quien eligió una ONU antes de salir vuelve a esa pantalla sin repetir el tipo.

### 5.3 Consumidores de `preauthorizationEnabled` que pasan a `usesPreauthorization`

| Lugar | Cambio |
|---|---|
| `RegisterSubscriptionForm.kt` cabecera y botón cerrar | cancelar con limpieza de OLT solo si `usesPreauthorization`; si no, volver atrás |
| `RegisterSubscriptionForm.kt` `canRegister` y fotos | la rama de foto remota solo con operación; el camino sin ONU usa `facadePhotoUri` como hoy |
| `RegisterSubscriptionScreen.kt` ~417 y ~427 | `onCancel` del overlay y `CATALOG` solo con `usesPreauthorization` |
| `RegisterReviewStep.kt` | tarjeta «Equipo» solo con ONU u operación; el camino sin ONU no la muestra |
| `RegisterWizardChrome.kt` `registrationProgressPhases` | parámetro `usesPreauthorization`; el paso `SERVICE_TYPE` no calcula `current` en la barra |
| ViewModel `onOnuSelected` (~1623) | guardar la serie solo con `usesPreauthorization` |
| ViewModel `observeOfflineMode` (~1717) | `isOfflineMode = offline && !usesPreauthorization` |

### 5.4 Interfaz

- Nuevo composable `ServiceTypeStep` (3 tarjetas con radio, nota informativa y enlace «Ver el camino con fibra»); etiquetas de prueba `SERVICE_TYPE_FIBER`, `SERVICE_TYPE_TV`, `SERVICE_TYPE_WIRELESS` en `RegisterSubscriptionTestTags`.
- `RegisterServiceStep`: `InstallationTypeSelector` pasa a una fila de solo lectura («Elegido al inicio») cuando `preauthorizationEnabled`.
- El estado `serviceTypeChosen` habilita «Continuar»; sin tipo, el pie dice «Elige un tipo de servicio».
- Textos nuevos en español, sentence case, sin signos de cierre en etiquetas.

### 5.5 Envío al backend

Sin cambio de contrato. Con el camino sin ONU el modelo `Subscription` sale con `installationType = ONLY_TV_FIBER` (o `WIRELESS`), `onu = null` (porque `requiresOnu()` es falso con CATV), `vlan = null`, `registrationOperationId = null` y `clientRequestId = formClientRequestId()`. `SubmitAndTrackRegistrationUseCase` ya tolera la ausencia de operación (`reconcile` devuelve `null`).

### 5.6 Modo offline

Con el camino sin ONU el modo offline vuelve a aplicar (solo se desactivaba por la preautorización). Sin conexión, la IP del cliente se pide para todos los tipos sin preautorización, igual que en el flujo clásico, porque la cola offline y el backend asignan una IP estática a los tipos que no son fibra: `requiresClientIpAddress = offline && !usesPreauthorization`. Como `usesPreauthorization` cambia al elegir el tipo, el ViewModel guarda el último valor de «sin conexión» (`deviceOffline`) y recalcula `isOfflineMode` y `requiresClientIpAddress` tanto cuando llega un valor nuevo como en `ServiceTypeSelected`.

### 5.7 Pantallas que cambian en el lienzo

`a-reg-03-servicio` (tipo de solo lectura), enlaces «Registrar» de `a-ini-03-tecnico` y `a-ins-01-lista` (ahora abren `a-reg-00-tipo`) y el mapa `A-flujos` (68 pantallas).

## 6. Pruebas

Unitarias (JUnit + MockK, como el resto del módulo):

- `RegisterSubscriptionComposeViewModelTest`: arranque en `SERVICE_TYPE`; Fibra avanza a `ONU_SELECTION`; CATV avanza a `CLIENT_LOCATION`, fija `tvCpeKind = OPTICAL_RECEIVER` y el envío no lleva `onu`, `vlan` ni `registrationOperationId`; Inalámbrico igual; tipo sin planes muestra el error y no avanza; Atrás desde `CLIENT_LOCATION` vuelve a `SERVICE_TYPE` solo en el camino sin ONU; una operación activa recuperada no muestra `SERVICE_TYPE`.
- Sin conexión: Solo TV exige la IP del cliente igual que Inalámbrico (el paso `INSTALLATION` no avanza sin una IP válida); Fibra no la pide.
- Recarga de datos (p. ej. rotación): la lista de planes y el plan elegido se conservan para Solo TV, Inalámbrico y Fibra; un borrador nunca restaura `SERVICE_TYPE`; elegir un tipo sin planes (también el actual) muestra el error y no lo marca como elegido; restaurar una ONU guardada no devuelve el asistente a `ONU_SELECTION` si ya se pasó el paso de tipo; el Atrás del sistema en `INSTALLATION` vuelve a `CLIENT_LOCATION` en el camino sin ONU.
- `RegisterWizardChromeTest`: 4 fases con preautorización, 3 sin ella, `SERVICE_TYPE` sin barra.
- `RegisterServiceStepTest` y `RegisterSubscriptionFormWizardTest`: selector de tipo de solo lectura; revisión sin tarjeta «Equipo» en el camino sin ONU; «Atrás» visible en `CLIENT_LOCATION` solo sin ONU; la revisión de Inalámbrico y de Solo TV con ONU no muestra la fila «Conexión» ni la nota de TV.
- Ajustar los 4 casos existentes que asumen `ONU_SELECTION` como primer paso.
- `RegisterSubscriptionFormValidationTest`: ya cubre CATV y ONU; no cambia.

Instrumentadas: `FiberRegisterFirstOnuE2ETest` debe tocar primero `SERVICE_TYPE_FIBER` y «Continuar». Siguen las reglas de pruebas largas del `AGENTS.md` de plataforma (consola visible, sin redirigir a un log oculto).

Comando de verificación: `gradlew.bat :presentation:testDevDebugUnitTest --tests "*RegisterSubscription*"` y los tests de `RegisterWizardChrome`.

Prueba manual obligatoria antes de proponer versión: en el laboratorio de staging, registrar un Solo TV (CATV) y un Inalámbrico sin ONU, y un Fibra completo para confirmar que no hay regresión.

## Desviaciones respecto al spec

- De los cuatro bloques de recuperación de operación (§5.2), tres fijan `wizardStep = ONU_SELECTION` además de `serviceTypeChosen = true`: el de la intención de cancelación pendiente, el de la suscripción vinculada (provisión) y el de los estados `CANCEL_*`, todos en `restoreActivePreauthorization`. El cuarto, `showPreauthorizationOperation`, fija `serviceTypeChosen = true` pero conserva el paso calculado según la operación (paso actual, `CLIENT_LOCATION`, `WAITING_FOR_ACS` o el del borrador restaurado). Los cuatro aplican `withOfflineState`.
- El botón cerrar de la cabecera en `SERVICE_TYPE` siempre sale de la pantalla; nunca inicia la cancelación de la OLT, aunque ya esté elegida Fibra (nada está autorizado en ese paso).
- Los mensajes de commit de la implementación están en inglés (el plan los proponía en español).
- **Offline y Solo TV (corrige §5.6 original):** el spec original eximía de la IP del cliente a `ONLY_TV_FIBER`. Se revierte: la cola offline (`RegisterSubscriptionUseCase.enqueueOffline`) exige la IP para cualquier tipo y el backend asigna una IP estática a los tipos que no son fibra, así que Solo TV sin conexión pide la IP igual que Inalámbrico. No se tocó el caso de uso ni el backend.
- «Atrás» en `CLIENT_LOCATION` cuando no hay ONU (`preauthorizationEnabled && !usesPreauthorization`), y un `BackHandler` en la pantalla que recorre el asistente hacia atrás (Revisión, Servicio, Cliente, Tipo) en ese camino; desde `SERVICE_TYPE` el Atrás del sistema sale. La X de la cabecera sigue saliendo sin confirmar. No se aplica con el resultado del registro visible ni mientras se registra.
- Al recargar los datos de la pantalla (rotación), `applyInitialCatalogData` filtra los planes por el tipo ya elegido y conserva el plan seleccionado; antes los reemplazaba por los de Fibra.
- `registrationWizardStep()` descarta también `SERVICE_TYPE`, y `restorePreauthorizationSelection` solo fija `ONU_SELECTION` si el tipo aún no se eligió o el paso es `SERVICE_TYPE`.
- La nota final de la revisión de Solo TV («No se autoriza ninguna ONU») solo aparece con receptor CATV.
- El E2E de fibra toca `SERVICE_TYPE_FIBER` y «Continuar» solo si el paso de tipo aparece; una ONU u operación recuperada lo omite.

## 7. Riesgos

- Es el flujo central de altas y toca su máquina de estados. Mitigación: la rama de fibra no cambia de comportamiento y tiene su E2E.
- Un técnico que elige Fibra por error solo puede cancelar y reiniciar en `ONU_SELECTION` (sin «Atrás»). Se acepta para no complicar la cancelación; el mismo patrón ya existe en fibra. En el camino sin ONU sí hay «Atrás» desde `CLIENT_LOCATION` hacia `SERVICE_TYPE`, conservando lo escrito.
- El camino sin ONU no tiene borrador remoto: si el sistema cierra la app a mitad del formulario se pierde lo escrito, como en el flujo anterior a la preautorización.
- Las pantallas de Progreso y Éxito de TV mostrarán pasos de fibra hasta que se rediseñen (fuera de alcance).
- Se sigue cargando la lista de ONU al abrir el asistente aunque el tipo no la use; optimización pendiente, no bloquea.

## 8. Despliegue y reversión

Solo requiere nueva versión de la app (distribución como las anteriores); el backend de producción no cambia. Reversión: reinstalar la versión previa; no hay migraciones ni datos nuevos. No se hace commit ni despliegue sin petición explícita.
