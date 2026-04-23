package com.carpooling.dto.response;

import java.util.Set;

public record AuthResponseDTO(
        String token,
        String username,
        Set<String> roles
) {}
