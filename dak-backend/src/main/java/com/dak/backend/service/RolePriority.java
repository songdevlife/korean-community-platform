package com.dak.backend.service;

import com.dak.backend.domain.Role;

import java.util.List;
import java.util.Set;

/**
 * Resolves the single role a user is presented as, from the several they may hold.
 *
 * getRoles() returns a Set, so picking one with findFirst gives whichever the
 * hash order happens to yield — an administrator who also holds USER could be
 * reported as either, and differently between calls. This list makes the answer
 * deterministic and is the only place the ordering is written down.
 *
 * Extracted because the same resolution now happens in more than one place, and
 * a second copy would be a second thing to keep in step.
 */
public final class RolePriority {

    private static final List<String> ORDER =
            List.of("ADMINISTRATOR", "MODERATOR", "BUSINESS_OWNER", "USER");

    private RolePriority() {}

    public static String highestOf(Set<Role> roles) {
        Set<String> names = roles.stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
        return ORDER.stream().filter(names::contains).findFirst().orElse("USER");
    }
}