# Alta FIBER v2 — progreso en Android

Estado: integración parcial. No habilita por sí sola el aprovisionamiento remoto.

Fuente de verdad del flujo completo: `../ispadmin-backend/.agent-docs/alta-v2-omci-tr069-runbook.md` desde la raíz del repo.

## Implementado

- Puerto `ProvisioningRepository` en dominio y adaptador Retrofit/Gson en data.
- Consulta, reintento y cancelación exclusivamente mediante Core, con ID de operación y revisión recibidos del servidor.
- Pantalla accesible desde el detalle de una suscripción FIBER. Estado UDF, polling solo en estados activos, confirmación antes de cancelar y nueva alta solo cuando el servidor informa `canStartAgain`.
- Salir cancela las consultas/acciones locales y limpia los indicadores transitorios. No equivale a cancelar el trabajo remoto: al volver se consulta su estado real.
- La ruta de progreso se reconoce en el contenedor de navegación y no se clasifica como Inicio.

## Verificación

Se observó RED y después GREEN en la regresión de salir durante un reintento y en la clasificación de la ruta.

```bash
./gradlew :presentation:compileDevDebugKotlin :presentation:testDevDebugUnitTest --tests '*Provisioning*Test' :data:testDebugUnitTest --tests '*ProvisioningRepositoryTest' --console=plain
```

Resultado: PASS. Pruebas locales de estado, pantalla, navegación y contrato HTTP; sin dispositivos de red ni despliegues.

## Pendientes

- Integrar el progreso inmediatamente después del alta, no solo desde el detalle.
- Mostrar historial completo y acceso a observabilidad.
- Completar y conectar el flujo runtime v2 en backend antes de pruebas reales. Los botones existentes no demuestran que las etapas remotas estén implementadas.
- Pedir confirmación al usuario antes de iniciar la prueba de laboratorio sobre VSOL; ZTE se conecta después.
