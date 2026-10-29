package com.smartledger.app.feature.procurement.data.api

import com.smartledger.app.feature.procurement.data.dto.CreatePurchaseRequest
import com.smartledger.app.feature.procurement.data.dto.MaterialReceiptResponse
import com.smartledger.app.feature.procurement.data.dto.PurchaseResponse
import com.smartledger.app.feature.procurement.data.dto.RecordReceiptRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for Procurement & Silk Purchases.
 */
interface PurchaseApiService {

    @GET("api/v1/purchases")
    suspend fun getPurchases(
        @Query("supplierId") supplierId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<PurchaseResponse>>

    @GET("api/v1/purchases/{id}")
    suspend fun getPurchaseById(
        @Path("id") id: String
    ): Response<PurchaseResponse>

    @POST("api/v1/purchases")
    suspend fun createPurchase(
        @Body request: CreatePurchaseRequest
    ): Response<PurchaseResponse>

    @POST("api/v1/purchases/{id}/receipts")
    suspend fun recordReceipt(
        @Path("id") id: String,
        @Body request: RecordReceiptRequest
    ): Response<MaterialReceiptResponse>
}
