package com.nexora.identity.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String displayName,
        boolean active,
        List<String> roles,
        Instant lastLoginAt,
        Instant createdAt
) {
}
