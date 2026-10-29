package com.nexora.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @Size(max = 128, message = "Current password is too long")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 12, max = 72, message = "New password must be between 12 and 72 characters")
        String newPassword
) {
}
