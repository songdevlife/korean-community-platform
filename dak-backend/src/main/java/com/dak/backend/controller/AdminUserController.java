package com.dak.backend.controller;

import com.dak.backend.common.ApiResponse;
import com.dak.backend.dto.AdminUserResponse;
import com.dak.backend.dto.UpdateUserRoleRequest;
import com.dak.backend.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Every account, newest first.
     *
     * Read-only. Exists so that "how many people have signed up" stops being
     * a question answered by connecting psql to production — see the register.
     */
    @GetMapping
    public ApiResponse<Page<AdminUserResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(adminUserService.listAll(page, pageSize));
    }

    @PatchMapping("/{userId}/role")
    public ApiResponse<AdminUserResponse> updateRole(@PathVariable UUID userId,
                                                       @Valid @RequestBody UpdateUserRoleRequest request) {
        return ApiResponse.ok(adminUserService.updateRole(userId, request));
    }
}