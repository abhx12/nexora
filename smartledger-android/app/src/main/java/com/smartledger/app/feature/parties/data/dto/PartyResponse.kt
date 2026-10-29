package com.smartledger.app.feature.parties.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Party response DTO.
 *
 * UI RULE: Always show partyCode (e.g. "CUST-001"), never the internal id UUID.
 * Status: "ACTIVE" or "INACTIVE".
 */
data class PartyResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("partyCode")
    val partyCode: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("partyType")
    val partyType: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("status")
    val status: String = "ACTIVE"
) {
    val isActive: Boolean
        get() = status.equals("ACTIVE", ignoreCase = true)
}
