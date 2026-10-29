package com.nexora.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username may only contain letters, numbers, underscores, dots, and hyphens")
        String username,

        @NotBlank(message = "Display name is required")
        @Size(min = 1, max = 200, message = "Display name must be at most 200 characters")
        String displayName,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
        String password,

        @NotBlank(message = "Role is required")
        String role
) {
}
