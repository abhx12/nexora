package com.nexora.desktop.api.dto

import kotlinx.serialization.Serializable

// Shapes of 05-API-SPEC section 4 (identity).

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String, val tokenType: String = "Bearer", val expiresIn: Long = 900)

@Serializable
data class MeResponse(
    val id: String,
    val username: String,
    val displayName: String,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val lastLoginAt: String? = null,
) {
    fun can(permission: String): Boolean = permission in permissions
}

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val displayName: String,
    val active: Boolean,
    val roles: List<String> = emptyList(),
    val lastLoginAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val displayName: String,
    val password: String,
    val role: String,
)

@Serializable
data class ToggleUserStatusRequest(val active: Boolean)

