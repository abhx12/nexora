package com.nexora.identity.api.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleUserStatusRequest(
        @NotNull(message = "Active status is required")
        Boolean active
) {
}
