package com.carpooling.service;

import com.carpooling.dto.request.LoginRequestDTO;
import com.carpooling.dto.request.RegisterRequestDTO;
import com.carpooling.dto.response.AuthResponseDTO;
import com.carpooling.dto.response.UserResponseDTO;
import com.carpooling.model.Role;
import com.carpooling.model.User;
import com.carpooling.repository.UserRepository;
import com.carpooling.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        String token = jwtUtils.generateToken(request.username());

        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .collect(Collectors.toSet());

        log.info("LOGIN — User '{}' authenticated with roles {}", request.username(), roles);
        return new AuthResponseDTO(token, request.username(), roles);
    }

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username '" + request.username() + "' is already taken");
        }

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.email(),
                Set.of(Role.OBSERVER)
        );
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getUsername());
        Set<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());

        log.info("REGISTER — New user '{}' created with roles {}", user.getUsername(), roles);
        return new AuthResponseDTO(token, user.getUsername(), roles);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::fromDomain)
                .toList();
    }

    @Transactional
    public UserResponseDTO addRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        user.getRoles().add(role);
        userRepository.save(user);
        log.info("ROLE — Added {} to user '{}'", role, user.getUsername());
        return UserResponseDTO.fromDomain(user);
    }

    @Transactional
    public UserResponseDTO removeRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        user.getRoles().remove(role);
        userRepository.save(user);
        log.info("ROLE — Removed {} from user '{}'", role, user.getUsername());
        return UserResponseDTO.fromDomain(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        if (user.getRoles().contains(Role.ADMIN)) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().contains(Role.ADMIN))
                    .count();
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot delete the last admin user");
            }
        }
        userRepository.delete(user);
        log.info("DELETE — User '{}' deleted", user.getUsername());
    }

    @Bean
    CommandLineRunner initDefaultAdmin(
            @Value("${admin.username:admin}") String adminUsername,
            @Value("${admin.password:admin123}") String adminPassword) {
        return args -> {
            if (!userRepository.existsByUsername(adminUsername)) {
                User admin = new User(
                        adminUsername,
                        passwordEncoder.encode(adminPassword),
                        null,
                        Set.of(Role.ADMIN, Role.OPERATOR, Role.OBSERVER)
                );
                userRepository.save(admin);
                log.info("═══════════════════════════════════════════════════");
                log.info("DEFAULT ADMIN created — username: '{}' password: '{}'", adminUsername, adminPassword);
                log.info("⚠️  Change the default password in production!");
                log.info("═══════════════════════════════════════════════════");
            }
        };
    }
}
