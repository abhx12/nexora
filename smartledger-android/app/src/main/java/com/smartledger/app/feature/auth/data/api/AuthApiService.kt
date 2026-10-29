package com.smartledger.app.feature.auth.data.api

import com.smartledger.app.feature.auth.data.dto.LoginRequest
import com.smartledger.app.feature.auth.data.dto.LoginResponse
import com.smartledger.app.feature.auth.data.dto.RefreshTokenRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for Authentication endpoints.
 */
interface AuthApiService {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequest
    ): Response<LoginResponse>

    /**
     * Synchronous version for OkHttp Authenticator (401 handling).
     */
    @POST("api/v1/auth/refresh")
    fun refreshSync(
        @Body request: RefreshTokenRequest
    ): Call<LoginResponse>
}
