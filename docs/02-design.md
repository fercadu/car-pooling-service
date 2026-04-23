# 🏗️ Design and Architecture

## 1. Overview

The service follows a **3-layer architecture** with Spring Boot 3.2.5 and Java 21:

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (SPA)                        │
│              index.html + i18n JSON files                │
├─────────────────────────────────────────────────────────┤
│                  REST API (/api/v1)                      │
│              CarPoolingController                        │
│       DTOs (request) ←──→ DTOs (response)               │
├─────────────────────────────────────────────────────────┤
│                  Service Layer                           │
│              CarPoolingService                           │
│         @Transactional — Business logic                  │
├─────────────────────────────────────────────────────────┤
│                Persistence Layer                         │
│        CarRepository   JourneyRepository                │
│              Spring Data JPA                             │
├─────────────────────────────────────────────────────────┤
│                    Database                              │
│          PostgreSQL (prod) / H2 (test)                   │
└─────────────────────────────────────────────────────────┘
```

## 2. Package Structure

```
com.carpooling
├── CarPoolingApplication.java          # @SpringBootApplication
├── controller
│   └── CarPoolingController.java       # REST endpoints
├── dto
│   ├── request
│   │   ├── CarRequestDTO.java          # Record with validations
│   │   └── JourneyRequestDTO.java      # Record with validations
│   └── response
│       ├── CarResponseDTO.java         # Record with fromDomain() factory
│       └── ErrorResponseDTO.java       # Consistent error response
├── exception
│   ├── GlobalExceptionHandler.java     # @RestControllerAdvice (7 handlers)
│   ├── GroupNotFoundException.java     # → 404
│   └── DuplicateGroupException.java   # → 409
├── model
│   ├── Car.java                        # @Entity — vehicle
│   └── Journey.java                    # @Entity — journey group
├── repository
│   ├── CarRepository.java              # JpaRepository<Car, Integer>
│   └── JourneyRepository.java          # JpaRepository<Journey, Integer>
└── service
    └── CarPoolingService.java          # Business logic
```

## 3. REST API Design

Base path: `/api/v1`

| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| `GET` | `/status` | Health check | — | `200` |
| `PUT` | `/cars` | Load fleet | `List<CarRequestDTO>` | `200` / `400` |
| `POST` | `/journeys` | Register group | `JourneyRequestDTO` | `201` / `400` / `409` |
| `DELETE` | `/journeys/{id}` | Dropoff | Path: `id` (positive int) | `204` / `400` / `404` |
| `GET` | `/journeys/{id}/car` | Locate car | Path: `id` (positive int) | `200` + `CarResponseDTO` / `204` / `404` |

### REST Conventions Applied
- **Plural nouns** in endpoints (`/cars`, `/journeys`)
- **Correct HTTP verbs**: GET (read), POST (create), PUT (replace), DELETE (remove)
- **URL versioning** (`/api/v1`)
- **DTOs separated** from domain entities
- **Semantic HTTP codes**: 201 Created, 204 No Content, 400 Bad Request, 404 Not Found, 405 Method Not Allowed, 409 Conflict

## 4. Data Model

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

- **Car**: `id` (PK), `seats` (4-6), `availableSeats` (updated on assign/release)
- **Journey**: `id` (PK), `people` (1-6), `assignedCar` (FK nullable), `createdAt` (for FIFO ordering)
- A Journey with `assignedCar = NULL` is in the waiting queue.

## 5. Assignment Algorithm

```
addJourney(group):
  1. Verify ID does not exist (→ 409 if duplicate)
  2. Persist journey in DB
  3. Find first car with availableSeats >= group.people (ORDER BY id ASC)
  4. If found → occupy(people), assign car to journey
  5. If not → stays in queue (assignedCar = NULL)

dropoff(groupId):
  1. Find journey by ID (→ 404 if not found)
  2. If had car → release(people), free seats
  3. Delete journey from DB
  4. If seats were freed → reassignWaiting()

reassignWaiting():
  1. Get waiting queue ORDER BY createdAt ASC
  2. For each waiting group:
     - Find car with enough seats
     - If found → assign
     - If not → keep waiting
```

## 6. Error Handling

The `GlobalExceptionHandler` (@RestControllerAdvice) catches 7 exception types:

| Exception | HTTP | When |
|---|---|---|
| `GroupNotFoundException` | 404 | Group not found |
| `DuplicateGroupException` | 409 | Duplicate group ID |
| `MethodArgumentNotValidException` | 400 | `@Valid` on `@RequestBody` fails |
| `ConstraintViolationException` | 400 | `@Positive`/`@NotEmpty` on params fails |
| `MethodArgumentTypeMismatchException` | 400 | Non-numeric path variable |
| `HttpRequestMethodNotSupportedException` | 405 | Wrong HTTP verb |
| `HttpMessageNotReadableException` | 400 | Malformed JSON |

Consistent error format:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "seats: Seats must be at least 4",
  "timestamp": "2026-04-23T10:00:00"
}
```

## 7. Internationalization (i18n)

- 4 JSON files in `/static/i18n/`: `es.json`, `en.json`, `fr.json`, `de.json`
- ~80 keys per language
- Async loading at startup (`preloadLanguages()`)
- `data-i18n` attribute in HTML for automatic translation
- Language saved in `localStorage`

## 8. Frontend (SPA)

Single Page Application embedded in `index.html` (~32KB):

| Section | Description |
|---|---|
| **Sidebar** | Forms: load fleet, register group, locate/dropoff |
| **Stats** | 4 cards: cars, free seats, active journeys, waiting |
| **Fleet grid** | Car visualization with occupied/free seats |
| **Waiting queue** | Queued groups with people icon |
| **Log console** | Real-time traces with ALL/INFO/WARN/ERROR/DEBUG filters |
| **Toasts** | Popup notifications for success/error with friendly messages |
