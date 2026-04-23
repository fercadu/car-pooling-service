package com.carpooling.dto.request;

import com.carpooling.model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequestDTO(
        @NotNull(message = "Role is required")
        Role role,
        @NotNull(message = "Action is required (ADD or REMOVE)")
        Action action
) {
    public enum Action { ADD, REMOVE }
}
