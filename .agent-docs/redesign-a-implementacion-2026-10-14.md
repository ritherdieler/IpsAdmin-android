# Propuesta A: implementación por fases (2026-10-14, con TDD)

Diseño de referencia: lienzo https://claude.ai/artifact/1yfKRpqjG9eEXu3jD4XQwX (64 pantallas, sin vistas ocultas). Rama `feature/redesign-a`, que parte de `473b68f` (arreglos de auditoría) sobre `feature/dashboard-redesign`. Un commit por fase.

## Hecho

| Fase | Qué cambia | Archivos clave | Pruebas |
|---|---|---|---|
| 1 Shell | Sin menú lateral. Barra inferior por rol, flecha atrás en destinos secundarios, títulos por ruta (Planes y Registro de la ONU ya no dicen «ISP Admin»), pantalla «Más», salida con confirmación solo en raíz. Logística y Cliente ven solo «Más». | `navigation/MainNavigation.kt`, `ui/features/main/MainShell.kt`, `ui/features/more/MoreScreen.kt`, `FeatureNavGraph.kt` | `MainNavigationTest`, `MainShellUiTest`, `MoreScreenUiTest`, `MainShellTestTagsTest` |
| 2 Ficha-hub | Cabecera navy con estado, «Debe N meses», deuda y «Cobrar»; acciones rápidas (Llamar, WhatsApp, Pagos, Ticket, Registro ONU); hoja de cobro con recibos impagos preseleccionados, Yape por defecto con «Nombre en Yape», restablecer si queda con menos de 2 recibos. Tocar una tarjeta del buscador abre la ficha. | `subscriptiondetail/hub/*`, `SubscriptionDetailScreen.kt`, `SubscriptionCard.kt` | `SubscriptionHubModelTest`, `SubscriptionHubViewModelTest`, `SubscriptionHubUiTest`, `HubQuickActionsUiTest`, `SubscriptionCardOpenDetailsTest` |
| 3 Caja | El buscador de pagadores pasa a «Caja»: mensaje inicial en vez de «No se encontraron resultados», resultados que abren la ficha, error legible, FAB «Nuevo cobro» (va a Suscripciones). | `payment/payerFinder/FindPayerComposeScreen.kt`, `PayerFinderViewmodel.kt` | `CashScreenTest` |
| 4 Tickets | Un botón principal por estado (Tomar o Cerrar); Reagendar y Cancelar en el menú ⋮; fechas en español («9 oct 2026 · 12:16»). | `supportTicket/list/compose/TicketActions.kt`, `TicketCard.kt` | `TicketActionsTest`; `SupportTicketCancelConfirmationTest` ahora abre el menú antes de «Cancelar» |
| 5 Registro | Tras autorizar la ONU en la OLT, el técnico pasa directo a Cliente y Servicio mientras ACS la confirma. Arriba del formulario se ve «ONU {serial} · conectando con la gestión remota…» y, al confirmar, «Equipo listo» en todos los pasos. Solo «Registrar suscripción» se bloquea mientras ACS no confirma: muestra el motivo junto al botón y reanuda la consulta a ACS. La pantalla de espera queda para la autorización en OLT, fallos y limpieza. | `RegisterSubscriptionComposeViewModel.kt` (`showPreauthorizationOperation`, `saveSubscription`), `RegisterSubscriptionState.acsPending`/`submitBlockedReason`, `RegisterSubscriptionForm.kt` (`AcsStatusBanner`) | 3 nuevas en `RegisterSubscriptionComposeViewModelTest` y 3 en `RegisterSubscriptionFormWizardTest`; 3 existentes pasan de `WAITING_FOR_ACS` a `CLIENT_LOCATION` |
| 6 Más/estados | Confirmación al cerrar sesión. | `profile/ProfileContent.kt` | `ProfileLogoutConfirmationTest` |

Decisiones:
- El ViewModel del hub no usa `IRepository`: depende de `SubscriptionHubGateway` (puerto estrecho), implementado por `RepositorySubscriptionHubGateway` sobre llamadas existentes. Sin endpoints nuevos.
- El cobro de varios meses registra un pago por recibo, del más antiguo al más nuevo; si uno falla se detiene y la hoja muestra el error.
- `DrawerNavigation`, `MenuDrawerContent` y `MainNavTestTags` se eliminaron. Sus pruebas se migraron a `MainNavigationTest` (mismas garantías) y a `MainShellTestTagsTest`.
- Fase 5: cuando ACS confirma con el técnico ya dentro del formulario, no se restaura el borrador ni se cambia de paso; se conserva lo escrito y solo se actualiza la ONU. El borrador se guarda en servidor solo desde `READY_FOR_FORM` (igual que antes), así que lo escrito durante la espera se persiste al confirmar ACS.
- Las e2e instrumentadas (`SubscriptionSearchStagingE2ETest`, `FiberRegisterFirstOnuE2ETest`) y el journey `mk1-payment-lab` navegan ahora con `main_bottom_bar`, `main_nav_subscriptions`, `main_nav_more` y `more_item_register_subscription`. Compilan; no se corrieron en dispositivo.

## Pendiente

- Fase 5: correr `FiberRegisterFirstOnuE2ETest` en dispositivo con OLT real; ahora el wizard llega a Cliente antes de que ACS confirme.
- «Hoy» del técnico, Instalaciones que unen órdenes y tickets de instalación, pestañas Pagos/Red/Historial de la ficha y acciones de ⋮ filtradas por rol (siguen en la tarjeta del buscador).
- Caja del día y desglose por método: no hay endpoint (requiere Core).
- Unificar diálogos/cargas (`MyCustomDialog` ×2, `Loader` → skeleton) en el resto de pantallas.

## Verificación

```bash
./gradlew :presentation:testDevDebugUnitTest :presentation:compileDevDebugAndroidTestKotlin :presentation:compileProdDebugKotlin
```

549 pruebas en verde (línea base 494). No se probó visualmente en emulador.
