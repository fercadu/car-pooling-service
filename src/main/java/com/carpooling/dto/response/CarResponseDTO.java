package com.carpooling.dto.response;

import com.carpooling.model.Car;

public record CarResponseDTO(int id, int seats) {

    public static CarResponseDTO fromDomain(Car car) {
        return new CarResponseDTO(car.getId(), car.getSeats());
    }
}
