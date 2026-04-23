package com.carpooling.controller;

import com.carpooling.dto.request.LoginRequestDTO;
import com.carpooling.dto.request.RegisterRequestDTO;
import com.carpooling.dto.request.UpdateRoleRequestDTO;
import com.carpooling.dto.response.AuthResponseDTO;
import com.carpooling.dto.response.UserResponseDTO;
import com.carpooling.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("POST /auth/login — user '{}'", request.username());
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        log.info("POST /auth/register — user '{}'", request.username());
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/auth/me")
    public ResponseEntity<UserResponseDTO> me(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails userDetails) {
        log.debug("GET /auth/me — user '{}'", userDetails.getUsername());
        var roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(java.util.stream.Collectors.toSet());
        return ResponseEntity.ok(new UserResponseDTO(null, userDetails.getUsername(), null, roles, true));
    }

    // ── User management (ADMIN only) ──

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> listUsers() {
        log.info("GET /users — listing all users");
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateRole(
            @PathVariable Long id, @Valid @RequestBody UpdateRoleRequestDTO request) {
        log.info("PUT /users/{}/role — {} role {}", id, request.action(), request.role());
        UserResponseDTO response = switch (request.action()) {
            case ADD -> authService.addRole(id, request.role());
            case REMOVE -> authService.removeRole(id, request.role());
        };
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /users/{}", id);
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
