package com.smartledger.app.core.auth

import com.smartledger.app.feature.auth.data.api.AuthApiService
import com.smartledger.app.feature.auth.data.dto.RefreshTokenRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Thread-safe OkHttp Authenticator that automatically refreshes expired JWT tokens upon 401 Unauthorized.
 *
 * Requirements from Phase 2:
 * - Call POST /api/v1/auth/refresh with refresh token
 * - On success -> save new access token -> retry original request
 * - On failure -> clearTokens() -> causes UI to navigate to Login
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val authApiServiceProvider: Provider<AuthApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops if server keeps returning 401
        if (responseCount(response) >= 2) {
            tokenStorage.clearTokens()
            return null
        }

        // Do not attempt refresh for login or refresh endpoints themselves
        val path = response.request.url.encodedPath
        if (path.contains("/auth/login") || path.contains("/auth/refresh")) {
            return null
        }

        synchronized(this) {
            val currentToken = tokenStorage.getAccessToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // If another thread already completed token refresh, retry using that token
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenStorage.getRefreshToken() ?: run {
                tokenStorage.clearTokens()
                return null
            }

            return try {
                val refreshCall = authApiServiceProvider.get()
                    .refreshSync(RefreshTokenRequest(refreshToken))
                val refreshResponse = refreshCall.execute()

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val tokens = refreshResponse.body()!!
                    tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${tokens.accessToken}")
                        .build()
                } else {
                    tokenStorage.clearTokens()
                    null
                }
            } catch (_: Exception) {
                tokenStorage.clearTokens()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
