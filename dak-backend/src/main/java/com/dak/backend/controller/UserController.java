package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.domain.User;
import com.dak.backend.dto.UpdateProfileRequest;
import com.dak.backend.dto.UserProfileResponse;
import com.dak.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal User user) {
        // Was findFirst over an unordered Set, so an administrator who also
        // holds USER could be reported as either — and this response is what
        // the account page reads to decide whether to show the admin entry.
        return ApiResponse.ok(userService.toProfile(user));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(@AuthenticationPrincipal User user,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(user, request));
    }
}