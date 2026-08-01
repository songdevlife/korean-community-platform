package com.dak.backend.service;

import com.dak.backend.domain.User;
import com.dak.backend.dto.UpdateProfileRequest;
import com.dak.backend.dto.UserProfileResponse;
import com.dak.backend.exception.ApiException;
import com.dak.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Changes the display name of the signed-in user.
     *
     * The name appears publicly as the author of a guide, so this is the route
     * out for anyone who used their legal name at sign-up without realising
     * where it would show. Guides reference the user rather than storing a
     * copy of the name, so past work re-attributes itself.
     *
     * Uniqueness is checked case-insensitively: two names differing only in
     * case read as the same name to a person, and allowing both would make
     * impersonation trivial. The check races another request writing the same
     * name, which the unique index in V18 settles — this exists to produce a
     * usable message in the ordinary case rather than a constraint violation.
     */
    @Transactional
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        String newName = request.displayName().trim();

        if (newName.isEmpty()) {
            throw ApiException.badRequest("INVALID_DISPLAY_NAME", "Enter a display name.");
        }

        // Unchanged apart from whitespace or case of their own name: allow it
        // through rather than reporting their own name as taken.
        if (!newName.equalsIgnoreCase(user.getDisplayName())
                && userRepository.existsByDisplayNameIgnoreCase(newName)) {
            throw ApiException.badRequest("DISPLAY_NAME_TAKEN",
                    "That display name is already in use.");
        }

        user.setDisplayName(newName);
        userRepository.save(user);

        return toProfile(user);
    }

    public UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(), user.getEmail(), user.getDisplayName(),
                user.getProfileImageUrl(), RolePriority.highestOf(user.getRoles()),
                user.isEmailVerified(), user.getCreatedAt());
    }
}