package com.smartledger.app.feature.auth.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Response payload from POST /api/v1/auth/login and POST /api/v1/auth/refresh.
 */
data class LoginResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("expiresIn")
    val expiresIn: Long = 900L
)
