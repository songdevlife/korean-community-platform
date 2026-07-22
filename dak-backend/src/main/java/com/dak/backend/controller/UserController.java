package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.domain.Role;
import com.dak.backend.domain.User;
import com.dak.backend.dto.UserProfileResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal User user) {
        String role = user.getRoles().stream().findFirst().map(Role::getName).orElse("USER");

        return ApiResponse.ok(new UserProfileResponse(
                user.getId(), user.getEmail(), user.getDisplayName(),
                user.getProfileImageUrl(), role, user.isEmailVerified(), user.getCreatedAt()
        ));
    }
}