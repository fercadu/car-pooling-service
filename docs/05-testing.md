# 🧪 Testing Guide

## 1. Summary

| Suite | Class | Tests | Type | Description |
|---|---|---|---|---|
| **Service** | `CarPoolingServiceTest` | 23 | Integration (Spring + H2) | Business logic |
| **Controller** | `CarPoolingControllerTest` | 16 | Integration (MockMvc + H2) | REST endpoints |
| **Application** | `CarPoolingApplicationTests` | 1 | Context | Spring Boot startup |
| **Total** | — | **40** | — | — |

## 2. Running Tests

```bash
# All tests
mvn test

# A specific class
mvn test -Dtest=CarPoolingServiceTest

# A specific test
mvn test -Dtest=CarPoolingServiceTest#dropoffFreesSeatsAndReassignsWaiting
```

## 3. Test Configuration

Tests use **H2 in-memory** (`src/test/resources/application.properties`):
- No PostgreSQL installation needed
- The database is created and destroyed on each run (`create-drop`)
- Service tests use `@SpringBootTest` with real service injection

## 4. Service Tests (`CarPoolingServiceTest`)

### Fleet Loading (loadCars)
| Test | Verifies |
|---|---|
| `loadCarsResetsAllState` | Loading cars deletes all previous journeys |
| `loadCarsWithMultipleCalls` | Fleet can be loaded multiple times in a row |

### Journey Assignment (addJourney + locate)
| Test | Verifies |
|---|---|
| `journeyIsAssignedToFirstCarWithEnoughSeats` | Assigned to the first available car |
| `journeyGoesToSecondCarIfFirstIsFull` | If first is full, uses the next one |
| `journeyGoesToWaitingQueueWhenNoCarsAvailable` | No free cars → waiting queue |
| `singlePersonAssignedSuccessfully` | A group of 1 person is assigned correctly |
| `groupOfSixAssignedToCarWithSixSeats` | A group of 6 goes to the 6-seat car |
| `multipleSmallGroupsShareSameCar` | Two small groups share the same car |

### Dropoff
| Test | Verifies |
|---|---|
| `dropoffExistingJourneyReturnsTrue` | Dropoff of existing group returns true |
| `dropoffNonExistentJourneyReturnsFalse` | Dropoff of non-existent group returns false |
| `dropoffFreesSeatsAndReassignsWaiting` | Frees seats and reassigns queue |
| `dropoffWaitingGroupRemovesFromQueue` | Removes group from queue without reassigning |
| `dropoffGroupThenLocateThrowsNotFound` | After dropoff, locate throws exception |

### Locate
| Test | Verifies |
|---|---|
| `locateUnknownGroupThrowsNotFound` | Non-existent group → `GroupNotFoundException` |
| `locateWaitingGroupReturnsEmptyOptional` | Waiting group → `Optional.empty()` |
| `locateAssignedGroupReturnsCarWithCorrectSeats` | Assigned group → correct car |

### Fairness
| Test | Verifies |
|---|---|
| `smallerGroupServedBeforeLargerWhenLargerCannotFit` | Smaller group served first if larger can't fit |
| `earlierGroupServedFirstWhenBothCanFit` | FIFO order when both fit |
| `multipleWaitingGroupsReassignedAfterDropoff` | Multiple groups reassigned in cascade |
| `waitingGroupNotReassignedIfStillNoSpace` | Group not reassigned if still no space |

### Edge Cases
| Test | Verifies |
|---|---|
| `addDuplicateJourneyThrowsException` | Duplicate ID → `DuplicateGroupException` |
| `dropoffTwiceReturnsFalseSecondTime` | Double dropoff → false the second time |
| `loadCarsAfterJourneysResetsEverything` | Complete reset after loading new fleet |

## 5. Controller Tests (`CarPoolingControllerTest`)

### Basic Endpoints
| Test | Endpoint | Status | Verifies |
|---|---|---|---|
| `statusReturns200` | `GET /status` | 200 | Health check |
| `loadCarsReturns200` | `PUT /cars` | 200 | Successful load |
| `journeyAssignsCar` | `POST /journeys` + `GET .../car` | 201, 200 | Full assignment |
| `journeyGoesToWaitingQueue` | `GET .../car` | 204 | Waiting state |
| `dropoffFreesSeatsAndAssignsWaiting` | `DELETE /journeys/{id}` | 204 | Reassignment |

### Validation and Errors
| Test | Endpoint | Status | Verifies |
|---|---|---|---|
| `loadCarsRejects7Seats` | `PUT /cars` | 400 | Seats > 6 rejected |
| `dropoffOfUnknownGroupReturns404` | `DELETE /journeys/999` | 404 | Non-existent group |
| `locateOfUnknownGroupReturns404` | `GET /journeys/999/car` | 404 | Non-existent group |
| `journeyRejectsPeopleOutOfRange` | `POST /journeys` | 400 | people=0 and people=7 |
| `duplicateJourneyReturns409` | `POST /journeys` | 409 | Duplicate ID |
| `nonNumericPathVariableReturns400` | `GET /journeys/abc/car` | 400 | Wrong type |
| `negativePathVariableReturns400` | `DELETE /journeys/-5` | 400 | Negative ID |
| `emptyCarListReturns400` | `PUT /cars` with `[]` | 400 | Empty list |
| `wrongHttpMethodReturns405` | `POST /status` | 405 | Wrong HTTP verb |

### Fairness
| Test | Verifies |
|---|---|
| `fairnessSmallGroupServedBeforeLargeWhenNoCarForLarge` | Reassignment respects rules |
| `dropoffOfWaitingGroupRemovesFromQueue` | Group correctly removed from queue |

## 6. Coverage Matrix

| Feature | Service | Controller | Total |
|---|---|---|---|
| Fleet loading | 2 | 2 | 4 |
| Journey registration | 6 | 2 | 8 |
| Dropoff | 5 | 4 | 9 |
| Locate | 3 | 2 | 5 |
| Fairness | 4 | 1 | 5 |
| Validations | 1 | 6 | 7 |
| Edge cases | 2 | — | 2 |
| **Total** | **23** | **16+1** | **40** |

## 7. Adding New Tests

To add a service test:
```java
@Test
void myNewTest() {
    service.loadCars(List.of(new Car(1, 4)));
    service.addJourney(new Journey(1, 3));
    // asserts...
}
```

To add a controller test:
```java
@Test
void myNewEndpointTest() throws Exception {
    mockMvc.perform(post("/api/v1/journeys")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":1,\"people\":3}"))
        .andExpect(status().isCreated());
}
```
