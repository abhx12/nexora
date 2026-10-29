package com.nexora.desktop.repository

import com.nexora.desktop.api.ApiClient
import com.nexora.desktop.api.ApiError
import com.nexora.desktop.api.AuthSession
import com.nexora.desktop.api.dto.LoginRequest
import com.nexora.desktop.api.dto.MeResponse
import com.nexora.desktop.api.dto.RefreshRequest
import com.nexora.desktop.api.dto.TokenResponse
import com.nexora.desktop.util.SettingsStore

/** Login, start-up resume and logout (FR-AUTH-01..07, FR-AUTH-11). */
class AuthRepository(private val api: ApiClient, private val session: AuthSession, private val settings: SettingsStore) {

    /** POST /auth/login, then GET /auth/me for the permissions that drive the menus. */
    suspend fun login(username: String, password: String): ApiResult<MeResponse> {
        val name = username.trim()
        val tokens: ApiResult<TokenResponse> = api.post("/auth/login", LoginRequest(name, password), authenticated = false)
        return when (tokens) {
            is ApiResult.Failure -> tokens
            is ApiResult.Success -> {
                session.signIn(tokens.value, name)
                settings.update { it.copy(lastUsername = name) }
                me()
            }
        }
    }

    suspend fun me(): ApiResult<MeResponse> = api.get("/auth/me")

    /**
     * Start-up with a stored refresh token: refresh, then load the user. Any failure means "show the login screen";
     * a refused token has already been deleted by the session.
     */
    suspend fun resume(): ApiResult<MeResponse> {
        val username = settings.current.lastUsername
        if (username == null || !session.restore(username)) return ApiResult.Failure(ApiError(401, "UNAUTHENTICATED"))
        if (!session.refreshAfter(null, api)) return ApiResult.Failure(ApiError(401, "UNAUTHENTICATED"))
        return me()
    }

    /** Revokes the refresh token on the server when reachable; local tokens are deleted in every case (works offline). */
    suspend fun logout() {
        session.currentRefreshToken()?.let { token ->
            api.post<RefreshRequest, Unit>("/auth/logout", RefreshRequest(token), authenticated = false)
        }
        session.endSession()
    }

    suspend fun changePassword(current: String, new: String): ApiResult<Unit> =
        api.post("/auth/password", com.nexora.desktop.api.dto.ChangePasswordRequest(current, new))
}
