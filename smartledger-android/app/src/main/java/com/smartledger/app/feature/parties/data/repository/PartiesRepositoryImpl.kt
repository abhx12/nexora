package com.smartledger.app.feature.parties.data.repository

import com.google.gson.Gson
import com.smartledger.app.BuildConfig
import com.smartledger.app.core.util.ApiError
import com.smartledger.app.feature.parties.data.api.PartiesApiService
import com.smartledger.app.feature.parties.data.dto.CreatePartyRequest
import com.smartledger.app.feature.parties.data.dto.PartyResponse
import com.smartledger.app.feature.parties.data.dto.UpdatePartyRequest
import com.smartledger.app.feature.parties.domain.PartiesRepository
import com.smartledger.app.feature.parties.domain.model.PartyType
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartiesRepositoryImpl @Inject constructor(
    private val partiesApiService: PartiesApiService,
    private val gson: Gson
) : PartiesRepository {

    // Thread-safe in-memory cache / mock data for standalone development
    private val mockParties = CopyOnWriteArrayList<PartyResponse>().apply {
        addAll(
            listOf(
                PartyResponse(
                    id = "cust-1",
                    partyCode = "CUST-001",
                    name = "Ramesh Silks & Sarees",
                    partyType = "CUSTOMER",
                    phone = "9845012345",
                    address = "Avenue Road, Bangalore, Karnataka",
                    status = "ACTIVE"
                ),
                PartyResponse(
                    id = "cust-2",
                    partyCode = "CUST-002",
                    name = "Kanchipuram Handlooms",
                    partyType = "CUSTOMER",
                    phone = "9443219876",
                    address = "Gandhi Road, Kanchipuram, Tamil Nadu",
                    status = "ACTIVE"
                ),
                PartyResponse(
                    id = "supp-1",
                    partyCode = "SUPP-001",
                    name = "Karnataka Silk Filatures Ltd",
                    partyType = "SUPPLIER",
                    phone = "9880198801",
                    address = "Ramanagara Cocoon Market, Karnataka",
                    status = "ACTIVE"
                ),
                PartyResponse(
                    id = "supp-2",
                    partyCode = "SUPP-002",
                    name = "Venkateshwara Silk Reelers",
                    partyType = "SUPPLIER",
                    phone = "9741234567",
                    address = "Sidlaghatta Silk Center, Kolar, Karnataka",
                    status = "ACTIVE"
                ),
                PartyResponse(
                    id = "mfg-1",
                    partyCode = "MFG-001",
                    name = "Balaji Twisting & Sizing Works",
                    partyType = "MANUFACTURER",
                    phone = "9845987654",
                    address = "Industrial Area, Doddaballapura, Karnataka",
                    status = "ACTIVE"
                ),
                PartyResponse(
                    id = "mfg-2",
                    partyCode = "MFG-002",
                    name = "Sri Lakshmi Sizing Unit",
                    partyType = "MANUFACTURER",
                    phone = null,
                    address = "Magadi Road, Bangalore, Karnataka",
                    status = "INACTIVE"
                ),
                PartyResponse(
                    id = "wrk-1",
                    partyCode = "WRK-001",
                    name = "Venkatesh (Twisting Operator)",
                    partyType = "WORKER",
                    phone = "9988776655",
                    address = "Unit 2, Factory Quarters",
                    status = "ACTIVE"
                ),
                PartyResponse(
                    id = "wrk-2",
                    partyCode = "WRK-002",
                    name = "Manjunath (Winder)",
                    partyType = "WORKER",
                    phone = null,
                    address = "Weavers Colony, Bangalore",
                    status = "ACTIVE"
                )
            )
        )
    }

    override suspend fun getParties(type: PartyType?, search: String?): Result<List<PartyResponse>> {
        val typeParam = type?.name
        val searchParam = search?.trim()?.takeIf { it.isNotBlank() }

        return try {
            val response = partiesApiService.getParties(type = typeParam, search = searchParam)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                // Filter mock in-memory parties
                var filtered = mockParties.toList()
                if (type != null) {
                    filtered = filtered.filter { it.partyType.equals(type.name, ignoreCase = true) }
                }
                if (!searchParam.isNullOrBlank()) {
                    filtered = filtered.filter {
                        it.name.contains(searchParam, ignoreCase = true) ||
                                it.partyCode.contains(searchParam, ignoreCase = true) ||
                                (it.phone != null && it.phone.contains(searchParam))
                    }
                }
                Result.success(filtered)
            } else {
                Result.failure(Exception("Unable to connect to server: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPartyById(id: String): Result<PartyResponse> {
        return try {
            val response = partiesApiService.getPartyById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val found = mockParties.firstOrNull { it.id == id || it.partyCode.equals(id, ignoreCase = true) }
                if (found != null) {
                    Result.success(found)
                } else {
                    Result.failure(Exception("Party not found"))
                }
            } else {
                Result.failure(Exception("Unable to fetch party details: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createParty(request: CreatePartyRequest): Result<PartyResponse> {
        val trimmedName = request.name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Party name is required"))
        }

        return try {
            val response = partiesApiService.createParty(request.copy(name = trimmedName))
            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!
                mockParties.add(0, created)
                Result.success(created)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val typeEnum = PartyType.fromString(request.partyType)
                val count = mockParties.count { it.partyType.equals(typeEnum.name, ignoreCase = true) } + 1
                val code = "${typeEnum.prefix}-%03d".format(count)

                val created = PartyResponse(
                    id = UUID.randomUUID().toString(),
                    partyCode = code,
                    name = trimmedName,
                    partyType = typeEnum.name,
                    phone = request.phone?.trim()?.takeIf { it.isNotBlank() },
                    address = request.address?.trim()?.takeIf { it.isNotBlank() },
                    status = "ACTIVE"
                )
                mockParties.add(0, created)
                Result.success(created)
            } else {
                Result.failure(Exception("Unable to create party: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateParty(id: String, request: UpdatePartyRequest): Result<PartyResponse> {
        return try {
            val response = partiesApiService.updateParty(id, request)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                val idx = mockParties.indexOfFirst { it.id == id }
                if (idx != -1) {
                    mockParties[idx] = updated
                }
                Result.success(updated)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.code())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            if (BuildConfig.USE_MOCK) {
                val idx = mockParties.indexOfFirst { it.id == id || it.partyCode.equals(id, ignoreCase = true) }
                if (idx != -1) {
                    val existing = mockParties[idx]
                    val updated = existing.copy(
                        name = request.name?.trim() ?: existing.name,
                        phone = request.phone?.trim() ?: existing.phone,
                        address = request.address?.trim() ?: existing.address,
                        status = request.status?.trim() ?: existing.status
                    )
                    mockParties[idx] = updated
                    Result.success(updated)
                } else {
                    Result.failure(Exception("Party not found for update"))
                }
            } else {
                Result.failure(Exception("Unable to update party: ${e.localizedMessage ?: "Network error"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        if (!errorBody.isNullOrBlank()) {
            try {
                val apiError = gson.fromJson(errorBody, ApiError::class.java)
                if (!apiError?.message.isNullOrBlank()) {
                    return apiError.message
                }
            } catch (_: Exception) {
            }
        }
        return when (statusCode) {
            400 -> "Invalid party details submitted"
            404 -> "Party not found"
            409 -> "Party code or name conflict"
            else -> "Party operation failed (code: $statusCode)"
        }
    }
}
