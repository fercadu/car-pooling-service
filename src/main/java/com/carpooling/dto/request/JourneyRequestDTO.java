package com.carpooling.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record JourneyRequestDTO(
        @Positive(message = "Journey ID must be positive")
        int id,
        @Min(value = 1, message = "People must be at least 1")
        @Max(value = 6, message = "People must be at most 6")
        int people
) {}
