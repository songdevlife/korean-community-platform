package com.dak.backend.service;

import com.dak.backend.domain.Role;
import com.dak.backend.domain.User;
import com.dak.backend.dto.AdminUserResponse;
import com.dak.backend.dto.UpdateUserRoleRequest;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.RoleRepository;
import com.dak.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminUserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public AdminUserResponse updateRole(UUID userId, UpdateUserRoleRequest request) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found."));

        boolean userIsCurrentlyAdmin = user.getRoles().stream()
                .anyMatch(r -> "ADMINISTRATOR".equals(r.getName()));
        boolean newRoleIsAdmin = "ADMINISTRATOR".equals(request.role());

        // Protect the platform's last active administrator from being demoted (05 API Spec §10.1 notes).
        if (userIsCurrentlyAdmin && !newRoleIsAdmin && userRepository.countByRoleName("ADMINISTRATOR") <= 1) {
            throw ApiException.conflict("LAST_ADMINISTRATOR",
                    "Cannot change the role of the platform's last remaining administrator.");
        }

        Role newRole = roleRepository.findByName(request.role())
                .orElseThrow(() -> new IllegalStateException("Role missing from database: " + request.role()));

        user.setRoles(Set.of(newRole));

        return new AdminUserResponse(
                user.getId(), user.getEmail(), user.getDisplayName(),
                newRole.getName(), user.getAccountStatus()
        );
    }
}