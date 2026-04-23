package com.carpooling.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CarRequestDTO(
        int id,
        @Min(value = 4, message = "Seats must be at least 4")
        @Max(value = 6, message = "Seats must be at most 6")
        int seats
) {}
