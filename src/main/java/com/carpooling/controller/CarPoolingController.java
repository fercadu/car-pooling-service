package com.carpooling.controller;

import com.carpooling.dto.request.CarRequestDTO;
import com.carpooling.dto.request.JourneyRequestDTO;
import com.carpooling.dto.response.CarResponseDTO;
import com.carpooling.exception.GroupNotFoundException;
import com.carpooling.model.Car;
import com.carpooling.model.Journey;
import com.carpooling.service.CarPoolingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Validated
@RestController
@RequestMapping("/api/v1")
public class CarPoolingController {

    private static final Logger log = LoggerFactory.getLogger(CarPoolingController.class);

    private final CarPoolingService service;

    public CarPoolingController(CarPoolingService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ResponseEntity<Void> status() {
        log.trace("GET /status — health check");
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/cars", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> loadCars(
            @RequestBody @NotEmpty(message = "Car list must not be empty") List<@Valid CarRequestDTO> cars) {
        log.info("PUT /cars — received {} cars", cars.size());

        List<Car> domainCars = cars.stream()
                .map(dto -> new Car(dto.id(), dto.seats()))
                .toList();

        service.loadCars(domainCars);
        log.info("PUT /cars — 200 OK");
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/journeys", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createJourney(@Valid @RequestBody JourneyRequestDTO journey) {
        log.info("POST /journeys — Group {} with {} people", journey.id(), journey.people());

        service.addJourney(new Journey(journey.id(), journey.people()));
        log.info("POST /journeys — 201 Created");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/journeys/{id}")
    public ResponseEntity<Void> dropoff(@PathVariable @Positive(message = "ID must be positive") int id) {
        log.info("DELETE /journeys/{} — dropoff", id);

        boolean found = service.dropoff(id);
        if (!found) {
            throw new GroupNotFoundException(id);
        }

        log.info("DELETE /journeys/{} — 204 No Content", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/journeys/{id}/car", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CarResponseDTO> locateCar(@PathVariable @Positive(message = "ID must be positive") int id) {
        log.debug("GET /journeys/{}/car — locate", id);

        Optional<Car> result = service.locate(id);
        if (result.isEmpty()) {
            log.debug("GET /journeys/{}/car — 204 No Content: waiting", id);
            return ResponseEntity.noContent().build();
        }

        CarResponseDTO response = CarResponseDTO.fromDomain(result.get());
        log.debug("GET /journeys/{}/car — 200 OK: Car #{}", id, response.id());
        return ResponseEntity.ok(response);
    }
}
