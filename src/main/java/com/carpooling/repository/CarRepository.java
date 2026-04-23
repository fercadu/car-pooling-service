package com.carpooling.repository;

import com.carpooling.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Integer> {

    @Query("SELECT c FROM Car c WHERE c.availableSeats >= :people ORDER BY c.id ASC")
    List<Car> findCarsWithAvailableSeats(int people);
}
