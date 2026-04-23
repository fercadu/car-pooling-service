# 📋 System Requirements

## 1. Overview

Car Pooling Service is a shared car assignment service. It manages a fleet of vehicles and assigns groups of people to cars based on seat availability, respecting arrival order.

## 2. Functional Requirements

### FR-01 — Fleet Management
- The operator can load a list of cars with their ID and number of seats (4, 5, or 6).
- Loading a new fleet **replaces** all previous data (cars, journeys, and waiting queue).
- The fleet can be reloaded at any time.

### FR-02 — Journey Registration
- A group of 1 to 6 people requests a journey by providing a group ID and the number of people.
- If a car with enough free seats is available, it is automatically assigned.
- If no car is available, the group enters the **waiting queue**.
- Registering two groups with the same ID is not allowed.

### FR-03 — Car Assignment
- The first available car with enough seats is assigned (ordered by ID ascending).
- A group always travels together in the same car.
- A group cannot be reassigned to another car once assigned.

### FR-04 — Fairness
- Groups should be served as fast as possible while maintaining arrival order.
- A later group can only be served before an earlier one if **no car can serve** the earlier group.
- Example: a group of 6 is waiting; a group of 2 arrives and there are 4 free seats → the group of 2 is assigned.

### FR-05 — Dropoff
- A group can leave at any time, whether they traveled or not.
- If the group had an assigned car, the seats are freed.
- After freeing seats, the system attempts to reassign groups from the waiting queue.

### FR-06 — Locate
- Given a group ID, the system returns which car they are traveling in.
- If the group is waiting, it indicates no car is assigned.
- If the group does not exist, an error is returned.

### FR-07 — Web Interface
- Operator dashboard with real-time visualization of fleet, journeys, and waiting queue.
- Log console with filtering by level (ALL, INFO, WARN, ERROR, DEBUG).
- Statistics: total cars, free seats, active journeys, groups waiting.
- Multi-language: Spanish, English, French, German.

### FR-08 — Persistence
- Data is stored in PostgreSQL.
- State survives service restarts.

## 3. Non-Functional Requirements

### NFR-01 — Performance
- Car assignment operates in linear time over the fleet.
- Maximum 500 log lines in the interface to avoid browser memory consumption.

### NFR-02 — Scalability
- The application is containerized with Docker.
- Configuration via environment variables for deployment on any cloud.

### NFR-03 — Usability
- Friendly error messages in popups (no technical validation paths).
- Responsive interface with dark theme.

### NFR-04 — Maintainability
- Layered architecture: Controller → Service → Repository.
- DTOs separated from domain entities.
- Translations externalized to JSON files.

### NFR-05 — Security
- Docker container runs with non-root user.
- No secrets stored in source code (environment variables).

## 4. Constraints

| Constraint | Value |
|---|---|
| Seats per car | 4, 5, or 6 |
| People per group | 1 to 6 |
| Group ID | Positive integer, unique |
| Car ID | Integer |
| Supported languages | es, en, fr, de |

## 5. Main Use Cases

```
┌──────────┐       ┌──────────────────────────┐
│ Operator │──────▶│ Load car fleet            │  PUT /api/v1/cars
│          │──────▶│ Register journey group    │  POST /api/v1/journeys
│          │──────▶│ Locate group              │  GET /api/v1/journeys/{id}/car
│          │──────▶│ Drop off group            │  DELETE /api/v1/journeys/{id}
│          │──────▶│ Check service status      │  GET /api/v1/status
└──────────┘       └──────────────────────────┘
```
