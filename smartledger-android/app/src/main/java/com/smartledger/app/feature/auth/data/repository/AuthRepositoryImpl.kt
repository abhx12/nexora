package com.smartledger.app.feature.auth.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.core.auth.TokenManager
import com.smartledger.app.core.util.ApiError
import com.smartledger.app.feature.auth.data.api.AuthApiService
import com.smartledger.app.feature.auth.data.dto.LoginRequest
import com.smartledger.app.feature.auth.data.dto.LoginResponse
import com.smartledger.app.feature.auth.data.dto.RefreshTokenRequest
import com.smartledger.app.feature.auth.domain.AuthRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenManager,
    private val gson: Gson
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<LoginResponse> {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()

        if (trimmedUser.isBlank() || trimmedPass.isBlank()) {
            return Result.failure(IllegalArgumentException("Username and password cannot be empty"))
        }

        return try {
            val response = authApiService.login(LoginRequest(trimmedUser, trimmedPass))
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                tokenStorage.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)
                Result.success(loginResponse)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            // When mock mode is enabled in debug builds and backend is unreachable, fallback to mock login
            if (BuildConfig.USE_MOCK) {
                val mockResponse = LoginResponse(
                    accessToken = "mock_jwt_access_token_${System.currentTimeMillis()}",
                    refreshToken = "mock_jwt_refresh_token_${System.currentTimeMillis()}",
                    expiresIn = 900L
                )
                tokenStorage.saveTokens(mockResponse.accessToken, mockResponse.refreshToken)
                Result.success(mockResponse)
            } else {
                Result.failure(Exception("Unable to connect to server: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<LoginResponse> {
        val refreshToken = tokenStorage.getRefreshToken()
            ?: return Result.failure(IllegalStateException("No refresh token available"))

        return try {
            val response = authApiService.refresh(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful && response.body() != null) {
                val refreshed = response.body()!!
                tokenStorage.saveTokens(refreshed.accessToken, refreshed.refreshToken)
                Result.success(refreshed)
            } else {
                tokenStorage.clearTokens()
                Result.failure(Exception("Session expired. Please log in again."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()

    override fun getAccessToken(): String? = tokenStorage.getAccessToken()

    override fun logout() {
        tokenStorage.clearTokens()
    }

    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        if (!errorBody.isNullOrBlank()) {
            try {
                val apiError = gson.fromJson(errorBody, ApiError::class.java)
                if (!apiError?.message.isNullOrBlank()) {
                    return apiError.message
                }
            } catch (_: Exception) {
                // If parsing fails, fall back to default HTTP message
            }
        }
        return when (statusCode) {
            401 -> "Invalid username or password"
            403 -> "Access denied"
            404 -> "Resource not found"
            500 -> "Internal server error"
            else -> "Authentication failed (code: $statusCode)"
        }
    }
}
