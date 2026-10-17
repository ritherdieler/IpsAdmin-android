# Dashboard financiero Android — 2026-10-09

El dashboard abre «Cobros y egresos». «Facturación y cartera» permite elegir el mes de servicio; inicialmente se usa el último mes completado. El período de movimientos es independiente y determina el corte. Los indicadores comerciales anteriores siguen accesibles, sin los gráficos y conceptos financieros del contrato antiguo.

La feature usa API Retrofit acotada `FinancialApi`, modelos tipados con importes `BigDecimal` y valores no disponibles `null`, repositorio propio y `FinancialViewModel` con constructor, StateFlow, intents y eventos. Koin reutiliza el Retrofit autenticado existente. No añade dependencias, credenciales, variables de entorno ni cambios de versión para distribución.

Se preserva el último corte ante fallo de actualización y se ofrece reintento. Cambiar un período elimina el corte previo para evitar datos mal etiquetados. El XLSX se solicita con el ID del corte mostrado. Android abre el selector de destino del sistema y escribe el archivo en el URI elegido, sin permisos de almacenamiento adicionales. La exportación genera cinco hojas desde el Core y respeta los permisos del usuario.

Cada indicador abre los registros correspondientes del corte. El detalle usa una lista Lazy con claves estables y formatea solamente registros visibles. La pantalla muestra cobertura, revisión, identificador, reconstrucciones históricas, vencimientos sin confirmar, grupos de antigüedad, zona, método, registrador, categoría y centro de costo. El presupuesto se muestra separado de los pagos efectivos y los corporativos separados por moneda.

Contrato y reglas del Core: `../../ispadmin-backend/.agent-docs/reportes-financieros-2026-10-09.md`. ADMIN y ACCOUNTANT acceden al reporte completo; SECRETARY recibe una proyección de cobros y cartera, con datos de egresos/presupuesto/corporativos eliminados por el servidor.

## Verificación

Con TDD: RED funcional confirmado con Gradle antes de implementar el ViewModel, repositorio y pantalla. Ocho pruebas PASS: carga, actualización fallida y reintento, cambio de período, exportación del ID mostrado, contrato tipado y errores HTTP, y acceso a registros en Compose/Robolectric. `:presentation:assembleDevDebug` PASS; `:presentation:compileDevDebugKotlin` PASS. Se mantuvieron las correcciones anteriores de actualización de gráficos y errores del dashboard.

```bash
./gradlew :presentation:testDevDebugUnitTest --tests '*Financial*Test' :presentation:assembleDevDebug
```

APK local: `presentation/build/outputs/apk/dev/debug/`. No se instaló en dispositivo ni se distribuyó. Activar primero el Core con V79; el APK DevDebug utiliza la URL dev ya configurada en el proyecto. No se comprobó el flujo contra producción ni el rendimiento con datos productivos.
