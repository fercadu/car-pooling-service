package com.carpooling.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "journeys")
public class Journey {

    @Id
    private int id;

    @Column(nullable = false)
    private int people;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_car_id")
    private Car assignedCar;

    @Column(nullable = false)
    private Instant createdAt;

    public Journey() {
    }

    public Journey(int id, int people) {
        this.id = id;
        this.people = people;
        this.createdAt = Instant.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPeople() {
        return people;
    }

    public void setPeople(int people) {
        this.people = people;
    }

    public Car getAssignedCar() {
        return assignedCar;
    }

    public void setAssignedCar(Car assignedCar) {
        this.assignedCar = assignedCar;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
