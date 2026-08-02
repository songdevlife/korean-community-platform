package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.*;
import com.dak.backend.exception.ApiException;
import com.dak.backend.service.AuthService;
import com.dak.backend.service.EmailVerificationService;
import com.dak.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")

public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    // Registration is closed until the privacy policy it would be collected
    // under is finished and linked. Enforced here rather than only in the
    // interface: hiding the form leaves the endpoint open to anyone who sends
    // the request directly, which is not the same as being closed.
    @Value("${app.registration.enabled}")
    private boolean registrationEnabled;

    public AuthController(AuthService authService,
        PasswordResetService passwordResetService,
        EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (!registrationEnabled) {
            throw ApiException.forbidden(
                    "New accounts are not being created at the moment. Please check back soon.");
        }
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal com.dak.backend.domain.User user,
                        @RequestBody(required = false) RefreshRequest request) {
        String refreshToken = request != null ? request.refreshToken() : null;
        authService.logout(refreshToken);
    }

    /**
     * 204 whatever happened an unknown address, one that asked a moment ago,
     * and a real send are indistinguishable from outside. Any other answer
     * turns this into a way to test whether someone has an account here.
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }

    /**
     * Open, because the link is followed from an inbox rather than from a
     * signed-in session. The token is the authorisation.
     */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
    }

    /**
     * Requires a session: this is the "didn't get it" button on the account
     * page, so the address is whoever is signed in rather than whatever was
     * typed into a form.
     */
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@AuthenticationPrincipal com.dak.backend.domain.User user) {
        emailVerificationService.sendVerification(user);
    }
}