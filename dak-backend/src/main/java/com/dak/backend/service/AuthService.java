package com.dak.backend.service;

import com.dak.backend.config.JwtService;
import com.dak.backend.domain.Role;
import com.dak.backend.domain.User;
import com.dak.backend.dto.AuthResponse;
import com.dak.backend.dto.LoginRequest;
import com.dak.backend.dto.RegisterRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.RoleRepository;
import com.dak.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalisedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalisedEmail)) {
            throw ApiException.conflict("EMAIL_ALREADY_EXISTS", "Email address is already registered.");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("USER role is missing — check V1__init.sql seed data."));

                User user = User.createNew(
                normalisedEmail,
                passwordEncoder.encode(request.password()),
                request.displayName().trim()
        );
        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        return buildAuthResponse(user);
    }
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalisedEmail = request.email().trim().toLowerCase();

        // Per 05 API Spec §3.2 notes: "must not reveal whether a specific email address exists" —
        // both a missing user and a wrong password return the same INVALID_CREDENTIALS error.
        User user = userRepository.findByEmailIgnoreCase(normalisedEmail)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS", "Email or password is incorrect.");
        }

        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "ACCOUNT_SUSPENDED", "This account is not active.");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String role = user.getRoles().stream().findFirst().map(Role::getName).orElse("USER");

        AuthResponse.UserSummary summary = new AuthResponse.UserSummary(
                user.getId(), user.getEmail(), user.getDisplayName(),
                role, user.isEmailVerified(), user.getCreatedAt()
        );

        return new AuthResponse(
                summary,
                jwtService.generateAccessToken(user.getId()),
                jwtService.generateRefreshToken(user.getId())
        );
    }
}