package com.smartledger.app.feature.parties.domain

import com.smartledger.app.feature.parties.data.dto.CreatePartyRequest
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.data.dto.UpdatePartyRequest
import com.smartledger.app.feature.parties.domain.model.PartyType

/**
 * Domain repository contract for Party management.
 */
interface PartiesRepository {

    suspend fun getParties(type: PartyType? = null, search: String? = null): Result<List<PartyResponse>>

    suspend fun getPartyById(id: String): Result<PartyResponse>

    suspend fun createParty(request: CreatePartyRequest): Result<PartyResponse>

    suspend fun updateParty(id: String, request: UpdatePartyRequest): Result<PartyResponse>
}
