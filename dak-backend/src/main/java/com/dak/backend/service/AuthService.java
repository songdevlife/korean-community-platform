package com.dak.backend.service;

import com.dak.backend.config.JwtService;
import com.dak.backend.config.TokenHasher;
import com.dak.backend.domain.Role;
import com.dak.backend.domain.Session;
import com.dak.backend.domain.User;
import com.dak.backend.dto.*;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.RoleRepository;
import com.dak.backend.repository.SessionRepository;
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
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;

    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 14;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                        SessionRepository sessionRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, TokenHasher tokenHasher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenHasher = tokenHasher;
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

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalisedEmail = request.email().trim().toLowerCase();

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

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        String hash = tokenHasher.hash(request.refreshToken());

        Session session = sessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN"));

        if (session.isRevoked()) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "SESSION_REVOKED", "This session is no longer active.");
        }

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "REFRESH_TOKEN_EXPIRED", "The refresh token has expired.");
        }

        User user = session.getUser();
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "ACCOUNT_SUSPENDED", "This account is not active.");
        }

        // Rotation: revoke the old session, issue a brand new one.
        // Per 05 API Spec §3.4: "the old refresh token becomes invalid after successful refresh".
        session.setRevoked(true);

        String newAccessToken = jwtService.generateAccessToken(user.getId());
        String newRefreshToken = issueSession(user);

        return new RefreshResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return; // Nothing to revoke; treat as already-logged-out rather than an error.
        }

        String hash = tokenHasher.hash(rawRefreshToken);
        sessionRepository.findByRefreshTokenHash(hash).ifPresent(session -> {
            // Repeating logout for an already revoked session should not error (05 API Spec §3.3 notes).
            session.setRevoked(true);
        });
    }

    private AuthResponse buildAuthResponse(User user) {
        String role = user.getRoles().stream().findFirst().map(Role::getName).orElse("USER");

        AuthResponse.UserSummary summary = new AuthResponse.UserSummary(
                user.getId(), user.getEmail(), user.getDisplayName(),
                role, user.isEmailVerified(), user.getCreatedAt()
        );

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = issueSession(user);

        return new AuthResponse(summary, accessToken, refreshToken);
    }

    private String issueSession(User user) {
        String rawToken = tokenHasher.generateRawToken();
        String hash = tokenHasher.hash(rawToken);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS);

        Session session = Session.createNew(user, hash, expiresAt);
        sessionRepository.save(session);

        return rawToken;
    }
}