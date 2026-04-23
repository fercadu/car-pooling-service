package com.carpooling.service;

import com.carpooling.exception.DuplicateGroupException;
import com.carpooling.exception.GroupNotFoundException;
import com.carpooling.model.Car;
import com.carpooling.model.Journey;
import com.carpooling.repository.CarRepository;
import com.carpooling.repository.JourneyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CarPoolingService {

    private static final Logger log = LoggerFactory.getLogger(CarPoolingService.class);

    private final CarRepository carRepository;
    private final JourneyRepository journeyRepository;

    public CarPoolingService(CarRepository carRepository, JourneyRepository journeyRepository) {
        this.carRepository = carRepository;
        this.journeyRepository = journeyRepository;
    }

    /**
     * Resets all state and loads a new fleet of cars.
     */
    @Transactional
    public void loadCars(List<Car> newCars) {
        log.info("══════════════════════════════════════════════════");
        log.info("LOAD CARS — Resetting all state");
        log.info("  Previous state: {} cars, {} journeys, {} waiting",
                carRepository.count(), journeyRepository.count(),
                journeyRepository.findWaitingQueue().size());

        journeyRepository.deleteAll();
        carRepository.deleteAll();

        for (Car c : newCars) {
            carRepository.save(new Car(c.getId(), c.getSeats()));
        }

        int totalSeats = carRepository.findAll().stream().mapToInt(Car::getSeats).sum();
        log.info("  New fleet loaded: {} cars, {} total seats", carRepository.count(), totalSeats);
        carRepository.findAll().forEach(c -> log.debug("    Car #{}: {} seats", c.getId(), c.getSeats()));
        log.info("══════════════════════════════════════════════════");
    }

    /**
     * Registers a new journey request. Tries to assign a car immediately;
     * if none available, the group is added to the waiting queue.
     */
    @Transactional
    public void addJourney(Journey journey) {
        log.info("──────────────────────────────────────────────────");
        log.info("NEW JOURNEY — Group {} ({} people)", journey.getId(), journey.getPeople());

        if (journeyRepository.existsById(journey.getId())) {
            log.warn("  ❌ Group {} already exists — rejecting", journey.getId());
            throw new DuplicateGroupException(journey.getId());
        }

        journey = journeyRepository.save(journey);

        if (tryAssign(journey)) {
            log.info("  ✅ Assigned to Car #{} (remaining seats: {})",
                    journey.getAssignedCar().getId(), journey.getAssignedCar().getAvailableSeats());
        } else {
            log.warn("  ⏳ No car available — added to waiting queue (position: {})",
                    journeyRepository.findWaitingQueue().size());
        }

        logState();
    }

    /**
     * Drops off a group. If they had a car, frees the seats and
     * tries to serve waiting groups.
     */
    @Transactional
    public boolean dropoff(int groupId) {
        log.info("──────────────────────────────────────────────────");
        log.info("DROPOFF — Group {}", groupId);

        Optional<Journey> opt = journeyRepository.findById(groupId);
        if (opt.isEmpty()) {
            log.warn("  ❌ Group {} not found — returning 404", groupId);
            return false;
        }

        Journey journey = opt.get();
        Car assignedCar = journey.getAssignedCar();

        if (assignedCar != null) {
            assignedCar.release(journey.getPeople());
            carRepository.save(assignedCar);
            log.info("  🚪 Group {} ({} people) dropped off from Car #{} (now {} free seats)",
                    groupId, journey.getPeople(), assignedCar.getId(), assignedCar.getAvailableSeats());
        } else {
            log.info("  🚪 Group {} ({} people) removed from waiting queue (was not assigned)",
                    groupId, journey.getPeople());
        }

        journeyRepository.delete(journey);

        if (assignedCar != null) {
            reassignWaiting();
        }

        logState();
        return true;
    }

    /**
     * Locates the car assigned to a group.
     * Throws GroupNotFoundException if the group does not exist.
     * Returns empty Optional if the group is waiting.
     */
    @Transactional(readOnly = true)
    public Optional<Car> locate(int groupId) {
        log.debug("LOCATE — Group {}", groupId);

        Optional<Journey> opt = journeyRepository.findById(groupId);
        if (opt.isEmpty()) {
            log.debug("  ❌ Group {} not found", groupId);
            throw new GroupNotFoundException(groupId);
        }

        Car car = opt.get().getAssignedCar();
        if (car != null) {
            log.debug("  📍 Group {} is in Car #{}", groupId, car.getId());
        } else {
            log.debug("  ⏳ Group {} is waiting (no car assigned)", groupId);
        }

        return Optional.ofNullable(car);
    }

    /**
     * Tries to assign a car to a journey group.
     */
    private boolean tryAssign(Journey journey) {
        List<Car> available = carRepository.findCarsWithAvailableSeats(journey.getPeople());
        if (available.isEmpty()) {
            log.debug("  tryAssign: No car found for Group {} ({} people)",
                    journey.getId(), journey.getPeople());
            return false;
        }

        Car car = available.get(0);
        car.occupy(journey.getPeople());
        carRepository.save(car);

        journey.setAssignedCar(car);
        journeyRepository.save(journey);

        log.debug("  tryAssign: Group {} ({} people) → Car #{} ({}/{} seats used)",
                journey.getId(), journey.getPeople(), car.getId(),
                car.getSeats() - car.getAvailableSeats(), car.getSeats());
        return true;
    }

    /**
     * After a dropoff, iterates the waiting queue in arrival order
     * and assigns cars where possible.
     */
    private void reassignWaiting() {
        List<Journey> waitingQueue = journeyRepository.findWaitingQueue();
        if (waitingQueue.isEmpty()) {
            log.debug("  reassignWaiting: Queue is empty, nothing to reassign");
            return;
        }

        log.info("  🔄 Reassigning waiting groups ({} in queue)...", waitingQueue.size());

        int reassigned = 0;
        for (Journey waiting : waitingQueue) {
            if (tryAssign(waiting)) {
                log.info("    ✅ Group {} ({} people) → Car #{} (remaining seats: {})",
                        waiting.getId(), waiting.getPeople(),
                        waiting.getAssignedCar().getId(), waiting.getAssignedCar().getAvailableSeats());
                reassigned++;
            } else {
                log.debug("    ⏳ Group {} ({} people) — still no car available",
                        waiting.getId(), waiting.getPeople());
            }
        }

        log.info("  🔄 Reassignment complete: {} groups assigned, {} still waiting",
                reassigned, journeyRepository.findWaitingQueue().size());
    }

    private void logState() {
        List<Car> allCars = carRepository.findAll();
        int totalSeats = allCars.stream().mapToInt(Car::getSeats).sum();
        int usedSeats = totalSeats - allCars.stream().mapToInt(Car::getAvailableSeats).sum();
        List<Journey> assigned = journeyRepository.findAssigned();
        List<Journey> waiting = journeyRepository.findWaitingQueue();

        log.info("  📊 State: {} cars | {}/{} seats used | {} active journeys | {} waiting",
                allCars.size(), usedSeats, totalSeats, assigned.size(), waiting.size());

        if (log.isDebugEnabled()) {
            log.debug("  Fleet detail:");
            allCars.forEach(c -> {
                String groups = assigned.stream()
                        .filter(j -> j.getAssignedCar() != null && j.getAssignedCar().getId() == c.getId())
                        .map(j -> "G" + j.getId() + "(" + j.getPeople() + "p)")
                        .collect(Collectors.joining(", "));
                log.debug("    Car #{}: {}/{} seats | {}",
                        c.getId(), c.getSeats() - c.getAvailableSeats(), c.getSeats(),
                        groups.isEmpty() ? "empty" : groups);
            });
            if (!waiting.isEmpty()) {
                String queue = waiting.stream()
                        .map(j -> "G" + j.getId() + "(" + j.getPeople() + "p)")
                        .collect(Collectors.joining(" → "));
                log.debug("  Waiting queue: {}", queue);
            }
        }
    }
}
