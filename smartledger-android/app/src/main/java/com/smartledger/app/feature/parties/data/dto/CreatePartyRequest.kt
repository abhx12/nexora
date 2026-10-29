package com.smartledger.app.feature.parties.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload for POST /api/v1/parties.
 *
 * Rules:
 * - name: required (must not be empty)
 * - partyType: "CUSTOMER", "SUPPLIER", "MANUFACTURER", "WORKER"
 * - phone: OPTIONAL (never mark it required)
 * - address: optional
 */
data class CreatePartyRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("partyType")
    val partyType: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("address")
    val address: String? = null
)
