package com.carpooling.dto.response;

import com.carpooling.model.User;

import java.util.Set;
import java.util.stream.Collectors;

public record UserResponseDTO(
        Long id,
        String username,
        String email,
        Set<String> roles,
        boolean enabled
) {
    public static UserResponseDTO fromDomain(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.isEnabled()
        );
    }
}
