package com.carpooling.repository;

import com.carpooling.model.Journey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JourneyRepository extends JpaRepository<Journey, Integer> {

    @Query("SELECT j FROM Journey j WHERE j.assignedCar IS NULL ORDER BY j.createdAt ASC")
    List<Journey> findWaitingQueue();

    @Query("SELECT j FROM Journey j WHERE j.assignedCar IS NOT NULL")
    List<Journey> findAssigned();
}
