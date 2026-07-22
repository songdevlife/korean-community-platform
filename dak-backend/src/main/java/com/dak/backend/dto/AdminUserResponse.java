package com.dak.backend.dto;

import java.util.UUID;

public record AdminUserResponse(UUID id, String email, String displayName, String role, String accountStatus) {}