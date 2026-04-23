package com.carpooling.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cars")
public class Car {

    @Id
    private int id;

    @Column(nullable = false)
    private int seats;

    @Column(nullable = false)
    private int availableSeats;

    public Car() {
    }

    public Car(int id, int seats) {
        this.id = id;
        this.seats = seats;
        this.availableSeats = seats;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
        this.availableSeats = seats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public void occupy(int people) {
        this.availableSeats -= people;
    }

    public void release(int people) {
        this.availableSeats += people;
    }
}
