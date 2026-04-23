# 🧪 Guía de Pruebas

## 1. Resumen

| Suite | Clase | Tests | Tipo | Descripción |
|---|---|---|---|---|
| **Service** | `CarPoolingServiceTest` | 23 | Integración (Spring + H2) | Lógica de negocio |
| **Controller** | `CarPoolingControllerTest` | 16 | Integración (MockMvc + H2) | Endpoints REST |
| **Application** | `CarPoolingApplicationTests` | 1 | Contexto | Arranque de Spring Boot |
| **Total** | — | **40** | — | — |

## 2. Ejecutar tests

```bash
# Todos los tests
mvn test

# Una clase específica
mvn test -Dtest=CarPoolingServiceTest

# Un test específico
mvn test -Dtest=CarPoolingServiceTest#dropoffFreesSeatsAndReassignsWaiting
```

## 3. Configuración de tests

Los tests usan **H2 en memoria** (`src/test/resources/application.properties`):
- No necesitan PostgreSQL instalado
- La BD se crea y destruye en cada ejecución (`create-drop`)
- Los tests de servicio usan `@SpringBootTest` con inyección del servicio real

## 4. Tests del servicio (`CarPoolingServiceTest`)

### Carga de flota (loadCars)
| Test | Verifica |
|---|---|
| `loadCarsResetsAllState` | Cargar coches elimina todos los viajes previos |
| `loadCarsWithMultipleCalls` | Se puede cargar flota varias veces seguidas |

### Asignación de viajes (addJourney + locate)
| Test | Verifica |
|---|---|
| `journeyIsAssignedToFirstCarWithEnoughSeats` | Se asigna al primer coche disponible |
| `journeyGoesToSecondCarIfFirstIsFull` | Si el primero está lleno, usa el siguiente |
| `journeyGoesToWaitingQueueWhenNoCarsAvailable` | Sin coches libres → cola de espera |
| `singlePersonAssignedSuccessfully` | Un grupo de 1 persona se asigna correctamente |
| `groupOfSixAssignedToCarWithSixSeats` | Un grupo de 6 va al coche de 6 asientos |
| `multipleSmallGroupsShareSameCar` | Dos grupos pequeños comparten coche |

### Abandono (dropoff)
| Test | Verifica |
|---|---|
| `dropoffExistingJourneyReturnsTrue` | Dropoff de grupo existente devuelve true |
| `dropoffNonExistentJourneyReturnsFalse` | Dropoff de grupo inexistente devuelve false |
| `dropoffFreesSeatsAndReassignsWaiting` | Libera asientos y reasigna cola |
| `dropoffWaitingGroupRemovesFromQueue` | Elimina grupo de la cola sin reasignar |
| `dropoffGroupThenLocateThrowsNotFound` | Tras dropoff, locate lanza excepción |

### Localización (locate)
| Test | Verifica |
|---|---|
| `locateUnknownGroupThrowsNotFound` | Grupo inexistente → `GroupNotFoundException` |
| `locateWaitingGroupReturnsEmptyOptional` | Grupo en espera → `Optional.empty()` |
| `locateAssignedGroupReturnsCarWithCorrectSeats` | Grupo asignado → coche correcto |

### Equidad (fairness)
| Test | Verifica |
|---|---|
| `smallerGroupServedBeforeLargerWhenLargerCannotFit` | Grupo pequeño se sirve antes si el grande no cabe |
| `earlierGroupServedFirstWhenBothCanFit` | Orden FIFO cuando ambos caben |
| `multipleWaitingGroupsReassignedAfterDropoff` | Múltiples grupos reasignados en cascada |
| `waitingGroupNotReassignedIfStillNoSpace` | Grupo no reasignado si sigue sin espacio |

### Casos límite
| Test | Verifica |
|---|---|
| `addDuplicateJourneyThrowsException` | ID duplicado → `DuplicateGroupException` |
| `dropoffTwiceReturnsFalseSecondTime` | Doble dropoff → false la segunda vez |
| `loadCarsAfterJourneysResetsEverything` | Reset completo tras cargar nueva flota |

## 5. Tests del controlador (`CarPoolingControllerTest`)

### Endpoints básicos
| Test | Endpoint | Status | Verifica |
|---|---|---|---|
| `statusReturns200` | `GET /status` | 200 | Health check |
| `loadCarsReturns200` | `PUT /cars` | 200 | Carga exitosa |
| `journeyAssignsCar` | `POST /journeys` + `GET .../car` | 201, 200 | Asignación completa |
| `journeyGoesToWaitingQueue` | `GET .../car` | 204 | En espera |
| `dropoffFreesSeatsAndAssignsWaiting` | `DELETE /journeys/{id}` | 204 | Reasignación |

### Errores y validaciones
| Test | Endpoint | Status | Verifica |
|---|---|---|---|
| `loadCarsRejects7Seats` | `PUT /cars` | 400 | Asientos > 6 rechazados |
| `dropoffOfUnknownGroupReturns404` | `DELETE /journeys/999` | 404 | Grupo inexistente |
| `locateOfUnknownGroupReturns404` | `GET /journeys/999/car` | 404 | Grupo inexistente |
| `journeyRejectsPeopleOutOfRange` | `POST /journeys` | 400 | people=0 y people=7 |
| `duplicateJourneyReturns409` | `POST /journeys` | 409 | ID duplicado |
| `nonNumericPathVariableReturns400` | `GET /journeys/abc/car` | 400 | Tipo incorrecto |
| `negativePathVariableReturns400` | `DELETE /journeys/-5` | 400 | ID negativo |
| `emptyCarListReturns400` | `PUT /cars` con `[]` | 400 | Lista vacía |
| `wrongHttpMethodReturns405` | `POST /status` | 405 | Verbo HTTP incorrecto |

### Equidad (fairness)
| Test | Verifica |
|---|---|
| `fairnessSmallGroupServedBeforeLargeWhenNoCarForLarge` | Reasignación respeta las reglas |
| `dropoffOfWaitingGroupRemovesFromQueue` | Grupo eliminado de cola correctamente |

## 6. Matriz de cobertura

| Funcionalidad | Service | Controller | Total |
|---|---|---|---|
| Carga de flota | 2 | 2 | 4 |
| Registro de viajes | 6 | 2 | 8 |
| Dropoff | 5 | 4 | 9 |
| Localización | 3 | 2 | 5 |
| Equidad (fairness) | 4 | 1 | 5 |
| Validaciones | 1 | 6 | 7 |
| Casos límite | 2 | — | 2 |
| **Total** | **23** | **16+1** | **40** |

## 7. Añadir nuevos tests

Para añadir un test de servicio:
```java
@Test
void miNuevoTest() {
    service.loadCars(List.of(new Car(1, 4)));
    service.addJourney(new Journey(1, 3));
    // asserts...
}
```

Para añadir un test de controlador:
```java
@Test
void miNuevoTestEndpoint() throws Exception {
    mockMvc.perform(post("/api/v1/journeys")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":1,\"people\":3}"))
        .andExpect(status().isCreated());
}
```
