package com.smartledger.app.feature.auth.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Request payload for POST /api/v1/auth/refresh.
 */
data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String
)
