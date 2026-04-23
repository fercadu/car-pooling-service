package com.carpooling.service;

import com.carpooling.exception.DuplicateGroupException;
import com.carpooling.exception.GroupNotFoundException;
import com.carpooling.model.Car;
import com.carpooling.model.Journey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CarPoolingServiceTest {

    @Autowired
    private CarPoolingService service;

    @BeforeEach
    void setUp() {
        service.loadCars(List.of(new Car(1, 4), new Car(2, 6)));
    }

    // --- loadCars ---

    @Test
    void loadCarsResetsAllState() {
        service.addJourney(new Journey(1, 3));
        service.loadCars(List.of(new Car(10, 5)));

        assertThrows(GroupNotFoundException.class, () -> service.locate(1));
    }

    @Test
    void loadCarsWithMultipleCalls() {
        service.loadCars(List.of(new Car(99, 6)));
        service.addJourney(new Journey(1, 6));

        Optional<Car> car = service.locate(1);
        assertNotNull(car);
        assertTrue(car.isPresent());
        assertEquals(99, car.get().getId());
    }

    // --- addJourney + locate ---

    @Test
    void journeyIsAssignedToFirstCarWithEnoughSeats() {
        service.addJourney(new Journey(1, 3));

        Optional<Car> result = service.locate(1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    @Test
    void journeyGoesToSecondCarIfFirstIsFull() {
        service.addJourney(new Journey(1, 4));
        service.addJourney(new Journey(2, 5));

        assertEquals(1, service.locate(1).get().getId());
        assertEquals(2, service.locate(2).get().getId());
    }

    @Test
    void journeyGoesToWaitingQueueWhenNoCarsAvailable() {
        service.addJourney(new Journey(1, 4));
        service.addJourney(new Journey(2, 6));
        service.addJourney(new Journey(3, 1));

        Optional<Car> result = service.locate(3);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void singlePersonAssignedSuccessfully() {
        service.addJourney(new Journey(1, 1));
        assertTrue(service.locate(1).isPresent());
    }

    @Test
    void groupOfSixAssignedToCarWithSixSeats() {
        service.addJourney(new Journey(1, 6));

        Optional<Car> result = service.locate(1);
        assertTrue(result.isPresent());
        assertEquals(2, result.get().getId());
    }

    @Test
    void multipleSmallGroupsShareSameCar() {
        service.addJourney(new Journey(1, 2));
        service.addJourney(new Journey(2, 2));

        assertEquals(1, service.locate(1).get().getId());
        assertEquals(1, service.locate(2).get().getId());
    }

    // --- dropoff ---

    @Test
    void dropoffExistingJourneyReturnsTrue() {
        service.addJourney(new Journey(1, 3));
        assertTrue(service.dropoff(1));
    }

    @Test
    void dropoffNonExistentJourneyReturnsFalse() {
        assertFalse(service.dropoff(999));
    }

    @Test
    void dropoffFreesSeatsAndReassignsWaiting() {
        service.addJourney(new Journey(1, 4));
        service.addJourney(new Journey(2, 6));
        service.addJourney(new Journey(3, 2));

        service.dropoff(1);

        assertTrue(service.locate(3).isPresent());
        assertEquals(1, service.locate(3).get().getId());
    }

    @Test
    void dropoffWaitingGroupRemovesFromQueue() {
        service.addJourney(new Journey(1, 4));
        service.addJourney(new Journey(2, 6));
        service.addJourney(new Journey(3, 3));

        assertTrue(service.dropoff(3));
        assertThrows(GroupNotFoundException.class, () -> service.locate(3));
    }

    @Test
    void dropoffGroupThenLocateThrowsNotFound() {
        service.addJourney(new Journey(1, 2));
        service.dropoff(1);
        assertThrows(GroupNotFoundException.class, () -> service.locate(1));
    }

    // --- locate ---

    @Test
    void locateUnknownGroupThrowsNotFound() {
        assertThrows(GroupNotFoundException.class, () -> service.locate(999));
    }

    @Test
    void locateWaitingGroupReturnsEmptyOptional() {
        service.addJourney(new Journey(1, 4));
        service.addJourney(new Journey(2, 6));
        service.addJourney(new Journey(3, 1));

        Optional<Car> result = service.locate(3);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void locateAssignedGroupReturnsCarWithCorrectSeats() {
        service.addJourney(new Journey(1, 4));

        Car car = service.locate(1).get();
        assertEquals(4, car.getSeats());
    }

    // --- Fairness ---

    @Test
    void smallerGroupServedBeforeLargerWhenLargerCannotFit() {
        service.loadCars(List.of(new Car(1, 4)));
        service.addJourney(new Journey(1, 4));
        service.addJourney(new Journey(2, 6));
        service.addJourney(new Journey(3, 2));

        service.dropoff(1);

        assertTrue(service.locate(3).isPresent());
        assertTrue(service.locate(2).isEmpty());
    }

    @Test
    void earlierGroupServedFirstWhenBothCanFit() {
        service.loadCars(List.of(new Car(1, 6)));
        service.addJourney(new Journey(1, 6));
        service.addJourney(new Journey(2, 3));
        service.addJourney(new Journey(3, 2));

        service.dropoff(1);

        assertTrue(service.locate(2).isPresent());
        assertTrue(service.locate(3).isPresent());
    }

    @Test
    void multipleWaitingGroupsReassignedAfterDropoff() {
        service.loadCars(List.of(new Car(1, 6)));
        service.addJourney(new Journey(1, 6));
        service.addJourney(new Journey(2, 2));
        service.addJourney(new Journey(3, 2));
        service.addJourney(new Journey(4, 2));

        service.dropoff(1);

        assertTrue(service.locate(2).isPresent());
        assertTrue(service.locate(3).isPresent());
        assertTrue(service.locate(4).isPresent());
    }

    @Test
    void waitingGroupNotReassignedIfStillNoSpace() {
        service.loadCars(List.of(new Car(1, 4)));
        service.addJourney(new Journey(1, 2));
        service.addJourney(new Journey(2, 2));
        service.addJourney(new Journey(3, 4));

        service.dropoff(1);
        assertTrue(service.locate(3).isEmpty());
    }

    // --- Edge cases ---

    @Test
    void addDuplicateJourneyThrowsException() {
        service.addJourney(new Journey(1, 2));
        assertThrows(DuplicateGroupException.class, () -> service.addJourney(new Journey(1, 4)));
    }

    @Test
    void dropoffTwiceReturnsFalseSecondTime() {
        service.addJourney(new Journey(1, 2));
        assertTrue(service.dropoff(1));
        assertFalse(service.dropoff(1));
    }

    @Test
    void loadCarsAfterJourneysResetsEverything() {
        service.addJourney(new Journey(1, 4));
        service.addJourney(new Journey(2, 6));
        service.addJourney(new Journey(3, 1));

        service.loadCars(List.of(new Car(50, 5)));

        assertThrows(GroupNotFoundException.class, () -> service.locate(1));
        assertThrows(GroupNotFoundException.class, () -> service.locate(2));
        assertThrows(GroupNotFoundException.class, () -> service.locate(3));
    }
}
