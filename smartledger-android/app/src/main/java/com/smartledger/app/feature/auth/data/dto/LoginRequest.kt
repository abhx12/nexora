package com.smartledger.app.feature.auth.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Request payload for POST /api/v1/auth/login.
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)
