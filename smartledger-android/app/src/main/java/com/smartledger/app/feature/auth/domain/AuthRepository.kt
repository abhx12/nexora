package com.smartledger.app.feature.auth.domain

import com.smartledger.app.feature.auth.data.dto.LoginResponse

/**
 * Domain repository interface for Authentication operations.
 */
interface AuthRepository {

    /**
     * Authenticates user with username & password, persists tokens on success.
     */
    suspend fun login(username: String, password: String): Result<LoginResponse>

    /**
     * Refreshes JWT token using stored refresh token.
     */
    suspend fun refreshToken(): Result<LoginResponse>

    /**
     * Returns true if user has a stored access token.
     */
    fun isLoggedIn(): Boolean

    /**
     * Returns the current access token, or null if logged out.
     */
    fun getAccessToken(): String?

    /**
     * Clears all tokens and resets authentication state.
     */
    fun logout()
}
