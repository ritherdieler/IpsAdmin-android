# Búsqueda de suscripciones por nombre (endpoint /subscription/search)

## Contexto

Se reemplazó la búsqueda por nombre/apellido que consumía `GET subscription/find/nameAndLastName`
(sin paginación, dos campos, sin estado de carga/error visible) por un buscador basado en el
endpoint paginado `GET subscription/search`, respaldado por Meilisearch en el backend.

Solo se migró el filtro **BY_NAME** al nuevo endpoint. Los filtros por documento, IP, fecha y
código siguen usando sus endpoints previos.

## Contrato HTTP consumido

- `GET subscription/search?q=&status=&page=0&size=20`
- Respuesta 200: `{ "items": SubscriptionResponse[], "page": Int, "size": Int, "total": Long, "totalPages": Int }`
- `items` tiene la misma forma que el `SubscriptionResponse` existente; se reutiliza `SubscriptionResponse.toDomain()`.
- `status` opcional (ACTIVE, CUT_OFF, SUSPENDED, CANCELLED). Hoy la app envía `null`.

## Capa de datos y dominio

- `domain/model/PagedResult.kt`: modelo de dominio genérico `{ items, page, size, total, totalPages }`
  con `canLoadMore = page + 1 < totalPages`.
- `domain/model/PagedSubscriptionResponse.kt`: wrapper de red `{ items: List<SubscriptionResponse>, page, size, total, totalPages }`.
- `RestApiServices.searchSubscriptions(q, status, page, size): Response<PagedSubscriptionResponse>`.
- `IRepository.searchSubscriptions(query, status, page, size): PagedResult<SubscriptionResume>`
  implementado en `Repository` mapeando cada item con `toDomain()`.
- `domain/usecase/subscription/SearchSubscriptionsUseCase`: devuelve
  `Result<PagedResult<SubscriptionResume>>` usando `runCatching` (regla UseCase + `Result<T>`).
  Aplica `query.trim()`. Registrado en Koin (`useCaseModule`).

## ViewModel (UDF)

`SubscriptionFinderViewModel`:

- Nueva dependencia `SearchSubscriptionsUseCase` (inyectada por Koin, ver `ViewModelModule`).
- Se eliminó la lógica del bloque `init` (regla del repo). La inicialización se dispara desde la UI
  vía `initialize()` (idempotente): arranca `observeSubscriptions()`, `findSubscription()` y `getPlaces()`.
- `SubscriptionFinderUiState` amplía: `isSearching`, `searchError`, `searchPerformed`,
  `currentPage`, `canLoadMore`.
- El filtro `BY_NAME` combina un único término `q = "$name $lastName".trim()` y llama al UseCase.
  Se mantiene `debounce(500)`.
- Paginación por scroll infinito: `loadNextPage()` hace append de la siguiente página cuando
  `canLoadMore` y el filtro activo es `BY_NAME`.
- `clearSearchError()` limpia el error tras mostrarlo.
- Los demás filtros se movieron a `legacyFind()` conservando su comportamiento previo.

## UI

`SubscriptionFinderScreen` / `SubscriptionFinder` / `SubscriptionList`:

- `BY_NAME` usa un único campo "Nombre o apellido" (antes eran dos campos).
- Spinner mientras `isSearching` (en el campo y en la sección de resultados en la primera carga).
- Snackbar cuando `searchError` (antes el error era silencioso), luego `clearSearchError()`.
- Estado vacío diferenciado: inicial (`searchPerformed == false`) vs sin resultados (`true`).
- Scroll infinito en el `LazyColumn`: al acercarse al final (`LOAD_MORE_THRESHOLD = 3`) se llama a
  `onLoadMore` y se muestra un footer con spinner mientras se cargan más páginas.

## Pruebas (MockK)

- `SearchSubscriptionsUseCaseTest`: éxito, trim del query, vacío y fallo.
- `SubscriptionFinderViewModelTest`: búsqueda ok (agrupada + paginación), error, vacío y append de
  página. Usa `runTest(testDispatcher)` compartiendo el scheduler con `Dispatchers.setMain`.

## Verificación

- `./gradlew :presentation:compileDevDebugSources` → BUILD SUCCESSFUL.
- Tests unitarios nuevos/afectados del módulo `presentation` → verdes.

## Nota / desviación

- Se corrigieron dos tests preexistentes desincronizados
  (`CreateInstallationOrderViewModelTest`, `InstallationOrderListViewModelTest`) que no compilaban
  porque sus ViewModels ya requerían `observabilityClient`; se añadió un `mockk(relaxed = true)`.
  Era necesario para que compilara el source set de tests.
