package com.smartledger.app.feature.parties.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload for PATCH /api/v1/parties/{id}.
 */
data class UpdatePartyRequest(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("status")
    val status: String? = null
)
