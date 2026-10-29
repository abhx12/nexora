package com.smartledger.app.feature.parties.data.api

import com.smartledger.app.feature.parties.data.dto.CreatePartyRequest
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.data.dto.UpdatePartyRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for Parties management endpoints.
 */
interface PartiesApiService {

    @GET("api/v1/parties")
    suspend fun getParties(
        @Query("type") type: String? = null,
        @Query("search") search: String? = null
    ): Response<List<PartyResponse>>

    @POST("api/v1/parties")
    suspend fun createParty(
        @Body request: CreatePartyRequest
    ): Response<PartyResponse>

    @GET("api/v1/parties/{id}")
    suspend fun getPartyById(
        @Path("id") id: String
    ): Response<PartyResponse>

    @PATCH("api/v1/parties/{id}")
    suspend fun updateParty(
        @Path("id") id: String,
        @Body request: UpdatePartyRequest
    ): Response<PartyResponse>
}
