# 🏗️ Diseño y Arquitectura

## 1. Visión general

El servicio sigue una arquitectura en **3 capas** con Spring Boot 3.2.5 y Java 21:

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (SPA)                        │
│              index.html + i18n JSON files                │
├─────────────────────────────────────────────────────────┤
│                  REST API (/api/v1)                      │
│              CarPoolingController                        │
│       DTOs (request) ←──→ DTOs (response)               │
├─────────────────────────────────────────────────────────┤
│                  Capa de Servicio                        │
│              CarPoolingService                           │
│         @Transactional — Lógica de negocio              │
├─────────────────────────────────────────────────────────┤
│                Capa de Persistencia                      │
│        CarRepository   JourneyRepository                │
│              Spring Data JPA                             │
├─────────────────────────────────────────────────────────┤
│                  Base de Datos                           │
│          PostgreSQL (prod) / H2 (test)                   │
└─────────────────────────────────────────────────────────┘
```

## 2. Estructura de paquetes

```
com.carpooling
├── CarPoolingApplication.java          # @SpringBootApplication
├── controller
│   └── CarPoolingController.java       # REST endpoints
├── dto
│   ├── request
│   │   ├── CarRequestDTO.java          # Record con validaciones
│   │   └── JourneyRequestDTO.java      # Record con validaciones
│   └── response
│       ├── CarResponseDTO.java         # Record con factory fromDomain()
│       └── ErrorResponseDTO.java       # Respuesta de error consistente
├── exception
│   ├── GlobalExceptionHandler.java     # @RestControllerAdvice (7 handlers)
│   ├── GroupNotFoundException.java     # → 404
│   └── DuplicateGroupException.java   # → 409
├── model
│   ├── Car.java                        # @Entity — vehículo
│   └── Journey.java                    # @Entity — grupo de viaje
├── repository
│   ├── CarRepository.java              # JpaRepository<Car, Integer>
│   └── JourneyRepository.java          # JpaRepository<Journey, Integer>
└── service
    └── CarPoolingService.java          # Lógica de negocio
```

## 3. Diseño REST API

Base path: `/api/v1`

| Método | Endpoint | Descripción | Request | Response |
|--------|----------|-------------|---------|----------|
| `GET` | `/status` | Health check | — | `200` |
| `PUT` | `/cars` | Cargar flota | `List<CarRequestDTO>` | `200` / `400` |
| `POST` | `/journeys` | Registrar grupo | `JourneyRequestDTO` | `201` / `400` / `409` |
| `DELETE` | `/journeys/{id}` | Dropoff | Path: `id` (positive int) | `204` / `400` / `404` |
| `GET` | `/journeys/{id}/car` | Localizar coche | Path: `id` (positive int) | `200` + `CarResponseDTO` / `204` / `404` |

### Convenciones REST aplicadas
- **Sustantivos plurales** en endpoints (`/cars`, `/journeys`)
- **Verbos HTTP correctos**: GET (leer), POST (crear), PUT (reemplazar), DELETE (eliminar)
- **Versionado** en URL (`/api/v1`)
- **DTOs separados** de las entidades de dominio
- **Códigos HTTP semánticos**: 201 Created, 204 No Content, 400 Bad Request, 404 Not Found, 405 Method Not Allowed, 409 Conflict

## 4. Modelo de datos

```
┌──────────────┐         ┌──────────────────┐
│    cars       │         │    journeys      │
├──────────────┤         ├──────────────────┤
│ id       PK  │◀────────│ id           PK  │
│ seats        │    FK   │ people           │
│ available_   │         │ assigned_car_id  │
│   seats      │         │ created_at       │
└──────────────┘         └──────────────────┘
```

- **Car**: `id` (PK), `seats` (4-6), `availableSeats` (se actualiza al asignar/liberar)
- **Journey**: `id` (PK), `people` (1-6), `assignedCar` (FK nullable), `createdAt` (para orden FIFO)
- Un Journey con `assignedCar = NULL` está en cola de espera.

## 5. Algoritmo de asignación

```
addJourney(group):
  1. Verificar que el ID no existe (→ 409 si duplicado)
  2. Persistir el journey en BD
  3. Buscar primer coche con availableSeats >= group.people (ORDER BY id ASC)
  4. Si encontrado → occupy(people), asignar coche al journey
  5. Si no → queda en cola (assignedCar = NULL)

dropoff(groupId):
  1. Buscar journey por ID (→ 404 si no existe)
  2. Si tenía coche → release(people), liberar asientos
  3. Eliminar journey de BD
  4. Si liberó asientos → reassignWaiting()

reassignWaiting():
  1. Obtener cola de espera ORDER BY createdAt ASC
  2. Para cada grupo esperando:
     - Buscar coche con suficientes asientos
     - Si encontrado → asignar
     - Si no → sigue esperando
```

## 6. Manejo de errores

El `GlobalExceptionHandler` (@RestControllerAdvice) captura 7 tipos de excepción:

| Excepción | HTTP | Cuándo |
|---|---|---|
| `GroupNotFoundException` | 404 | Grupo no encontrado |
| `DuplicateGroupException` | 409 | ID de grupo duplicado |
| `MethodArgumentNotValidException` | 400 | `@Valid` en `@RequestBody` falla |
| `ConstraintViolationException` | 400 | `@Positive`/`@NotEmpty` en params falla |
| `MethodArgumentTypeMismatchException` | 400 | Path variable no numérica |
| `HttpRequestMethodNotSupportedException` | 405 | Verbo HTTP incorrecto |
| `HttpMessageNotReadableException` | 400 | JSON mal formado |

Formato de error consistente:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "seats: Seats must be at least 4",
  "timestamp": "2026-04-23T10:00:00"
}
```

## 7. Internacionalización (i18n)

- 4 ficheros JSON en `/static/i18n/`: `es.json`, `en.json`, `fr.json`, `de.json`
- ~80 claves por idioma
- Carga asíncrona al inicio (`preloadLanguages()`)
- Atributo `data-i18n` en HTML para traducción automática
- Idioma guardado en `localStorage`

## 8. Frontend (SPA)

Single Page Application embebida en `index.html` (~32KB):

| Sección | Descripción |
|---|---|
| **Sidebar** | Formularios: cargar flota, registrar grupo, localizar/dropoff |
| **Stats** | 4 tarjetas: coches, asientos libres, viajes activos, en espera |
| **Fleet grid** | Visualización de coches con asientos ocupados/libres |
| **Waiting queue** | Grupos en cola con icono de personas |
| **Log console** | Trazas en tiempo real con filtros ALL/INFO/WARN/ERROR/DEBUG |
| **Toasts** | Notificaciones popup para éxito/error con mensajes amigables |
